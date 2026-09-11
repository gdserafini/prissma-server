package br.pucpr.prissma_server.design;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DesignApprovalRepository extends JpaRepository<DesignApproval, Long> {

    @Query("SELECT a FROM DesignApproval a LEFT JOIN FETCH a.approverUser "
            + "WHERE a.designSubmission.id = :submissionId ORDER BY a.id DESC")
    List<DesignApproval> findAllBySubmission(@Param("submissionId") Long submissionId);
}
