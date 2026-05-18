package br.pucpr.prissma_server.attachments;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByConstructionProjectIdOrderByUploadedAtDesc(Long constructionProjectId);

    List<Attachment> findByConstructionProjectIdAndStageIdOrderByUploadedAtDesc(Long constructionProjectId, Long stageId);

    List<Attachment> findByConstructionProjectIdAndTaskIdOrderByUploadedAtDesc(Long constructionProjectId, Long taskId);
}
