package br.pucpr.prissma_server;

import br.pucpr.prissma_server.projects.ConstructionProject;
import br.pucpr.prissma_server.projects.ConstructionProjectController;
import br.pucpr.prissma_server.projects.ConstructionProjectMember;
import br.pucpr.prissma_server.projects.ConstructionProjectMemberRepository;
import br.pucpr.prissma_server.projects.ConstructionProjectRepository;
import br.pucpr.prissma_server.projects.ConstructionProjectService;
import br.pucpr.prissma_server.task.TaskService;
import br.pucpr.prissma_server.users.Role;
import br.pucpr.prissma_server.users.User;
import br.pucpr.prissma_server.users.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ConstructionProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ConstructionProjectService.class)
public class ConstructionProjectControllerServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ConstructionProjectRepository repository;

    @MockitoBean
    private ConstructionProjectMemberRepository memberRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TaskService taskService;

    // ConstructionProjectService (importado real via @Import) exige 5 colaboradores;
    // sem estes dois mocks o contexto do slice nem sobe (NoSuchBeanDefinitionException).
    @MockitoBean
    private br.pucpr.prissma_server.projects.ProjectPermissionService permissionService;

    @MockitoBean
    private br.pucpr.prissma_server.task.TaskRepository taskRepository;

    // O WorkspaceContextFilter é um bean de Filter e entra no slice web mesmo
    // com addFilters=false (ele é construído, só não aplicado) — precisa do
    // WorkspaceService satisfeito.
    @MockitoBean
    private br.pucpr.prissma_server.workspaces.WorkspaceService workspaceService;

    private ConstructionProject sampleProject() {
        ConstructionProject p = new ConstructionProject();
        p.setTitle("Obra de Teste");
        p.setCep("80000-000");
        p.setStreet("Rua das Flores");
        p.setCity("Curitiba");
        p.setState("PR");
        p.setNumber("100");
        p.setComplement("Apto 12");
        p.setProjectType("RESIDENTIAL");
        p.setCategory("HOUSE");
        p.setLandArea(new BigDecimal("250.00"));
        p.setBuiltArea(new BigDecimal("180.00"));
        p.setStatus("PLANNING");
        p.setPlannedStartDate(LocalDate.of(2026,6,1));
        p.setPlannedEndDate(LocalDate.of(2026,12,1));
        p.setWorkspaceId(5L);
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        return p;
    }

    private User adminUser() {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.ADMIN);
        return user;
    }

    private User regularUser() {
        User user = new User();
        user.setId(2L);
        user.setRole(Role.USER);
        return user;
    }

    private UsernamePasswordAuthenticationToken auth(Long userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    /**
     * O slice roda com addFilters=false, então o WorkspaceContextFilter não
     * existe aqui — o contexto de tenant é posto direto no SecurityContextHolder
     * (MockMvc executa na mesma thread).
     */
    private void workspaceContext(Long userId, Long workspaceId,
                                  br.pucpr.prissma_server.workspaces.WorkspaceRole role, boolean owner) {
        UsernamePasswordAuthenticationToken authentication = auth(userId);
        authentication.setDetails(new br.pucpr.prissma_server.workspaces.WorkspaceContext(workspaceId, role, owner));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @org.junit.jupiter.api.AfterEach
    void clearSecurityContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /projects -> 201 when created")
    void postCreateProject() throws Exception {
        workspaceContext(1L, 5L, br.pucpr.prissma_server.workspaces.WorkspaceRole.OWNER, true);
        when(repository.existsByWorkspaceIdAndTitle(5L, "Obra de Teste")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser()));
        when(repository.save(any(ConstructionProject.class))).thenAnswer(invocation -> {
            ConstructionProject p = invocation.getArgument(0);
            try {
                java.lang.reflect.Field idField = ConstructionProject.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(p, 1L);
            } catch (Exception ignored) {}
            return p;
        });

        String payload = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {{
            put("title", "Obra de Teste");
            put("cep", "80000-000");
            put("street", "Rua das Flores");
            put("city", "Curitiba");
            put("state", "PR");
            put("number", "100");
            put("complement", "Apto 12");
            put("projectType", "RESIDENTIAL");
            put("category", "HOUSE");
            put("landArea", 250.00);
            put("builtArea", 180.00);
            put("status", "PLANNING");
            put("plannedStartDate", "2026-06-01");
            put("plannedEndDate", "2026-12-01");
        }});

        mockMvc.perform(post("/projects")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Obra de Teste"))
                .andExpect(jsonPath("$.street").value("Rua das Flores"));
    }

    @Test
    @DisplayName("POST /projects -> 400 on duplicate title")
    void postCreateProject_duplicate() throws Exception {
        workspaceContext(1L, 5L, br.pucpr.prissma_server.workspaces.WorkspaceRole.OWNER, true);
        when(repository.existsByWorkspaceIdAndTitle(5L, "Obra de Teste")).thenReturn(true);

        String payload = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {{
            put("title", "Obra de Teste");
        }});

        mockMvc.perform(post("/projects")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /projects -> 200 and list")
    void getAllProjects() throws Exception {
        ConstructionProject p = sampleProject();
        workspaceContext(2L, 5L, br.pucpr.prissma_server.workspaces.WorkspaceRole.MEMBER, false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser()));
        when(memberRepository.findAllProjectsByUserIdAndWorkspaceId(2L, 5L)).thenReturn(List.of(p));

        mockMvc.perform(get("/projects")
                        .principal(auth(2L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Obra de Teste"));
    }

    @Test
    @DisplayName("GET /projects/{id} -> 404 when not found")
    void getById_notFound() throws Exception {
        when(repository.findById(10L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/projects/10")
                        .principal(auth(1L)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /projects/{id} -> 200 when updated")
    void patchUpdateProject() throws Exception {
        ConstructionProject existing = sampleProject();
        when(repository.findById(20L)).thenReturn(Optional.of(existing));
        when(repository.existsByWorkspaceIdAndTitle(5L, "New Title")).thenReturn(false);
        when(repository.save(any(ConstructionProject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String payload = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {{
            put("title", "New Title");
            put("street", "Av. Atualizada");
            put("status", "IN_PROGRESS");
        }});

        mockMvc.perform(patch("/projects/20")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"))
                .andExpect(jsonPath("$.street").value("Av. Atualizada"));
    }

    @Test
    @DisplayName("PATCH /projects/{id} -> 400 when new title duplicates")
    void patchUpdateProject_duplicateTitle() throws Exception {
        ConstructionProject existing = sampleProject();
        when(repository.findById(21L)).thenReturn(Optional.of(existing));
        when(repository.existsByWorkspaceIdAndTitle(5L, "Other")).thenReturn(true);

        String payload = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {{
            put("title", "Other");
        }});

        mockMvc.perform(patch("/projects/21")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /projects/{id}/members -> 201 when member is added")
    void postAddProjectMember() throws Exception {
        ConstructionProject project = sampleProject();
        project.setId(40L);
        User target = new User();
        target.setId(4L);
        target.setName("Carlos Engineer");
        target.setEmail("carlos@example.com");
        target.setRole(Role.USER);

        User actor = adminUser();
        actor.setRole(Role.USER);

        ConstructionProjectMember actorMember = new ConstructionProjectMember();
        actorMember.setConstructionProject(project);
        actorMember.setUser(actor);
        actorMember.setRoleInProject("OWNER");
        actorMember.setMembershipStatus("ACTIVE");
        actorMember.setJoinedAt(Instant.now());

        when(repository.findById(40L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(memberRepository.findByConstructionProjectIdAndUserId(40L, 1L)).thenReturn(Optional.of(actorMember));
        when(userRepository.findById(4L)).thenReturn(Optional.of(target));
        when(memberRepository.findByConstructionProjectIdAndUserId(40L, 4L)).thenReturn(Optional.empty());
        when(memberRepository.save(org.mockito.ArgumentMatchers.any(ConstructionProjectMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String payload = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {{
            put("userId", 4L);
            put("roleInProject", "ENGINEER");
        }});

        mockMvc.perform(post("/projects/40/members")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(40L))
                .andExpect(jsonPath("$.user.id").value(4L))
                .andExpect(jsonPath("$.roleInProject").value("ENGINEER"));
    }

    @Test
    @DisplayName("POST /projects/{id}/members -> 409 when member already exists")
    void postAddProjectMember_duplicate() throws Exception {
        ConstructionProject project = sampleProject();
        project.setId(41L);
        User actor = adminUser();
        actor.setRole(Role.USER);
        ConstructionProjectMember actorMember = new ConstructionProjectMember();
        actorMember.setConstructionProject(project);
        actorMember.setUser(actor);
        actorMember.setRoleInProject("OWNER");
        actorMember.setMembershipStatus("ACTIVE");
        actorMember.setJoinedAt(Instant.now());

        User target = regularUser();

        when(repository.findById(41L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(memberRepository.findByConstructionProjectIdAndUserId(41L, 1L)).thenReturn(Optional.of(actorMember));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(memberRepository.findByConstructionProjectIdAndUserId(41L, 2L)).thenReturn(Optional.of(new ConstructionProjectMember()));

        String payload = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {{
            put("userId", 2L);
            put("roleInProject", "FOREMAN");
        }});

        mockMvc.perform(post("/projects/41/members")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /projects/{id}/members -> 200 when caller can access the project")
    void getProjectMembers() throws Exception {
        ConstructionProject project = sampleProject();
        project.setId(50L);

        User owner = adminUser();
        owner.setRole(Role.USER);

        User engineer = new User();
        engineer.setId(4L);
        engineer.setName("Carlos Engineer");
        engineer.setEmail("carlos@example.com");
        engineer.setRole(Role.USER);

        ConstructionProjectMember ownerMember = new ConstructionProjectMember();
        ownerMember.setConstructionProject(project);
        ownerMember.setUser(owner);
        ownerMember.setRoleInProject("OWNER");
        ownerMember.setMembershipStatus("ACTIVE");
        ownerMember.setJoinedAt(Instant.now());

        ConstructionProjectMember engineerMember = new ConstructionProjectMember();
        engineerMember.setConstructionProject(project);
        engineerMember.setUser(engineer);
        engineerMember.setRoleInProject("ENGINEER");
        engineerMember.setMembershipStatus("ACTIVE");
        engineerMember.setJoinedAt(Instant.now());

        when(repository.findById(50L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(memberRepository.findByConstructionProjectIdAndUserId(50L, 1L)).thenReturn(Optional.of(ownerMember));
        when(memberRepository.findAllByConstructionProjectIdOrderByJoinedAtAscIdAsc(50L))
                .thenReturn(List.of(ownerMember, engineerMember));

        mockMvc.perform(get("/projects/50/members")
                        .principal(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].user.id").value(1L))
                .andExpect(jsonPath("$[1].user.id").value(4L))
                .andExpect(jsonPath("$[1].roleInProject").value("ENGINEER"));
    }

    @Test
    @DisplayName("DELETE /projects/{id} -> 204 when deleted")
    void deleteProject_success() throws Exception {
        when(repository.existsById(33L)).thenReturn(true);
        doNothing().when(repository).deleteById(33L);

        mockMvc.perform(delete("/projects/33")
                        .principal(auth(1L)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /projects/{id} -> 404 when not found")
    void deleteProject_notFound() throws Exception {
        when(repository.existsById(34L)).thenReturn(false);

        mockMvc.perform(delete("/projects/34")
                        .principal(auth(1L)))
                .andExpect(status().isNotFound());
    }
}

