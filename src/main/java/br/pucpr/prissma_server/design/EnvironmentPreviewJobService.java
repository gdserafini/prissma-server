package br.pucpr.prissma_server.design;

import br.pucpr.prissma_server.attachments.storage.FileStorageService;
import br.pucpr.prissma_server.attachments.storage.StoredFile;
import br.pucpr.prissma_server.attachments.storage.UploadedFileValidator;
import br.pucpr.prissma_server.genai.EnvironmentPreviewRequest;
import br.pucpr.prissma_server.genai.EnvironmentPreviewService;
import br.pucpr.prissma_server.genai.ImageBytes;
import br.pucpr.prissma_server.projects.ProjectPermission;
import br.pucpr.prissma_server.projects.ProjectPermissionService;
import br.pucpr.prissma_server.users.User;
import br.pucpr.prissma_server.users.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

/**
 * Prévia visual por IA de uma proposta.
 *
 * O disparo é síncrono e barato: valida, guarda as imagens de entrada, grava a
 * linha em {@code PROCESSING} e devolve 202. Quem conversa com a OpenAI é
 * {@link #process(Long)}, chamado pelo listener depois do commit, em outra
 * thread — e é por isso que ele não checa permissão nem lê o contexto de
 * workspace: nenhum dos dois existe fora da requisição. A autorização acontece
 * aqui, no disparo.
 */
