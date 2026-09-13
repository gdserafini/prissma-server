package br.pucpr.prissma_server.design;

import br.pucpr.prissma_server.attachments.storage.FileStorageService;
import br.pucpr.prissma_server.attachments.storage.UploadedFileValidator;
import br.pucpr.prissma_server.genai.EnvironmentPreviewRequest;
import br.pucpr.prissma_server.projects.ConstructionProject;
import br.pucpr.prissma_server.projects.ConstructionProjectRepository;
import br.pucpr.prissma_server.projects.ProjectPermission;
import br.pucpr.prissma_server.projects.ProjectPermissionService;
import br.pucpr.prissma_server.users.User;
import br.pucpr.prissma_server.users.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DesignProposalService Tests")
class DesignProposalServiceTest {

    @Mock
    private DesignProposalRepository proposalRepository;

    @Mock
    private DesignSubmissionRepository submissionRepository;

    @Mock
    private DesignApprovalRepository approvalRepository;

    @Mock
    private EnvironmentPreviewRepository previewRepository;

    @Mock
    private ConstructionProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectPermissionService permissionService;

    @Mock
    private FileStorageService storage;

    @Mock
    private UploadedFileValidator validator;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private DesignProposalService service;

    private static final Long PROJECT_ID = 1L;
    private static final Long PROPOSAL_ID = 10L;
    private static final Long USER_ID = 7L;

    private ConstructionProject project;
    private User autor;
    private DesignProposal proposta;

    @BeforeEach
    void setUp() {
        project = new ConstructionProject();
        project.setId(PROJECT_ID);
        project.setTitle("Reforma Vila Nova");

        autor = new User();
        autor.setId(USER_ID);
        autor.setName("Arquiteta Responsavel");

        proposta = new DesignProposal();
        proposta.setId(PROPOSAL_ID);
        proposta.setConstructionProject(project);
        proposta.setTitle("Sala de estar");
        proposta.setEnvironmentType(EnvironmentPreviewRequest.EnvironmentType.LIVING_ROOM);
        proposta.setCreatedByUser(autor);
        proposta.setCreatedByName(autor.getName());
        proposta.setCreatedAt(Instant.now());
        proposta.setUpdatedAt(Instant.now());
    }

    private ProposalRequest requisicaoValida() {
        return new ProposalRequest("Sala de estar", "Painel ripado atras da TV", "LIVING_ROOM", null);
    }

    private DesignSubmission versao(Long id, int numero, ProposalStatus status, String fileUrl) {
        DesignSubmission versao = new DesignSubmission();
        versao.setId(id);
        versao.setProposal(proposta);
        versao.setVersion(numero);
        versao.setStatus(status);
        versao.setFileUrl(fileUrl);
        versao.setAuthorUser(autor);
        versao.setAuthorName(autor.getName());
        versao.setSubmittedAt(Instant.now());
        versao.setUpdatedAt(Instant.now());
        return versao;
    }

    @Test
    @DisplayName("create exige MANAGE_PROPOSALS e cria a proposta ja com a v1")
    void createCriaPropostaComPrimeiraVersao() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(proposalRepository.existsByConstructionProjectIdAndTitleIgnoreCase(PROJECT_ID, "Sala de estar"))
                .thenReturn(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(autor));

        ProposalResponse response = service.create(PROJECT_ID, requisicaoValida(), null, USER_ID);

        verify(permissionService).requirePermission(PROJECT_ID, USER_ID, ProjectPermission.MANAGE_PROPOSALS);

        ArgumentCaptor<DesignSubmission> captor = ArgumentCaptor.forClass(DesignSubmission.class);
        verify(submissionRepository).save(captor.capture());
        DesignSubmission primeira = captor.getValue();
        assertEquals(1, primeira.getVersion());
        assertEquals(ProposalStatus.DRAFT, primeira.getStatus());
        assertFalse(primeira.isGeneratedByAi());
        assertEquals("Arquiteta Responsavel", primeira.getAuthorName());

