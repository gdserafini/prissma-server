package br.pucpr.prissma_server.task;

import br.pucpr.prissma_server.projects.ConstructionProject;
import br.pucpr.prissma_server.stage.Stage;
import br.pucpr.prissma_server.users.Role;
import br.pucpr.prissma_server.users.User;
import br.pucpr.prissma_server.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService Tests")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService service;

    private User user;
    private Task task;

    private void setId(Object entity, Long id) {
        try {
            java.lang.reflect.Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(10L);
        user.setName("João Silva");
        user.setEmail("joao@example.com");
        user.setRole(Role.ENG);

        ConstructionProject project = new ConstructionProject();
        project.setId(1L);
        project.setTitle("Obra de Teste");

        Stage stage = new Stage();
        stage.setId(2L);
        stage.setName("Fundação");
        stage.setConstructionProject(project);

        task = new Task();
        setId(task, 100L);
        task.setStage(stage);
        task.setAssigneeUser(user);
        task.setTitle("Escavação");
        task.setDescription("Executar escavação da fundação");
        task.setPriority("HIGH");
        task.setStatus("TODO");
        task.setPlannedStartDate(LocalDate.of(2026, 6, 1));
        task.setPlannedEndDate(LocalDate.of(2026, 6, 5));
        task.setCreatedAt(Instant.parse("2026-06-01T10:00:00Z"));
        task.setUpdatedAt(Instant.parse("2026-06-01T10:00:00Z"));
    }

    @Test
    @DisplayName("Should list tasks assigned to a user")
    void listAssignedToUser_succeeds() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(taskRepository.findByAssigneeUserIdOrderByCreatedAtAscIdAsc(10L)).thenReturn(List.of(task));

        List<TaskResponse> responses = service.listAssignedToUser(10L);

        assertEquals(1, responses.size());
        assertEquals(100L, responses.getFirst().getId());
        assertEquals(2L, responses.getFirst().getStageId());
        assertEquals("Fundação", responses.getFirst().getStageName());
        assertEquals(10L, responses.getFirst().getAssigneeUserId());
        assertEquals("João Silva", responses.getFirst().getAssigneeName());
        assertEquals("ENG", responses.getFirst().getAssigneeRole());
    }

    @Test
    @DisplayName("Should reject task listing when user does not exist")
    void listAssignedToUser_userNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.listAssignedToUser(99L));

        assertEquals("User not found", exception.getReason());
    }
}


