package br.pucpr.prissma_server.attachments;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/attachments")
public class AttachmentController {

    private final AttachmentService service;

    public AttachmentController(AttachmentService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> upload(@PathVariable Long projectId,
                                                     @RequestParam("file") MultipartFile file,
                                                     @RequestParam(value = "target", defaultValue = "PROJECT") AttachmentTarget target,
                                                     @RequestParam(value = "stageId", required = false) Long stageId,
                                                     @RequestParam(value = "taskId", required = false) Long taskId,
                                                     Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Long targetId = switch (target) {
            case STAGE -> stageId;
            case TASK -> taskId;
            case PROJECT -> null;
        };
        Attachment saved = service.upload(projectId, target, targetId, file, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(AttachmentResponse.from(saved));
    }

    @GetMapping
    public ResponseEntity<List<AttachmentResponse>> list(@PathVariable Long projectId,
                                                         @RequestParam(value = "stageId", required = false) Long stageId,
                                                         @RequestParam(value = "taskId", required = false) Long taskId,
                                                         Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        List<AttachmentResponse> list = service.list(projectId, stageId, taskId, userId).stream()
                .map(AttachmentResponse::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long projectId,
                                             @PathVariable Long id,
                                             Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        AttachmentService.DownloadPayload payload = service.download(projectId, id, userId);

        MediaType mediaType = payload.contentType() != null
                ? MediaType.parseMediaType(payload.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(payload.fileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(payload.resource());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long projectId,
                                       @PathVariable Long id,
                                       Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        service.delete(projectId, id, userId);
        return ResponseEntity.noContent().build();
    }
}
