package br.pucpr.prissma_server.diary;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiaryEntryRepository extends JpaRepository<DiaryEntry, Long> {
    @Query(value = "SELECT d FROM DiaryEntry d "
            + "LEFT JOIN FETCH d.responsibleUser "
            + "LEFT JOIN FETCH d.attachment "
            + "WHERE d.constructionProject.id = :projectId "
            + "ORDER BY d.entryDate DESC, d.id DESC",
            countQuery = "SELECT COUNT(d) FROM DiaryEntry d WHERE d.constructionProject.id = :projectId")
    Page<DiaryEntry> findPageByProject(@Param("projectId") Long projectId, Pageable pageable);
}
