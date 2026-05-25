package br.pucpr.prissma_server.attachments;

import br.pucpr.prissma_server.attachments.storage.FileStorageService;
import br.pucpr.prissma_server.attachments.storage.StorageProperties;
import br.pucpr.prissma_server.attachments.storage.StoredFile;
import br.pucpr.prissma_server.projects.ConstructionProject;
import br.pucpr.prissma_server.projects.ConstructionProjectMember;
import br.pucpr.prissma_server.projects.ConstructionProjectMemberRepository;
import br.pucpr.prissma_server.projects.ConstructionProjectRepository;
import br.pucpr.prissma_server.stage.Stage;
import br.pucpr.prissma_server.task.Task;
import br.pucpr.prissma_server.users.Role;
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
import java.util.Set;

@Service
public class AttachmentService {

    private static final Set<String> UPLOAD_ROLES = Set.of("OWNER", "ENG", "ARQ", "MASTER");

    private static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] GIF87_MAGIC = {0x47, 0x49, 0x46, 0x38, 0x37, 0x61};
    private static final byte[] GIF89_MAGIC = {0x47, 0x49, 0x46, 0x38, 0x39, 0x61};
    private static final byte[] BMP_MAGIC = {0x42, 0x4D};
    private static final byte[] TIFF_LE_MAGIC = {0x49, 0x49, 0x2A, 0x00};
    private static final byte[] TIFF_BE_MAGIC = {0x4D, 0x4D, 0x00, 0x2A};
    private static final byte[] WEBP_RIFF_MAGIC = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_FORMAT_TAG = {0x57, 0x45, 0x42, 0x50};
    private static final byte[] ZIP_MAGIC = {0x50, 0x4B, 0x03, 0x04};

    private final AttachmentRepository repository;
    private final ConstructionProjectRepository projectRepository;
    private final ConstructionProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final FileStorageService storage;
    private final StorageProperties storageProperties;
    private final EntityManager entityManager;

    public AttachmentService(AttachmentRepository repository,
                             ConstructionProjectRepository projectRepository,
                             ConstructionProjectMemberRepository memberRepository,
                             UserRepository userRepository,
                             FileStorageService storage,
                             StorageProperties storageProperties,
                             EntityManager entityManager) {
        this.repository = repository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.storage = storage;
        this.storageProperties = storageProperties;
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

        String contentType = normalizeContentType(file.getContentType());
        validateContentType(contentType);
        validateSize(file.getSize());

        String originalName = sanitizeFileName(file.getOriginalFilename());
        String extension = resolveExtension(contentType);

        StoredFile stored;
        try (InputStream in = file.getInputStream()) {
            verifyMagicBytes(in, contentType);
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

    private void requireProjectAccess(Long projectId, User user) {
        if (user.getRole() == Role.ADMIN) return;
        memberRepository.findByConstructionProjectIdAndUserId(projectId, user.getId())
                .filter(m -> "ACTIVE".equals(m.getMembershipStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "User is not a member of this project"));
    }

    private void requireUploadPermission(Long projectId, User user) {
        if (user.getRole() == Role.ADMIN) return;
        ConstructionProjectMember member = memberRepository
                .findByConstructionProjectIdAndUserId(projectId, user.getId())
                .filter(m -> "ACTIVE".equals(m.getMembershipStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "User is not a member of this project"));
        if (!UPLOAD_ROLES.contains(member.getRoleInProject())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "User role does not allow uploading attachments");
        }
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

    private void validateContentType(String contentType) {
        if (contentType == null
                || !storageProperties.getLimits().getAllowedContentTypes().contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Unsupported file type. Allowed: "
                            + storageProperties.getLimits().getAllowedContentTypes());
        }
    }

    private void validateSize(long size) {
        long max = storageProperties.getLimits().getMaxFileSizeBytes();
        if (size > max) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Arquivo excede o tamanho máximo permitido de " + max + " bytes");
        }
    }

    private void verifyMagicBytes(InputStream in, String contentType) throws IOException {
        int needed = switch (contentType) {
            case "image/webp" -> 12;
            case "image/png" -> 8;
            case "image/gif" -> 6;
            case "application/pdf", "image/tiff", DOCX_CONTENT_TYPE -> 4;
            case "image/jpeg" -> 3;
            case "image/bmp" -> 2;
            default -> 0;
        };
        if (needed == 0) return;

        byte[] header = in.readNBytes(needed);
        if (header.length < needed || !matchesSignature(header, contentType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "File content does not match declared type");
        }
    }

    private boolean matchesSignature(byte[] header, String contentType) {
        return switch (contentType) {
            case "application/pdf" -> startsWith(header, PDF_MAGIC);
            case "image/png" -> startsWith(header, PNG_MAGIC);
            case "image/jpeg" -> startsWith(header, JPEG_MAGIC);
            case "image/gif" -> startsWith(header, GIF87_MAGIC) || startsWith(header, GIF89_MAGIC);
            case "image/bmp" -> startsWith(header, BMP_MAGIC);
            case "image/tiff" -> startsWith(header, TIFF_LE_MAGIC) || startsWith(header, TIFF_BE_MAGIC);
            case "image/webp" -> startsWith(header, WEBP_RIFF_MAGIC) && regionEquals(header, 8, WEBP_FORMAT_TAG);
            case DOCX_CONTENT_TYPE -> startsWith(header, ZIP_MAGIC);
            default -> true;
        };
    }

    private boolean startsWith(byte[] header, byte[] expected) {
        return regionEquals(header, 0, expected);
    }

    private boolean regionEquals(byte[] header, int offset, byte[] expected) {
        if (header.length < offset + expected.length) return false;
        for (int i = 0; i < expected.length; i++) {
            if (header[offset + i] != expected[i]) return false;
        }
        return true;
    }

    private String normalizeContentType(String raw) {
        if (raw == null) return null;
        int semi = raw.indexOf(';');
        return (semi >= 0 ? raw.substring(0, semi) : raw).trim().toLowerCase();
    }

    private String resolveExtension(String contentType) {
        return switch (contentType) {
            case "application/pdf" -> "pdf";
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/gif" -> "gif";
            case "image/bmp" -> "bmp";
            case "image/tiff" -> "tiff";
            case "image/webp" -> "webp";
            case DOCX_CONTENT_TYPE -> "docx";
            default -> "bin";
        };
    }

    private String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) return "file";
        String base = name.replace("\\", "/");
        int slash = base.lastIndexOf('/');
        if (slash >= 0) base = base.substring(slash + 1);
        base = base.replaceAll("[\\r\\n\\t]", "_");
        if (base.length() > 255) base = base.substring(0, 255);
        return base;
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
