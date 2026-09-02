package br.pucpr.prissma_server.diary;

import br.pucpr.prissma_server.attachments.Attachment;
import br.pucpr.prissma_server.attachments.AttachmentRepository;
import br.pucpr.prissma_server.projects.ConstructionProject;
import br.pucpr.prissma_server.projects.ConstructionProjectMember;
import br.pucpr.prissma_server.projects.ConstructionProjectMemberRepository;
import br.pucpr.prissma_server.projects.ConstructionProjectRepository;
import br.pucpr.prissma_server.projects.ProjectPermission;
import br.pucpr.prissma_server.projects.ProjectPermissionService;
import br.pucpr.prissma_server.users.User;
import br.pucpr.prissma_server.users.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

/**
 * RF17: diario da obra.
 *
 * Cada registro guarda data com dia e horario, tipo (ocorrencia, entrega,
 * efetivo ou impedimento), responsavel, uma breve descricao e, opcionalmente,
 * o vinculo com um anexo ja carregado na obra.
 */
@Service
public class DiaryService {

    /** Teto de itens por pagina, para o cliente nao pedir a tabela inteira em uma requisicao. */
    static final int MAX_PAGE_SIZE = 100;
    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_DESCRIPTION_LENGTH = 2000;

    private final DiaryEntryRepository diaryRepository;
    private final ConstructionProjectRepository projectRepository;
    private final ConstructionProjectMemberRepository memberRepository;
    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final ProjectPermissionService permissionService;

    public DiaryService(DiaryEntryRepository diaryRepository,
                        ConstructionProjectRepository projectRepository,
                        ConstructionProjectMemberRepository memberRepository,
                        AttachmentRepository attachmentRepository,
                        UserRepository userRepository,
                        ProjectPermissionService permissionService) {
        this.diaryRepository = diaryRepository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.attachmentRepository = attachmentRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
    }

    @Transactional
    public DiaryEntryResponse create(Long projectId, DiaryEntryRequest request, Long userId) {
        ConstructionProject project = requireProject(projectId);
        permissionService.requirePermission(projectId, userId, ProjectPermission.MANAGE_DIARY);

        DiaryEntryType type = DiaryEntryType.fromString(request.getEntryType());
        Instant entryDate = requireEntryDate(request.getEntryDate());
        String description = requireDescription(request.getDescription());

        Long responsibleId = request.getResponsibleUserId() != null
                ? request.getResponsibleUserId()
                : userId;
        User responsible = requireProjectMemberUser(projectId, responsibleId);

        DiaryEntry entry = new DiaryEntry();
        entry.setConstructionProject(project);
        entry.setEntryDate(entryDate);
        entry.setEntryType(type);
        entry.setResponsibleUser(responsible);
        entry.setResponsibleName(responsible.getName());
        entry.setDescription(description);
        entry.setAttachment(resolveAttachment(request.getAttachmentId(), projectId));

        Instant now = Instant.now();
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);

