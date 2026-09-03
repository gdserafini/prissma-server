package br.pucpr.prissma_server.diary;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;

@RestController
@RequestMapping("/projects/{projectId}/diary-entries")
public class DiaryController {

    private final DiaryService service;

    public DiaryController(DiaryService service) {
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
    public ResponseEntity<DiaryEntryResponse> create(@PathVariable Long projectId,
                                                     @Valid @RequestBody DiaryEntryRequest request,
                                                     Authentication auth) {
        DiaryEntryResponse response = service.create(projectId, request, resolveUserId(auth));
        URI location = URI.create("/projects/" + projectId + "/diary-entries/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<DiaryEntryPageResponse> list(@PathVariable Long projectId,
                                                       @RequestParam(required = false) Integer page,
                                                       @RequestParam(required = false) Integer size,
                                                       Authentication auth) {
        return ResponseEntity.ok(service.list(projectId, page, size, resolveUserId(auth)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long projectId,
                                       @PathVariable Long id,
                                       Authentication auth) {
        service.delete(projectId, id, resolveUserId(auth));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
