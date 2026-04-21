package br.pucpr.prissma_server;

import br.pucpr.prissma_server.projects.ConstructionProject;
import br.pucpr.prissma_server.projects.ConstructionProjectController;
import br.pucpr.prissma_server.projects.ConstructionProjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ConstructionProjectController.class)
@AutoConfigureMockMvc(addFilters = false) // disable security filters for controller unit tests
public class ConstructionProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConstructionProjectService service; // injected mock from TestConfiguration

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ConstructionProjectService constructionProjectService() {
            return Mockito.mock(ConstructionProjectService.class);
        }
    }

    private ConstructionProject sampleProject() {
        ConstructionProject p = new ConstructionProject();
        p.setTitle("Obra de Teste");
        p.setAddress("Rua das Flores, 100");
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

        when(service.createProject(any(ConstructionProject.class))).thenReturn(returned);

        String payload = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {{
            put("title", "Obra de Teste");
            put("address", "Rua das Flores, 100");
            put("projectType", "RESIDENTIAL");
            put("category", "HOUSE");
            put("landArea", 250.00);
            put("builtArea", 180.00);
            put("status", "PLANNING");
            put("plannedStartDate", "2026-06-01");
            put("plannedEndDate", "2026-12-01");
        }});

        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Obra de Teste"))
                .andExpect(jsonPath("$.address").value("Rua das Flores, 100"))
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
        when(service.getAll()).thenReturn(List.of(p));

        mockMvc.perform(get("/projects"))
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
        when(service.getById(5L)).thenReturn(p);

        mockMvc.perform(get("/projects/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectType").value("RESIDENTIAL"));
    }

    @Test
    @DisplayName("PATCH /projects/{id} updates and returns project")
    public void updateProject_returnsUpdated() throws Exception {
        ConstructionProject existing = sampleProject();
        try {
            java.lang.reflect.Field idField = ConstructionProject.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(existing, 10L);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }

        ConstructionProject updated = sampleProject();
        try {
            java.lang.reflect.Field idField = ConstructionProject.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(updated, 10L);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        updated.setAddress("Av. Atualizada, 200");
        updated.setStatus("IN_PROGRESS");

        when(service.updateProject(any(Long.class), any())).thenReturn(updated);

        String payload = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {{
            put("address", "Av. Atualizada, 200");
            put("status", "IN_PROGRESS");
        }});

        mockMvc.perform(patch("/projects/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("Av. Atualizada, 200"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("DELETE /projects/{id} returns no content")
    public void deleteProject_returnsNoContent() throws Exception {
        // service.deleteProject doesn't return value; just ensure endpoint returns 204
        mockMvc.perform(delete("/projects/20"))
                .andExpect(status().isNoContent());
    }
}
