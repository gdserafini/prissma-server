package br.pucpr.prissma_server.workspaces;

import br.pucpr.prissma_server.auth.AuthService;
import br.pucpr.prissma_server.auth.LoginResponse;
import br.pucpr.prissma_server.users.User;
import br.pucpr.prissma_server.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/workspaces")
public class WorkspaceController {

    private final WorkspaceService service;
    private final AuthService authService;
    private final UserRepository userRepository;

    public WorkspaceController(WorkspaceService service,
                               AuthService authService,
                               UserRepository userRepository) {
        this.service = service;
        this.authService = authService;
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

    @GetMapping
    public ResponseEntity<List<WorkspaceResponse>> list(Authentication auth) {
        Long userId = resolveUserId(auth);
        List<WorkspaceResponse> workspaces = service.listWorkspacesForUser(userId).stream()
                .map(w -> WorkspaceResponse.from(w, userId))
                .toList();
        return ResponseEntity.ok(workspaces);
    }

    @PostMapping
    public ResponseEntity<WorkspaceResponse> create(@RequestBody CreateWorkspaceRequest request,
                                                    Authentication auth) {
        Long userId = resolveUserId(auth);
        Workspace workspace = service.createWorkspace(userId, request == null ? null : request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(WorkspaceResponse.from(workspace, userId));
    }

    /**
     * Troca de conta: valida o acesso e reemite o token com os claims do
     * workspace alvo. Sem vínculo -> 404 genérico (anti-enumeração).
     *
     * Limitação conhecida (MVP): o token antigo não é revogado — a API é
     * stateless, sem jti/sessões; ele vale até expirar (24h).
     */
    @PostMapping("/{id}/switch")
    public ResponseEntity<LoginResponse> switchWorkspace(@PathVariable Long id, Authentication auth) {
        Long userId = resolveUserId(auth);
        WorkspaceContext ctx = service.resolveContext(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return ResponseEntity.ok(new LoginResponse(authService.issueToken(user, ctx)));
    }
}
