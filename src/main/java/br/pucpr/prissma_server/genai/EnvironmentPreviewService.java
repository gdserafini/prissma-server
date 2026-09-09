package br.pucpr.prissma_server.genai;

import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Set;

@Service
public class EnvironmentPreviewService {

    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "image/png", "image/jpeg", "image/webp"
    );

    private final EnvironmentPreviewPromptStrategyFactory strategyFactory;
    private final OpenAIImageGateway imageGateway;

    public EnvironmentPreviewService(EnvironmentPreviewPromptStrategyFactory strategyFactory,
                                     OpenAIImageGateway imageGateway) {
        this.strategyFactory = strategyFactory;
        this.imageGateway = imageGateway;
    }

    public byte[] generate(MultipartFile rawImage,
                           MultipartFile floorPlan,
                           EnvironmentPreviewRequest request) {
        validateImage(rawImage, "rawImage", true);
        validateImage(floorPlan, "floorPlan", false);

        boolean hasFloorPlan = floorPlan != null && !floorPlan.isEmpty();
        EnvironmentPreviewPromptStrategy strategy = strategyFactory.forFloorPlan(hasFloorPlan);
        String prompt = strategy.generate(new EnvironmentPreviewPromptContext(request, hasFloorPlan));

        try {
            return imageGateway.edit(rawImage, floorPlan, prompt, request.generationMode());
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Não foi possível ler a imagem enviada", ex);
        }
    }

    private void validateImage(MultipartFile file, String field, boolean required) {
        if (file == null || file.isEmpty()) {
            if (required) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O arquivo '" + field + "' é obrigatório");
            }
            return;
        }

        String contentType = file.getContentType();
        if (contentType == null || !SUPPORTED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "O arquivo '" + field + "' deve ser PNG, JPEG ou WebP");
        }
    }
}
