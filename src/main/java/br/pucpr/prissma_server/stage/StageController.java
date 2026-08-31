package br.pucpr.prissma_server.stage;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping
public class StageController {

    private final StageService service;

    public StageController(StageService service) {
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

    @PostMapping("/projects/{projectId}/stages")
    public ResponseEntity<StageResponse> create(
            @PathVariable Long projectId,
            @Valid @RequestBody StageRequest request,
            Authentication auth) {
        Long userId = resolveUserId(auth);
        StageResponse response = service.create(projectId, request, userId);
        return ResponseEntity.created(URI.create("/stages/" + response.getId())).body(response);
    }

    @GetMapping("/projects/{projectId}/stages")
    public ResponseEntity<List<StageResponse>> listByProject(@PathVariable Long projectId,
                                                             Authentication auth) {
        List<StageResponse> stages = service.listByProject(projectId, resolveUserId(auth));
        return ResponseEntity.ok(stages);
    }

    @GetMapping("/stages/{id}")
    public ResponseEntity<StageResponse> get(@PathVariable Long id, Authentication auth) {
        StageResponse response = service.get(id, resolveUserId(auth));
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/stages/{id}")
    public ResponseEntity<StageResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody StageRequest request,
            Authentication auth) {
        Long userId = resolveUserId(auth);
        StageResponse response = service.update(id, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/stages/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication auth) {
        Long userId = resolveUserId(auth);
        service.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/projects/{projectId}/stages/reorder")
    public ResponseEntity<StageReorderResponse> reorder(
            @PathVariable Long projectId,
            @RequestBody List<Long> stageIds,
            Authentication auth) {
        Long userId = resolveUserId(auth);
        service.reorder(projectId, stageIds, userId);
        List<StageResponse> stages = service.listByProject(projectId, userId);
        return ResponseEntity.ok(new StageReorderResponse("Stages reordered successfully", stages));
    }
}


