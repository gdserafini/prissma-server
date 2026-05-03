package br.pucpr.prissma_server;

import br.pucpr.prissma_server.projects.ConstructionProject;
import br.pucpr.prissma_server.projects.ConstructionProjectController;
import br.pucpr.prissma_server.projects.ConstructionProjectMemberRepository;
import br.pucpr.prissma_server.projects.ConstructionProjectRepository;
import br.pucpr.prissma_server.projects.ConstructionProjectService;
import br.pucpr.prissma_server.users.Role;
import br.pucpr.prissma_server.users.User;
import br.pucpr.prissma_server.users.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ConstructionProjectController.class)
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

    @Test
    @DisplayName("POST /projects -> 201 when created")
    void postCreateProject() throws Exception {
        when(repository.existsByTitle("Obra de Teste")).thenReturn(false);
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
                        .with(authentication(new UsernamePasswordAuthenticationToken(1L, null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Obra de Teste"))
                .andExpect(jsonPath("$.street").value("Rua das Flores"));
    }

    @Test
    @DisplayName("POST /projects -> 400 on duplicate title")
    void postCreateProject_duplicate() throws Exception {
        when(repository.existsByTitle("Obra de Teste")).thenReturn(true);

        String payload = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {{
            put("title", "Obra de Teste");
        }});

        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /projects -> 200 and list")
    void getAllProjects() throws Exception {
        ConstructionProject p = sampleProject();
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser()));
        when(memberRepository.findAllProjectsByUserId(2L)).thenReturn(List.of(p));

        mockMvc.perform(get("/projects")
                        .with(authentication(new UsernamePasswordAuthenticationToken(2L, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Obra de Teste"));
    }

    @Test
    @DisplayName("GET /projects/{id} -> 404 when not found")
    void getById_notFound() throws Exception {
        when(repository.findById(10L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/projects/10"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /projects/{id} -> 200 when updated")
    void patchUpdateProject() throws Exception {
        ConstructionProject existing = sampleProject();
        when(repository.findById(20L)).thenReturn(Optional.of(existing));
        when(repository.existsByTitle("New Title")).thenReturn(false);
        when(repository.save(any(ConstructionProject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String payload = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {{
            put("title", "New Title");
            put("street", "Av. Atualizada");
            put("status", "IN_PROGRESS");
        }});

        mockMvc.perform(patch("/projects/20")
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
        when(repository.existsByTitle("Other")).thenReturn(true);

        String payload = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {{
            put("title", "Other");
        }});

        mockMvc.perform(patch("/projects/21")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /projects/{id} -> 204 when deleted")
    void deleteProject_success() throws Exception {
        when(repository.existsById(33L)).thenReturn(true);
        doNothing().when(repository).deleteById(33L);

        mockMvc.perform(delete("/projects/33"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /projects/{id} -> 404 when not found")
    void deleteProject_notFound() throws Exception {
        when(repository.existsById(34L)).thenReturn(false);

        mockMvc.perform(delete("/projects/34"))
                .andExpect(status().isNotFound());
    }
}

