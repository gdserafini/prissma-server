package br.pucpr.prissma_server.workspaces;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    /** Próprios + memberships ativas, sem duplicata, na ordem de criação. */
    @Query("""
            SELECT w FROM Workspace w
             WHERE w.deletedAt IS NULL
               AND (w.ownerId = :userId
                    OR EXISTS (SELECT 1 FROM WorkspaceMember m
                                WHERE m.workspace = w
                                  AND m.userId = :userId
                                  AND m.active = TRUE
                                  AND m.deletedAt IS NULL))
             ORDER BY w.id
            """)
    List<Workspace> findAllForUser(@Param("userId") Long userId);

    Optional<Workspace> findByIdAndDeletedAtIsNull(Long id);

    Optional<Workspace> findByOwnerIdAndPrimaryTrueAndDeletedAtIsNull(Long ownerId);

    long countByOwnerIdAndDeletedAtIsNull(Long ownerId);
}
