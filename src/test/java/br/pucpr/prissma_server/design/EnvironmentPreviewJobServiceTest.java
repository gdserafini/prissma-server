package br.pucpr.prissma_server.design;

import br.pucpr.prissma_server.attachments.storage.FileStorageService;
import br.pucpr.prissma_server.attachments.storage.StoredFile;
import br.pucpr.prissma_server.attachments.storage.UploadedFileValidator;
import br.pucpr.prissma_server.genai.EnvironmentPreviewRequest;
import br.pucpr.prissma_server.genai.EnvironmentPreviewService;
import br.pucpr.prissma_server.genai.ImageBytes;
import br.pucpr.prissma_server.projects.ConstructionProject;
import br.pucpr.prissma_server.projects.ProjectPermission;
import br.pucpr.prissma_server.projects.ProjectPermissionService;
import br.pucpr.prissma_server.users.User;
import br.pucpr.prissma_server.users.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnvironmentPreviewJobService Tests")
class EnvironmentPreviewJobServiceTest {

    @Mock
    private EnvironmentPreviewRepository previewRepository;

    @Mock
    private DesignSubmissionRepository submissionRepository;

    @Mock
    private DesignProposalRepository proposalRepository;

    @Mock
    private DesignProposalService proposalService;

    @Mock
    private EnvironmentPreviewService previewService;

    @Mock
    private ProjectPermissionService permissionService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService storage;

    @Mock
    private UploadedFileValidator validator;

    @Mock
    private ApplicationEventPublisher events;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private EnvironmentPreviewJobService service;

    private static final Long PROJECT_ID = 1L;
    private static final Long PROPOSAL_ID = 10L;
    private static final Long PREVIEW_ID = 55L;
    private static final Long USER_ID = 7L;

    private DesignProposal proposta;
    private User autor;

    @BeforeEach
    void setUp() {
        service = new EnvironmentPreviewJobService(previewRepository, submissionRepository,
                proposalRepository, proposalService, previewService, permissionService,
                userRepository, storage, validator, events, objectMapper);

        ConstructionProject project = new ConstructionProject();
        project.setId(PROJECT_ID);

        autor = new User();
        autor.setId(USER_ID);
        autor.setName("Arquiteta Responsavel");

        proposta = new DesignProposal();
        proposta.setId(PROPOSAL_ID);
        proposta.setConstructionProject(project);
        proposta.setTitle("Sala de estar");
        proposta.setEnvironmentType(EnvironmentPreviewRequest.EnvironmentType.LIVING_ROOM);
        proposta.setUpdatedAt(Instant.now());
    }

    private EnvironmentPreviewRequest opcoes() {
        return new EnvironmentPreviewRequest(
                EnvironmentPreviewRequest.EnvironmentType.LIVING_ROOM,
                EnvironmentPreviewRequest.DesignStyle.MODERN,
                List.of(EnvironmentPreviewRequest.ColorPalette.OFF_WHITE),
                EnvironmentPreviewRequest.Lighting.WARM_INDIRECT,
                EnvironmentPreviewRequest.Flooring.LIGHT_PORCELAIN,
                EnvironmentPreviewRequest.GenerationMode.PREVIEW,
                "Painel ripado atras da TV");
    }

    private EnvironmentPreview previaProcessando() throws Exception {
        EnvironmentPreview preview = new EnvironmentPreview();
        preview.setId(PREVIEW_ID);
        preview.setProposal(proposta);
        preview.setStatus(PreviewStatus.PROCESSING);
        preview.setRawImageKey("proposals/10/foto.jpg");
        preview.setOptionsJson(objectMapper.writeValueAsString(opcoes()));
        preview.setRequestedByUser(autor);
        preview.setCreatedAt(Instant.now());
        return preview;
    }

    @Test
    @DisplayName("request grava a previa em PROCESSING e publica o evento depois do commit")
    void requestGravaProcessing() {
        when(proposalService.requireProposalScopedToProject(PROJECT_ID, PROPOSAL_ID)).thenReturn(proposta);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(autor));
        when(validator.normalizeContentType(any())).thenReturn("image/jpeg");
        when(validator.resolveExtension("image/jpeg")).thenReturn("jpg");
        when(storage.store(any(), eq("jpg"), eq("proposals/10")))
                .thenReturn(new StoredFile("proposals/10/foto.jpg", 100));

        var foto = new MockMultipartFile("rawImage", "sala.jpg", "image/jpeg", new byte[]{1, 2, 3});

        PreviewResponse response = service.request(PROJECT_ID, PROPOSAL_ID, foto, null, opcoes(), USER_ID);

