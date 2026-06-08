package br.pucpr.prissma_server.users;

import br.pucpr.prissma_server.task.TaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserValidator userValidator;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Should reject user deletion when there are linked tasks")
    void deleteUser_whenUserHasLinkedTask_throwsBadRequest() {
        when(userRepository.existsById(10L)).thenReturn(true);
        when(taskRepository.existsByAssigneeUserId(10L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.deleteUser(10L));

        assertEquals("Não é possível excluir o usuário pois há tarefa vinculada", exception.getReason());
        verify(userRepository, never()).deleteById(10L);
    }

    @Test
    @DisplayName("Should delete user when there are no linked tasks")
    void deleteUser_whenUserHasNoLinkedTask_deletesUser() {
        when(userRepository.existsById(10L)).thenReturn(true);
        when(taskRepository.existsByAssigneeUserId(10L)).thenReturn(false);

        userService.deleteUser(10L);

        verify(userRepository).deleteById(10L);
    }
}
