package br.pucpr.prissma_server.workspaces;

import br.pucpr.prissma_server.users.User;
import br.pucpr.prissma_server.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class WorkspaceService {

    /** Teto de workspaces por dono (decisão A5 do plano). */
    static final int MAX_WORKSPACES_PER_OWNER = 10;

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;

    public WorkspaceService(WorkspaceRepository workspaceRepository,
                            WorkspaceMemberRepository memberRepository,
                            UserRepository userRepository) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    /**
     * Garante o workspace primário do usuário. Roda NO LOGIN, idempotente.
     *
     * Membro puro (só convidado para a conta de outra pessoa, sem workspace
     * próprio) devolve null DE PROPÓSITO: criar uma conta vazia para quem foi
     * convidado a trabalhar na conta alheia só polui o seletor.
     */
    @Transactional
    public Workspace ensurePrimaryWorkspace(Long userId) {
        Optional<Workspace> primary = workspaceRepository.findByOwnerIdAndPrimaryTrueAndDeletedAtIsNull(userId);
        if (primary.isPresent()) {
            return primary.get();
        }

        boolean isPureMember = workspaceRepository.countByOwnerIdAndDeletedAtIsNull(userId) == 0
                && !memberRepository.findAllByUserIdAndActiveTrueAndDeletedAtIsNullOrderByIdAsc(userId).isEmpty();
        if (isPureMember) {
            return null;
        }
        if (workspaceRepository.countByOwnerIdAndDeletedAtIsNull(userId) > 0) {
            // Possui workspaces mas nenhum primário (estado raro) — não inventar outro.
            return null;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return createWorkspaceInternal(user.getId(), "Obras de " + user.getName(), true);
    }

    /** Workspaces que o usuário alcança: próprios + memberships ativas, uma query. */
    @Transactional(readOnly = true)
    public List<Workspace> listWorkspacesForUser(Long userId) {
        return workspaceRepository.findAllForUser(userId);
    }

    @Transactional
    public Workspace createWorkspace(Long ownerId, String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        }
        if (workspaceRepository.countByOwnerIdAndDeletedAtIsNull(ownerId) >= MAX_WORKSPACES_PER_OWNER) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Workspace limit reached (" + MAX_WORKSPACES_PER_OWNER + ")");
        }
        boolean firstOwned = workspaceRepository
                .findByOwnerIdAndPrimaryTrueAndDeletedAtIsNull(ownerId).isEmpty()
                && workspaceRepository.countByOwnerIdAndDeletedAtIsNull(ownerId) == 0;
        return createWorkspaceInternal(ownerId, name.trim(), firstOwned);
    }

    private Workspace createWorkspaceInternal(Long ownerId, String name, boolean primary) {
        Instant now = Instant.now();

        Workspace workspace = new Workspace();
        workspace.setOwnerId(ownerId);
        workspace.setName(name);
        workspace.setPrimary(primary);
        workspace.setCreatedAt(now);
        workspace.setUpdatedAt(now);
        workspace = workspaceRepository.save(workspace);

        // O dono também tem linha de membership (simplifica "quem tem acesso");
        // a fonte de verdade de "é dono" segue sendo workspaces.owner_id.
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUserId(ownerId);
        member.setRole(WorkspaceRole.OWNER);
        member.setAcceptedAt(now);
        member.setCreatedAt(now);
        member.setUpdatedAt(now);
        memberRepository.save(member);

        return workspace;
    }

    /**
     * Contexto do usuário no workspace: dono -> OWNER; membership ativa ->
     * papel dela; sem vínculo ou workspace deletado/inexistente -> vazio.
     */
    @Transactional(readOnly = true)
    public Optional<WorkspaceContext> resolveContext(Long workspaceId, Long userId) {
        Optional<Workspace> workspace = workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId);
        if (workspace.isEmpty()) {
            return Optional.empty();
        }
        if (workspace.get().getOwnerId().equals(userId)) {
            return Optional.of(new WorkspaceContext(workspaceId, WorkspaceRole.OWNER, true));
        }
        return memberRepository.findByWorkspaceIdAndUserIdAndDeletedAtIsNull(workspaceId, userId)
                .filter(WorkspaceMember::isActive)
                .map(m -> new WorkspaceContext(workspaceId, m.getRole(), false));
    }

    /** Fallback server-side: workspace primário -> primeira membership ativa. */
    @Transactional(readOnly = true)
    public Optional<WorkspaceContext> resolveFallbackContext(Long userId) {
        Optional<Workspace> primary = workspaceRepository.findByOwnerIdAndPrimaryTrueAndDeletedAtIsNull(userId);
        if (primary.isPresent()) {
            return Optional.of(new WorkspaceContext(primary.get().getId(), WorkspaceRole.OWNER, true));
        }
        return memberRepository.findAllActiveWithWorkspaceForUser(userId).stream()
                .findFirst()
                .map(m -> new WorkspaceContext(m.getWorkspace().getId(), m.getRole(),
                        m.getWorkspace().getOwnerId().equals(userId)));
    }
}
