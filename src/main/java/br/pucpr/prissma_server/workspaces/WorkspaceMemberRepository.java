package br.pucpr.prissma_server.workspaces;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

    /**
     * Memberships ativas com o workspace já carregado (fetch join): usado pelo
     * WorkspaceContextFilter, que roda fora de transação — LAZY estouraria.
     */
    @Query("""
            SELECT m FROM WorkspaceMember m JOIN FETCH m.workspace w
             WHERE m.userId = :userId
               AND m.active = TRUE
               AND m.deletedAt IS NULL
               AND w.deletedAt IS NULL
             ORDER BY m.id
            """)
    List<WorkspaceMember> findAllActiveWithWorkspaceForUser(@Param("userId") Long userId);

    Optional<WorkspaceMember> findByWorkspaceIdAndUserIdAndDeletedAtIsNull(Long workspaceId, Long userId);

    /** Memberships vivas do usuário, na ordem de criação (fallback de contexto usa a primeira). */
    List<WorkspaceMember> findAllByUserIdAndActiveTrueAndDeletedAtIsNullOrderByIdAsc(Long userId);

    List<WorkspaceMember> findAllByWorkspaceIdAndDeletedAtIsNullOrderByIdAsc(Long workspaceId);
}
