package br.pucpr.prissma_server.design;

import br.pucpr.prissma_server.genai.EnvironmentPreviewRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/projects/{projectId}/proposals")
public class DesignProposalController {

    private final DesignProposalService service;
    private final EnvironmentPreviewJobService previewJobService;

    public DesignProposalController(DesignProposalService service,
                                    EnvironmentPreviewJobService previewJobService) {
        this.service = service;
        this.previewJobService = previewJobService;
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

    /**
     * Criação em multipart porque o arquivo da v1 é opcional: uma proposta pode
     * nascer só com título e ambiente e receber a imagem pela prévia da IA.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProposalResponse> create(@PathVariable Long projectId,
                                                   @Valid @RequestPart("proposal") ProposalRequest request,
                                                   @RequestPart(value = "file", required = false) MultipartFile file,
                                                   Authentication auth) {
        ProposalResponse response = service.create(projectId, request, file, resolveUserId(auth));
        URI location = URI.create("/projects/" + projectId + "/proposals/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<ProposalPageResponse> list(@PathVariable Long projectId,
                                                     @RequestParam(required = false) Integer page,
                                                     @RequestParam(required = false) Integer size,
                                                     Authentication auth) {
        return ResponseEntity.ok(service.list(projectId, page, size, resolveUserId(auth)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProposalResponse> get(@PathVariable Long projectId,
                                                @PathVariable Long id,
                                                Authentication auth) {
        return ResponseEntity.ok(service.get(projectId, id, resolveUserId(auth)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProposalResponse> update(@PathVariable Long projectId,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody ProposalRequest request,
                                                   Authentication auth) {
        return ResponseEntity.ok(service.update(projectId, id, request, resolveUserId(auth)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long projectId,
                                       @PathVariable Long id,
                                       Authentication auth) {
        service.delete(projectId, id, resolveUserId(auth));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProposalVersionResponse> addVersion(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            Authentication auth) {
        ProposalVersionResponse response =
                service.addVersion(projectId, id, description, file, resolveUserId(auth));
        URI location = URI.create(
                "/projects/" + projectId + "/proposals/" + id + "/versions/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/{id}/versions/{versionId}/status")
    public ResponseEntity<ProposalVersionResponse> changeStatus(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @PathVariable Long versionId,
            @Valid @RequestBody ProposalStatusRequest request,
            Authentication auth) {
        return ResponseEntity.ok(
                service.changeStatus(projectId, id, versionId, request, resolveUserId(auth)));
    }

    @GetMapping("/{id}/versions/{versionId}/image")
    public ResponseEntity<Resource> versionImage(@PathVariable Long projectId,
                                                 @PathVariable Long id,
                                                 @PathVariable Long versionId,
                                                 Authentication auth) {
        DesignProposalService.VersionImage image =
                service.loadVersionImage(projectId, id, versionId, resolveUserId(auth));

        MediaType mediaType = image.contentType() != null
                ? MediaType.parseMediaType(image.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;

        // inline: a tela mostra a imagem no card, não oferece download.
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(image.fileName() != null ? image.fileName() : "previa", StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(image.resource());
    }

    /**
     * Dispara a prévia por IA. Responde 202: a geração leva de 30 a 60s e
     * continua depois que esta requisição termina.
     */
    @PostMapping(value = "/{id}/previews", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PreviewResponse> requestPreview(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestPart("rawImage") MultipartFile rawImage,
            @RequestPart(value = "floorPlan", required = false) MultipartFile floorPlan,
            @Valid @RequestPart("options") EnvironmentPreviewRequest options,
            Authentication auth) {
        PreviewResponse response = previewJobService.request(
                projectId, id, rawImage, floorPlan, options, resolveUserId(auth));
        URI location = URI.create(
                "/projects/" + projectId + "/proposals/" + id + "/previews/" + response.id());
        return ResponseEntity.accepted().location(location).body(response);
    }

    @GetMapping("/{id}/previews/{previewId}")
    public ResponseEntity<PreviewResponse> getPreview(@PathVariable Long projectId,
                                                      @PathVariable Long id,
                                                      @PathVariable Long previewId,
                                                      Authentication auth) {
        return ResponseEntity.ok(
                previewJobService.get(projectId, id, previewId, resolveUserId(auth)));
    }
}