        assertEquals(1, response.versionCount());
        assertEquals(1, response.latestVersion().version());
        assertFalse(response.latestVersion().hasImage());
    }

    @Test
    @DisplayName("create recusa titulo repetido na mesma obra com 409")
    void createRecusaTituloRepetido() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(proposalRepository.existsByConstructionProjectIdAndTitleIgnoreCase(PROJECT_ID, "Sala de estar"))
                .thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(PROJECT_ID, requisicaoValida(), null, USER_ID));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(proposalRepository, never()).save(any());
    }

    @Test
    @DisplayName("create recusa ambiente fora do enum com 400")
    void createRecusaAmbienteInvalido() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(proposalRepository.existsByConstructionProjectIdAndTitleIgnoreCase(anyLong(), any()))
                .thenReturn(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(autor));

        ProposalRequest request = new ProposalRequest("Sala de estar", null, "VARANDA_GOURMET", null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(PROJECT_ID, request, null, USER_ID));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("list monta o card com a versao mais recente e a contagem, numa consulta so")
    void listMontaCards() {
        DesignSubmission v2 = versao(102L, 2, ProposalStatus.APPROVED, "proposals/10/b.png");
        DesignSubmission v1 = versao(101L, 1, ProposalStatus.REJECTED, "proposals/10/a.png");

        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(proposalRepository.findPageByProject(eq(PROJECT_ID), any()))
                .thenReturn(new PageImpl<>(List.of(proposta), PageRequest.of(0, 20), 1));
        when(submissionRepository.findAllByProposalIds(List.of(PROPOSAL_ID)))
                .thenReturn(List.of(v2, v1));

        ProposalPageResponse response = service.list(PROJECT_ID, null, null, USER_ID);

        verify(permissionService).requirePermission(PROJECT_ID, USER_ID, ProjectPermission.VIEW_PROJECT);
        verify(submissionRepository, times(1)).findAllByProposalIds(any());

        assertEquals(1, response.content().size());
        ProposalResponse card = response.content().getFirst();
        assertEquals(2, card.versionCount());
        assertEquals(2, card.latestVersion().version());
        assertEquals(ProposalStatus.APPROVED, card.latestVersion().status());
        assertTrue(card.latestVersion().hasImage());
        assertNull(card.versions(), "o card nao carrega o historico inteiro");
    }

    @Test
    @DisplayName("proposta de outra obra responde 404, nunca 403")
    void propostaDeOutraObraResponde404() {
        ConstructionProject outra = new ConstructionProject();
        outra.setId(99L);
        proposta.setConstructionProject(outra);
        when(proposalRepository.findById(PROPOSAL_ID)).thenReturn(Optional.of(proposta));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.get(PROJECT_ID, PROPOSAL_ID, USER_ID));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verifyNoInteractions(permissionService);
    }

    @Test
    @DisplayName("addVersion numera a versao seguinte")
    void addVersionNumeraSeguinte() {
        when(proposalRepository.findById(PROPOSAL_ID)).thenReturn(Optional.of(proposta));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(autor));
        when(submissionRepository.findMaxVersion(PROPOSAL_ID)).thenReturn(2);
        when(validator.normalizeContentType(any())).thenReturn("image/png");
        when(validator.sanitizeFileName(any())).thenReturn("planta.png");
        when(validator.resolveExtension("image/png")).thenReturn("png");
        when(storage.store(any(), eq("png"), eq("proposals/10")))
                .thenReturn(new br.pucpr.prissma_server.attachments.storage.StoredFile("proposals/10/x.png", 12));

        var arquivo = new org.springframework.mock.web.MockMultipartFile(
                "file", "planta.png", "image/png", new byte[]{1, 2, 3});

        ProposalVersionResponse response =
                service.addVersion(PROJECT_ID, PROPOSAL_ID, "ajuste do painel", arquivo, USER_ID);

        assertEquals(3, response.version());
        assertTrue(response.hasImage());
        assertFalse(response.generatedByAi());
        verify(permissionService).requirePermission(PROJECT_ID, USER_ID, ProjectPermission.MANAGE_PROPOSALS);
    }

    @Test
    @DisplayName("addVersion sem arquivo responde 400")
    void addVersionSemArquivo() {
        when(proposalRepository.findById(PROPOSAL_ID)).thenReturn(Optional.of(proposta));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.addVersion(PROJECT_ID, PROPOSAL_ID, null, null, USER_ID));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("aprovar uma versao registra o parecer em design_approvals")
    void aprovarRegistraParecer() {
        DesignSubmission v1 = versao(101L, 1, ProposalStatus.PENDING_REVIEW, "proposals/10/a.png");
        when(proposalRepository.findById(PROPOSAL_ID)).thenReturn(Optional.of(proposta));
        when(submissionRepository.findById(101L)).thenReturn(Optional.of(v1));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(autor));

        ProposalVersionResponse response = service.changeStatus(PROJECT_ID, PROPOSAL_ID, 101L,
                new ProposalStatusRequest("APPROVED", "Pode seguir para execucao"), USER_ID);

        assertEquals(ProposalStatus.APPROVED, response.status());

        ArgumentCaptor<DesignApproval> captor = ArgumentCaptor.forClass(DesignApproval.class);
        verify(approvalRepository).save(captor.capture());
        assertEquals("APPROVED", captor.getValue().getApprovalStatus());
        assertEquals("Pode seguir para execucao", captor.getValue().getComment());
    }

    @Test
    @DisplayName("mandar para analise nao gera parecer, so muda o status")
    void analiseNaoGeraParecer() {
        DesignSubmission v1 = versao(101L, 1, ProposalStatus.DRAFT, null);
        when(proposalRepository.findById(PROPOSAL_ID)).thenReturn(Optional.of(proposta));
        when(submissionRepository.findById(101L)).thenReturn(Optional.of(v1));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(autor));

        service.changeStatus(PROJECT_ID, PROPOSAL_ID, 101L,
                new ProposalStatusRequest("PENDING_REVIEW", null), USER_ID);

        verifyNoInteractions(approvalRepository);
    }

    @Test
    @DisplayName("delete apaga os arquivos das versoes e das previas junto com a proposta")
    void deleteApagaArquivos() {
        when(proposalRepository.findById(PROPOSAL_ID)).thenReturn(Optional.of(proposta));
        when(submissionRepository.findFileUrlsByProposal(PROPOSAL_ID))
                .thenReturn(List.of("proposals/10/a.png"));
        when(previewRepository.findRawImageKeysByProposal(PROPOSAL_ID))
                .thenReturn(List.of("proposals/10/foto.jpg"));
        when(previewRepository.findFloorPlanKeysByProposal(PROPOSAL_ID))
                .thenReturn(List.of("proposals/10/planta.png"));

        service.delete(PROJECT_ID, PROPOSAL_ID, USER_ID);

        verify(storage).delete("proposals/10/a.png");
        verify(storage).delete("proposals/10/foto.jpg");
        verify(storage).delete("proposals/10/planta.png");
        verify(proposalRepository).delete(proposta);
    }

    /**
     * A regressao do 500 no DELETE: carregar as versoes como entidades deixava
     * filhas gerenciadas apontando para a proposta removida, e o flush do commit
     * estourava TransientObjectException. O delete nao pode materializa-las.
     */
    @Test
    @DisplayName("delete nao materializa as versoes da proposta")
    void deleteNaoCarregaVersoes() {
        when(proposalRepository.findById(PROPOSAL_ID)).thenReturn(Optional.of(proposta));

        service.delete(PROJECT_ID, PROPOSAL_ID, USER_ID);

        verify(submissionRepository, never()).findAllByProposalOrderByVersionDesc(anyLong());
    }

    @Test
    @DisplayName("imagem de versao ainda sem arquivo responde 404")
    void imagemDeVersaoSemArquivo() {
        DesignSubmission v1 = versao(101L, 1, ProposalStatus.DRAFT, null);
        when(proposalRepository.findById(PROPOSAL_ID)).thenReturn(Optional.of(proposta));
        when(submissionRepository.findById(101L)).thenReturn(Optional.of(v1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.loadVersionImage(PROJECT_ID, PROPOSAL_ID, 101L, USER_ID));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verifyNoInteractions(storage);
    }
}