        verify(permissionService).requirePermission(PROJECT_ID, USER_ID, ProjectPermission.MANAGE_PROPOSALS);
        verify(events).publishEvent(any(EnvironmentPreviewRequestedEvent.class));
        verifyNoInteractions(previewService);

        ArgumentCaptor<EnvironmentPreview> captor = ArgumentCaptor.forClass(EnvironmentPreview.class);
        verify(previewRepository).save(captor.capture());
        assertEquals(PreviewStatus.PROCESSING, captor.getValue().getStatus());
        assertEquals("proposals/10/foto.jpg", captor.getValue().getRawImageKey());
        assertNull(captor.getValue().getFloorPlanKey());
        assertEquals(PreviewStatus.PROCESSING, response.status());
        assertNull(response.versionId());
    }

    @Test
    @DisplayName("request sem a foto do ambiente responde 400")
    void requestSemFoto() {
        when(proposalService.requireProposalScopedToProject(PROJECT_ID, PROPOSAL_ID)).thenReturn(proposta);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.request(PROJECT_ID, PROPOSAL_ID, null, null, opcoes(), USER_ID));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(events);
    }

    @Test
    @DisplayName("process cria a versao seguinte marcada como gerada por IA e conclui em READY")
    void processCriaVersaoEConclui() throws Exception {
        EnvironmentPreview preview = previaProcessando();
        when(previewRepository.findById(PREVIEW_ID)).thenReturn(Optional.of(preview));
        when(storage.load("proposals/10/foto.jpg")).thenReturn(new ByteArrayResource(new byte[]{9, 9}));
        when(previewService.generate(any(ImageBytes.class), eq(null), any(EnvironmentPreviewRequest.class)))
                .thenReturn(new byte[]{7, 7, 7});
        when(submissionRepository.findMaxVersion(PROPOSAL_ID)).thenReturn(1);
        when(storage.store(any(), eq("png"), eq("proposals/10")))
                .thenReturn(new StoredFile("proposals/10/previa.png", 3));
        when(proposalService.buildVersion(eq(proposta), eq(autor), eq(2), any(), eq(true), any()))
                .thenAnswer(invocation -> {
                    DesignSubmission versao = new DesignSubmission();
                    versao.setId(202L);
                    versao.setProposal(proposta);
                    versao.setVersion(invocation.getArgument(2));
                    versao.setGeneratedByAi(true);
                    versao.setStatus(ProposalStatus.DRAFT);
                    return versao;
                });

        service.process(PREVIEW_ID);

        ArgumentCaptor<DesignSubmission> versaoCaptor = ArgumentCaptor.forClass(DesignSubmission.class);
        verify(submissionRepository).save(versaoCaptor.capture());
        assertEquals("proposals/10/previa.png", versaoCaptor.getValue().getFileUrl());
        assertEquals("image/png", versaoCaptor.getValue().getFileType());
        assertEquals("previa-ia-v2.png", versaoCaptor.getValue().getFileName());

        assertEquals(PreviewStatus.READY, preview.getStatus());
        assertNotNull(preview.getCompletedAt());
        assertEquals(202L, preview.getResultSubmission().getId());
        assertNull(preview.getErrorMessage());
    }

    @Test
    @DisplayName("falha da OpenAI vira FAILED com a mensagem que a tela mostra no retry")
    void processMarcaFalha() throws Exception {
        EnvironmentPreview preview = previaProcessando();
        when(previewRepository.findById(PREVIEW_ID)).thenReturn(Optional.of(preview));
        when(storage.load("proposals/10/foto.jpg")).thenReturn(new ByteArrayResource(new byte[]{9, 9}));
        when(previewService.generate(any(ImageBytes.class), eq(null), any(EnvironmentPreviewRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "A integração de imagens está sem OPENAI_API_KEY configurada"));

        service.process(PREVIEW_ID);

        assertEquals(PreviewStatus.FAILED, preview.getStatus());
        assertEquals("A integração de imagens está sem OPENAI_API_KEY configurada",
                preview.getErrorMessage());
        assertNotNull(preview.getCompletedAt());
        verify(submissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("previa que ja saiu de PROCESSING nao e reprocessada")
    void processIgnoraPreviaConcluida() throws Exception {
        EnvironmentPreview preview = previaProcessando();
        preview.setStatus(PreviewStatus.READY);
        when(previewRepository.findById(PREVIEW_ID)).thenReturn(Optional.of(preview));

        service.process(PREVIEW_ID);

        verifyNoInteractions(previewService);
        verify(previewRepository, never()).save(any());
    }
}
