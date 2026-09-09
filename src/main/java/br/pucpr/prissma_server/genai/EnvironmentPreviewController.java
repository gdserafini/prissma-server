package br.pucpr.prissma_server.genai;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/environment-previews")
public class EnvironmentPreviewController {

    private final EnvironmentPreviewService service;

    public EnvironmentPreviewController(EnvironmentPreviewService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generate(
            @RequestPart("rawImage") MultipartFile rawImage,
            @RequestPart(value = "floorPlan", required = false) MultipartFile floorPlan,
            @Valid @RequestPart("options") EnvironmentPreviewRequest options) {
        byte[] image = service.generate(rawImage, floorPlan, options);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }
}
