package br.pucpr.prissma_server;

import br.pucpr.prissma_server.projects.ConstructionProject;
import br.pucpr.prissma_server.projects.ConstructionProjectController;
import br.pucpr.prissma_server.projects.ConstructionProjectMember;
import br.pucpr.prissma_server.projects.AddProjectMemberRequest;
import br.pucpr.prissma_server.projects.ConstructionProjectService;
import br.pucpr.prissma_server.projects.AcompanhamentoResponse;
import br.pucpr.prissma_server.projects.AcompanhamentoStageResponse;
import br.pucpr.prissma_server.users.Role;
import br.pucpr.prissma_server.users.User;
import br.pucpr.prissma_server.users.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ConstructionProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ConstructionProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // O WorkspaceContextFilter (bean de Filter) entra no slice web mesmo com
    // addFilters=false — o mock satisfaz a construção dele.
    @MockitoBean
    private br.pucpr.prissma_server.workspaces.WorkspaceService workspaceService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ConstructionProjectService service;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private br.pucpr.prissma_server.task.TaskService taskService;

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
        p.setPlannedStartDate(LocalDate.of(2026, 6, 1));
        p.setPlannedEndDate(LocalDate.of(2026, 12, 1));
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

    private UsernamePasswordAuthenticationToken auth(Long userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    @DisplayName("POST /projects creates a project and returns 201")
    public void createProject_returnsCreated() throws Exception {
        ConstructionProject returned = sampleProject();
        try {
            java.lang.reflect.Field idField = ConstructionProject.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(returned, 1L);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }

        when(service.createProject(any(ConstructionProject.class), any(Long.class))).thenReturn(returned);

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
                .andExpect(jsonPath("$.street").value("Rua das Flores"))
                .andExpect(jsonPath("$.projectType").value("RESIDENTIAL"));
    }

    @Test
    @DisplayName("GET /projects returns a list")
    public void getAll_returnsList() throws Exception {
        ConstructionProject p = sampleProject();
        try {
            java.lang.reflect.Field idField = ConstructionProject.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(p, 2L);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser()));
        when(service.getAll()).thenReturn(List.of(p));

        mockMvc.perform(get("/projects")
                        .principal(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Obra de Teste"));
    }

    @Test
    @DisplayName("GET /projects/{id} returns project when found")
    public void getById_returnsProject() throws Exception {
        ConstructionProject p = sampleProject();
        try {
            java.lang.reflect.Field idField = ConstructionProject.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(p, 5L);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        when(service.getById(5L, 1L)).thenReturn(p);

        mockMvc.perform(get("/projects/5")
                        .principal(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectType").value("RESIDENTIAL"));
    }

    @Test
    @DisplayName("PATCH /projects/{id} updates and returns project")
    public void updateProject_returnsUpdated() throws Exception {
        ConstructionProject updated = sampleProject();
        try {
            java.lang.reflect.Field idField = ConstructionProject.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(updated, 10L);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        updated.setStreet("Av. Atualizada");
        updated.setStatus("IN_PROGRESS");

        when(service.updateProject(any(Long.class), any(), any(Long.class))).thenReturn(updated);

        String payload = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {{
            put("street", "Av. Atualizada");
            put("status", "IN_PROGRESS");
        }});

        mockMvc.perform(patch("/projects/10")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.street").value("Av. Atualizada"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("POST /projects/{id}/members creates a project member")
    public void addMember_returnsCreated() throws Exception {
        ConstructionProject project = sampleProject();
        try {
            java.lang.reflect.Field idField = ConstructionProject.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(project, 7L);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }

        User memberUser = new User();
        memberUser.setId(2L);
        memberUser.setName("Maria Santos");
        memberUser.setEmail("maria@example.com");
        memberUser.setRole(Role.USER);

        ConstructionProjectMember member = new ConstructionProjectMember();
        member.setConstructionProject(project);
        member.setUser(memberUser);
        member.setRoleInProject("FOREMAN");
        member.setMembershipStatus("ACTIVE");
        member.setJoinedAt(Instant.now());

        when(service.addMember(eq(7L), eq(1L), any(AddProjectMemberRequest.class)))
                .thenReturn(member);

        String payload = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {{
            put("userId", 2L);
            put("roleInProject", "FOREMAN");
        }});

        mockMvc.perform(post("/projects/7/members")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(7L))
                .andExpect(jsonPath("$.user.id").value(2L))
                .andExpect(jsonPath("$.roleInProject").value("FOREMAN"))
                .andExpect(jsonPath("$.membershipStatus").value("ACTIVE"));
    }

    @Test
    @DisplayName("DELETE /projects/{id} returns no content")
    public void deleteProject_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/projects/20")
                        .principal(auth(1L)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /projects/{id}/acompanhamento returns summary")
    public void getAcompanhamento_returnsSummary() throws Exception {
        Map<String, Long> stageStatusCounts = new LinkedHashMap<>();
        stageStatusCounts.put("PLANNED", 1L);
        stageStatusCounts.put("IN_PROGRESS", 0L);
        stageStatusCounts.put("BLOCKED", 0L);
        stageStatusCounts.put("DONE", 0L);

        Map<String, Long> taskStatusCounts = new LinkedHashMap<>();
        taskStatusCounts.put("TODO", 2L);
        taskStatusCounts.put("IN_PROGRESS", 1L);
        taskStatusCounts.put("BLOCKED", 0L);
        taskStatusCounts.put("DONE", 0L);

        AcompanhamentoStageResponse stageSummary = new AcompanhamentoStageResponse(
                11L, "Fundação", "Execução das fundações", 1, "PLANNED",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1),
                3, taskStatusCounts
        );

        AcompanhamentoResponse summary = new AcompanhamentoResponse(
                5L, "Obra de Teste", "PLANNING",
                1, 0,
                3, 0,
                stageStatusCounts,
                taskStatusCounts,
                List.of(stageSummary)
        );

        when(taskService.summarizeProject(5L, 1L)).thenReturn(summary);

        mockMvc.perform(get("/projects/5/acompanhamento")
                        .principal(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.obraId").value(5L))
                .andExpect(jsonPath("$.totalEtapas").value(1))
                .andExpect(jsonPath("$.totalTarefas").value(3))
                .andExpect(jsonPath("$.stageStatusCounts.PLANNED").value(1))
                .andExpect(jsonPath("$.etapas[0].name").value("Fundação"))
                .andExpect(jsonPath("$.etapas[0].totalTarefas").value(3));
    }
}
