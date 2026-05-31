package br.pucpr.prissma_server.projects;

import br.pucpr.prissma_server.users.Role;
import br.pucpr.prissma_server.users.User;
import br.pucpr.prissma_server.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
public class ConstructionProjectController {

    private final ConstructionProjectService service;
    private final br.pucpr.prissma_server.task.TaskService taskService;
    private final UserRepository userRepository;

    public ConstructionProjectController(ConstructionProjectService service,
                                         br.pucpr.prissma_server.task.TaskService taskService,
                                         UserRepository userRepository) {
        this.service = service;
        this.taskService = taskService;
        this.userRepository = userRepository;
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
    public ResponseEntity<ConstructionProjectResponse> createProject(@RequestBody ConstructionProjectRequest request,
                                                                     Authentication auth) {
        Long userId = resolveUserId(auth);
        ConstructionProject project = service.createProject(request.toEntity(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ConstructionProjectResponse.from(project));
    }

    @GetMapping
    public ResponseEntity<List<ConstructionProjectResponse>> getAll(Authentication auth) {
        Long userId = resolveUserId(auth);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<ConstructionProject> projects;
        if (user.getRole() == Role.ADMIN) {
            // Admin sees all projects
            projects = service.getAll();
        } else {
            // Other users see only their linked projects
            projects = service.getAllForUser(userId);
        }

        List<ConstructionProjectResponse> list = projects.stream().map(ConstructionProjectResponse::from).toList();
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

    @PostMapping("/{projectId}/members")
    public ResponseEntity<ConstructionProjectMemberResponse> addMember(@PathVariable Long projectId,
                                                                       @RequestBody AddProjectMemberRequest request,
                                                                       Authentication auth) {
        Long userId = resolveUserId(auth);
        ConstructionProjectMember member = service.addMember(projectId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ConstructionProjectMemberResponse.from(member));
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<ConstructionProjectMemberResponse>> getMembers(@PathVariable Long projectId,
                                                                              Authentication auth) {
        Long userId = resolveUserId(auth);
        List<ConstructionProjectMemberResponse> members = service.getMembers(projectId, userId);
        return ResponseEntity.ok(members);
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long projectId,
                                             @PathVariable Long userId,
                                             Authentication auth) {
        Long actorUserId = resolveUserId(auth);
        service.removeMember(projectId, actorUserId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/acompanhamento")
    public ResponseEntity<AcompanhamentoResponse> getAcompanhamento(@PathVariable Long id,
                                                                    Authentication auth) {
        Long userId = resolveUserId(auth);
        return ResponseEntity.ok(taskService.summarizeProject(id, userId));
    }
}
