package br.pucpr.prissma_server.task;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/stages/{stageId}/tasks")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    private Long resolveUserId(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        if (principal instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(auth.getName());
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@PathVariable Long stageId,
                                               @Valid @RequestBody TaskRequest request,
                                               Authentication auth) {
        Long userId = resolveUserId(auth);
        TaskResponse response = service.create(stageId, request, userId);
        return ResponseEntity.created(URI.create("/stages/" + stageId + "/tasks/" + response.getId()))
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> list(@PathVariable Long stageId,
                                                   Authentication auth) {
        Long userId = resolveUserId(auth);
        return ResponseEntity.ok(service.listByStage(stageId, userId));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> get(@PathVariable Long stageId,
                                            @PathVariable Long taskId,
                                            Authentication auth) {
        Long userId = resolveUserId(auth);
        return ResponseEntity.ok(service.get(stageId, taskId, userId));
    }

    @PatchMapping("/{taskId}")
    public ResponseEntity<TaskResponse> update(@PathVariable Long stageId,
                                               @PathVariable Long taskId,
                                               @Valid @RequestBody TaskRequest request,
                                               Authentication auth) {
        Long userId = resolveUserId(auth);
        return ResponseEntity.ok(service.update(stageId, taskId, request, userId));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> delete(@PathVariable Long stageId,
                                       @PathVariable Long taskId,
                                       Authentication auth) {
        Long userId = resolveUserId(auth);
        service.delete(stageId, taskId, userId);
        return ResponseEntity.noContent().build();
    }
}


