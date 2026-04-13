package br.pucpr.prissma_server.projects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
public class ConstructionProjectController {

    private final ConstructionProjectService service;

    public ConstructionProjectController(ConstructionProjectService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ConstructionProjectResponse> createProject(@RequestBody ConstructionProjectRequest request) {
        ConstructionProject project = service.createProject(request.toEntity());
        return ResponseEntity.status(HttpStatus.CREATED).body(ConstructionProjectResponse.from(project));
    }

    @GetMapping
    public ResponseEntity<List<ConstructionProjectResponse>> getAll() {
        List<ConstructionProjectResponse> list = service.getAll().stream().map(ConstructionProjectResponse::from).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConstructionProjectResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ConstructionProjectResponse.from(service.getById(id)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ConstructionProjectResponse> updateProject(@PathVariable Long id,
                                                                     @RequestBody ConstructionProjectRequest request) {
        ConstructionProject updated = service.updateProject(id, request);
        return ResponseEntity.ok(ConstructionProjectResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        service.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}