        return DiaryEntryResponse.from(diaryRepository.save(entry));
    }

    /**
     * Lista paginada do diario da obra, da data mais recente para a mais antiga.
     * A ordenacao esta fixa na query do repositorio: o cliente escolhe pagina e
     * tamanho, nunca a ordem.
     */
    @Transactional(readOnly = true)
    public DiaryEntryPageResponse list(Long projectId, Integer page, Integer size, Long userId) {
        requireProject(projectId);
        permissionService.requirePermission(projectId, userId, ProjectPermission.VIEW_PROJECT);

        Pageable pageable = PageRequest.of(resolvePage(page), resolveSize(size));
        Page<DiaryEntry> entries = diaryRepository.findPageByProject(projectId, pageable);
        return DiaryEntryPageResponse.from(entries);
    }

    @Transactional(readOnly = true)
    public DiaryEntryResponse get(Long projectId, Long entryId, Long userId) {
        DiaryEntry entry = requireEntryScopedToProject(projectId, entryId);
        permissionService.requirePermission(projectId, userId, ProjectPermission.VIEW_PROJECT);
        return DiaryEntryResponse.from(entry);
    }

    @Transactional
    public DiaryEntryResponse update(Long projectId, Long entryId,
                                     DiaryEntryUpdateRequest request, Long userId) {
        DiaryEntry entry = requireEntryScopedToProject(projectId, entryId);
        permissionService.requirePermission(projectId, userId, ProjectPermission.MANAGE_DIARY);

        if (request.getEntryDate() != null) {
            entry.setEntryDate(requireEntryDate(request.getEntryDate()));
        }
        if (request.getEntryType() != null && !request.getEntryType().isBlank()) {
            entry.setEntryType(DiaryEntryType.fromString(request.getEntryType()));
        }
        if (request.getResponsibleUserId() != null) {
            User responsible = requireProjectMemberUser(projectId, request.getResponsibleUserId());
            entry.setResponsibleUser(responsible);
            entry.setResponsibleName(responsible.getName());
        }
        if (request.getDescription() != null) {
            entry.setDescription(requireDescription(request.getDescription()));
        }
        if (request.getAttachmentId() != null) {
            entry.setAttachment(resolveAttachment(request.getAttachmentId(), projectId));
        } else if (Boolean.TRUE.equals(request.getUnlinkAttachment())) {
            entry.setAttachment(null);
        }

        entry.setUpdatedAt(Instant.now());
        return DiaryEntryResponse.from(diaryRepository.save(entry));
    }

    @Transactional
    public void delete(Long projectId, Long entryId, Long userId) {
        DiaryEntry entry = requireEntryScopedToProject(projectId, entryId);
        permissionService.requirePermission(projectId, userId, ProjectPermission.MANAGE_DIARY);
        diaryRepository.delete(entry);
    }

    private ConstructionProject requireProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    /**
     * Carrega o registro garantindo que ele pertence a obra da URL. Sem essa
     * checagem, um membro de uma obra leria/editaria o diario de outra so
     * trocando o id no path.
     */
    private DiaryEntry requireEntryScopedToProject(Long projectId, Long entryId) {
        DiaryEntry entry = diaryRepository.findById(entryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Diary entry not found"));
        if (entry.getConstructionProject() == null
                || !projectId.equals(entry.getConstructionProject().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Diary entry not found");
        }
        return entry;
    }

    /**
     * O diario registra o que ja aconteceu; data futura seria um lancamento invalido.
     *
     * As anotacoes de bean validation dos DTOs (@NotNull, @NotBlank, @Size) sao
     * documentacao ate que spring-boot-starter-validation entre no projeto: sem
     * o validador no classpath o @Valid do controller nao roda. Por isso o que e
     * obrigatorio e checado aqui tambem, e nao so na borda.
     */
    private Instant requireEntryDate(Instant entryDate) {
        if (entryDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Entry date is required");
        }
        if (entryDate.isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Entry date cannot be in the future");
        }
        return entryDate;
    }

    private String requireDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Description is required");
        }
        String trimmed = description.trim();
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Description must be at most " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        return trimmed;
    }

    /**
     * O responsavel precisa ser um membro ativo da obra: e ele quem responde
     * pelo registro, entao apontar alguem de fora deixaria o diario sem valor
     * de prova.
     */
    private User requireProjectMemberUser(Long projectId, Long responsibleUserId) {
        User user = userRepository.findById(responsibleUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Responsible user not found"));

        ConstructionProjectMember member = memberRepository
                .findByConstructionProjectIdAndUserId(projectId, responsibleUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Responsible user is not a member of this project"));

        if (!"ACTIVE".equals(member.getMembershipStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Responsible user is not an active member of this project");
        }
        return user;
    }

    /** O anexo vinculado tem de ser da mesma obra do registro. */
    private Attachment resolveAttachment(Long attachmentId, Long projectId) {
        if (attachmentId == null) {
            return null;
        }
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Attachment not found in this project"));
        if (attachment.getConstructionProject() == null
                || !projectId.equals(attachment.getConstructionProject().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Attachment not found in this project");
        }
        return attachment;
    }

    private int resolvePage(Integer page) {
        if (page == null) {
            return 0;
        }
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be zero or greater");
        }
        return page;
    }

    private int resolveSize(Integer size) {
        if (size == null) {
            return DEFAULT_PAGE_SIZE;
        }
        if (size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Size must be greater than zero");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Size must be at most " + MAX_PAGE_SIZE);
        }
        return size;
    }
}