@Service
public class EnvironmentPreviewJobService {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentPreviewJobService.class);

    private final EnvironmentPreviewRepository previewRepository;
    private final DesignSubmissionRepository submissionRepository;
    private final DesignProposalRepository proposalRepository;
    private final DesignProposalService proposalService;
    private final EnvironmentPreviewService previewService;
    private final ProjectPermissionService permissionService;
    private final UserRepository userRepository;
    private final FileStorageService storage;
    private final UploadedFileValidator validator;
    private final ApplicationEventPublisher events;
    private final ObjectMapper objectMapper;

    public EnvironmentPreviewJobService(EnvironmentPreviewRepository previewRepository,
                                        DesignSubmissionRepository submissionRepository,
                                        DesignProposalRepository proposalRepository,
                                        DesignProposalService proposalService,
                                        EnvironmentPreviewService previewService,
                                        ProjectPermissionService permissionService,
                                        UserRepository userRepository,
                                        FileStorageService storage,
                                        UploadedFileValidator validator,
                                        ApplicationEventPublisher events,
                                        ObjectMapper objectMapper) {
        this.previewRepository = previewRepository;
        this.submissionRepository = submissionRepository;
        this.proposalRepository = proposalRepository;
        this.proposalService = proposalService;
        this.previewService = previewService;
        this.permissionService = permissionService;
        this.userRepository = userRepository;
        this.storage = storage;
        this.validator = validator;
        this.events = events;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PreviewResponse request(Long projectId,
                                   Long proposalId,
                                   MultipartFile rawImage,
                                   MultipartFile floorPlan,
                                   EnvironmentPreviewRequest options,
                                   Long userId) {
        DesignProposal proposal = proposalService.requireProposalScopedToProject(projectId, proposalId);
        permissionService.requirePermission(projectId, userId, ProjectPermission.MANAGE_PROPOSALS);

        if (rawImage == null || rawImage.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A foto do ambiente é obrigatória");
        }

        String rawKey = storeInput(rawImage, "rawImage", proposalId);
        String floorPlanKey = null;
        try {
            if (floorPlan != null && !floorPlan.isEmpty()) {
                floorPlanKey = storeInput(floorPlan, "floorPlan", proposalId);
            }

            EnvironmentPreview preview = new EnvironmentPreview();
            preview.setProposal(proposal);
            preview.setStatus(PreviewStatus.PROCESSING);
            preview.setRawImageKey(rawKey);
            preview.setFloorPlanKey(floorPlanKey);
            preview.setOptionsJson(serialize(options));
            preview.setRequestedByUser(requireUser(userId));
            preview.setCreatedAt(Instant.now());
            previewRepository.save(preview);

            events.publishEvent(new EnvironmentPreviewRequestedEvent(preview.getId()));
            return PreviewResponse.from(preview);
        } catch (RuntimeException ex) {
            proposalService.safeDelete(rawKey);
            proposalService.safeDelete(floorPlanKey);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public PreviewResponse get(Long projectId, Long proposalId, Long previewId, Long userId) {
        proposalService.requireProposalScopedToProject(projectId, proposalId);
        permissionService.requirePermission(projectId, userId, ProjectPermission.VIEW_PROJECT);

        EnvironmentPreview preview = previewRepository.findByIdAndProposalId(previewId, proposalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Prévia não encontrada"));
        return PreviewResponse.from(preview);
    }

    /**
     * Executa a geração. Roda fora da requisição: sem SecurityContext, sem
     * WorkspaceContext e sem checagem de permissão — só ids já resolvidos.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long previewId) {
        EnvironmentPreview preview = previewRepository.findById(previewId).orElse(null);
        if (preview == null || preview.getStatus() != PreviewStatus.PROCESSING) {
            return;
        }

        String generatedKey = null;
        try {
            EnvironmentPreviewRequest options =
                    objectMapper.readValue(preview.getOptionsJson(), EnvironmentPreviewRequest.class);

            byte[] png = previewService.generate(
                    readStored(preview.getRawImageKey()),
                    readStored(preview.getFloorPlanKey()),
                    options);

            DesignProposal proposal = preview.getProposal();
            Instant now = Instant.now();

            StoredFile stored;
            try (InputStream in = new ByteArrayInputStream(png)) {
                stored = storage.store(in, "png", "proposals/" + proposal.getId());
            }
            generatedKey = stored.storageKey();

            User author = preview.getRequestedByUser();
            DesignSubmission version = proposalService.buildVersion(proposal, author,
                    submissionRepository.findMaxVersion(proposal.getId()) + 1,
                    options.additionalInstructions(), true, now);
            version.setFileUrl(generatedKey);
            version.setFileName("previa-ia-v" + version.getVersion() + ".png");
            version.setFileType("image/png");
            submissionRepository.save(version);

            proposal.setUpdatedAt(now);
            proposalRepository.save(proposal);

            preview.setResultSubmission(version);
            preview.setStatus(PreviewStatus.READY);
            preview.setCompletedAt(now);
            previewRepository.save(preview);
        } catch (ResponseStatusException ex) {
            proposalService.safeDelete(generatedKey);
            fail(preview, ex.getReason() != null
                    ? ex.getReason()
                    : "Não foi possível gerar a prévia. Tente novamente.");
        } catch (Exception ex) {
            log.error("Falha ao gerar a prévia {}", previewId, ex);
            proposalService.safeDelete(generatedKey);
            fail(preview, "Não foi possível gerar a prévia. Tente novamente.");
        }
    }

    private void fail(EnvironmentPreview preview, String message) {
        preview.setStatus(PreviewStatus.FAILED);
        preview.setErrorMessage(message);
        preview.setCompletedAt(Instant.now());
        previewRepository.save(preview);
    }

    private String storeInput(MultipartFile file, String field, Long proposalId) {
        String contentType = validator.normalizeContentType(file.getContentType());
        validator.validateImageContentType(contentType, field);
        validator.validateSize(file.getSize());

        try (InputStream header = file.getInputStream()) {
            validator.verifyMagicBytes(header, contentType);
            try (InputStream payload = file.getInputStream()) {
                return storage.store(payload, validator.resolveExtension(contentType),
                        "proposals/" + proposalId).storageKey();
            }
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Não foi possível salvar o arquivo. Tente novamente em alguns instantes.");
        }
    }

    private ImageBytes readStored(String storageKey) throws IOException {
        if (storageKey == null || storageKey.isBlank()) {
            return null;
        }
        try (InputStream in = storage.load(storageKey).getInputStream()) {
            return new ImageBytes(in.readAllBytes(), contentTypeOf(storageKey));
        }
    }

    /** A extensão foi escrita por nós em {@code storeInput}, então é fonte confiável. */
    private String contentTypeOf(String storageKey) {
        String lower = storageKey.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    private String serialize(EnvironmentPreviewRequest options) {
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Não foi possível ler as opções da prévia");
        }
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
