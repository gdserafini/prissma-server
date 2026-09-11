package br.pucpr.prissma_server.attachments;

import br.pucpr.prissma_server.attachments.storage.FileStorageService;
import br.pucpr.prissma_server.attachments.storage.StoredFile;
import br.pucpr.prissma_server.attachments.storage.UploadedFileValidator;
import br.pucpr.prissma_server.projects.ConstructionProject;
import br.pucpr.prissma_server.projects.ConstructionProjectRepository;
import br.pucpr.prissma_server.projects.ProjectPermission;
import br.pucpr.prissma_server.projects.ProjectPermissionService;
import br.pucpr.prissma_server.stage.Stage;
import br.pucpr.prissma_server.task.Task;
import br.pucpr.prissma_server.users.User;
import br.pucpr.prissma_server.users.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;

@Service
public class AttachmentService {

    private final AttachmentRepository repository;
    private final ConstructionProjectRepository projectRepository;
    private final ProjectPermissionService permissionService;
    private final UserRepository userRepository;
    private final FileStorageService storage;
    private final UploadedFileValidator validator;
    private final EntityManager entityManager;

    public AttachmentService(AttachmentRepository repository,
                             ConstructionProjectRepository projectRepository,
                             ProjectPermissionService permissionService,
                             UserRepository userRepository,
                             FileStorageService storage,
                             UploadedFileValidator validator,
                             EntityManager entityManager) {
        this.repository = repository;
        this.projectRepository = projectRepository;
        this.permissionService = permissionService;
        this.userRepository = userRepository;
        this.storage = storage;
        this.validator = validator;
        this.entityManager = entityManager;
    }

    @Transactional
    public Attachment upload(Long projectId,
                             AttachmentTarget target,
                             Long targetId,
                             MultipartFile file,
                             Long userId) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        ConstructionProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        requireUploadPermission(project.getId(), user);

        Stage stage = null;
        Task task = null;
        if (target == AttachmentTarget.STAGE) {
            stage = resolveStage(targetId, project.getId());
        } else if (target == AttachmentTarget.TASK) {
            task = resolveTask(targetId, project.getId());
            stage = task.getStage();
        }

        String contentType = validator.normalizeContentType(file.getContentType());
        validator.validateContentType(contentType);
        validator.validateSize(file.getSize());

        String originalName = validator.sanitizeFileName(file.getOriginalFilename());
        String extension = validator.resolveExtension(contentType);

        StoredFile stored;
        try (InputStream in = file.getInputStream()) {
            validator.verifyMagicBytes(in, contentType);
            try (InputStream payload = file.getInputStream()) {
                stored = storage.store(payload, extension, "projects/" + project.getId());
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Não foi possível salvar o arquivo. Tente novamente em alguns instantes.");
        }

        try {
            Attachment attachment = new Attachment();
            attachment.setConstructionProject(project);
            attachment.setStage(stage);
            attachment.setTask(task);
            attachment.setUploadedByUser(user);
            attachment.setFileName(originalName);
            attachment.setFileType(contentType);
            attachment.setFileUrl(stored.storageKey());
            attachment.setUploadedAt(Instant.now());
            return repository.save(attachment);
        } catch (RuntimeException e) {
            safeDelete(stored.storageKey());
            throw e;
        }
    }

    public List<Attachment> list(Long projectId, Long stageId, Long taskId, Long userId) {
        ConstructionProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        requireProjectAccess(project.getId(), user);

        if (taskId != null) {
            return repository.findByConstructionProjectIdAndTaskIdOrderByUploadedAtDesc(project.getId(), taskId);
        }
        if (stageId != null) {
            return repository.findByConstructionProjectIdAndStageIdOrderByUploadedAtDesc(project.getId(), stageId);
        }
        return repository.findByConstructionProjectIdOrderByUploadedAtDesc(project.getId());
    }

    public DownloadPayload download(Long projectId, Long attachmentId, Long userId) {
        Attachment attachment = loadAttachmentScopedToProject(projectId, attachmentId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        requireProjectAccess(projectId, user);

        Resource resource = storage.load(attachment.getFileUrl());
        return new DownloadPayload(resource, attachment.getFileName(), attachment.getFileType());
    }

    @Transactional
    public void delete(Long projectId, Long attachmentId, Long userId) {
        Attachment attachment = loadAttachmentScopedToProject(projectId, attachmentId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        requireUploadPermission(projectId, user);

        String storageKey = attachment.getFileUrl();
        repository.delete(attachment);
        safeDelete(storageKey);
    }

    private Attachment loadAttachmentScopedToProject(Long projectId, Long attachmentId) {
        Attachment attachment = repository.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));
        if (attachment.getConstructionProject() == null
                || !projectId.equals(attachment.getConstructionProject().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found");
        }
        return attachment;
    }

    /** Leitura/download de anexo: basta VIEW_PROJECT (ou ADMIN global). */
    private void requireProjectAccess(Long projectId, User user) {
        permissionService.requirePermission(projectId, user.getId(), ProjectPermission.VIEW_PROJECT);
    }

    /**
     * Upload e exclusao de anexo: exige MANAGE_ATTACHMENTS.
     *
     * A lista fixa anterior era UPLOAD_ROLES = {OWNER, ENG, ARQ, MASTER}, mas
     * construction_project_members.role_in_project so aceita OWNER, ARCHITECT,
     * ENGINEER, FOREMAN e USER: "ENG", "ARQ" e "MASTER" sao valores do enum de
     * papel GLOBAL (users.role) e nunca casavam. Na pratica so o OWNER
     * conseguia anexar arquivo; engenheiro, arquiteto e mestre de obra levavam
     * 403 sem motivo aparente.
     */
    private void requireUploadPermission(Long projectId, User user) {
        permissionService.requirePermission(projectId, user.getId(), ProjectPermission.MANAGE_ATTACHMENTS);
    }

    private Stage resolveStage(Long stageId, Long projectId) {
        if (stageId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "stageId is required for STAGE target");
        }
        Stage stage = entityManager.find(Stage.class, stageId);
        if (stage == null || stage.getConstructionProject() == null
                || !projectId.equals(stage.getConstructionProject().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stage not found in this project");
        }
        return stage;
    }

    private Task resolveTask(Long taskId, Long projectId) {
        if (taskId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "taskId is required for TASK target");
        }
        Task task = entityManager.find(Task.class, taskId);
        if (task == null || task.getStage() == null
                || task.getStage().getConstructionProject() == null
                || !projectId.equals(task.getStage().getConstructionProject().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found in this project");
        }
        return task;
    }

    private void safeDelete(String storageKey) {
        try {
            storage.delete(storageKey);
        } catch (RuntimeException ignored) {
        }
    }

    public record DownloadPayload(Resource resource, String fileName, String contentType) {
    }
}
