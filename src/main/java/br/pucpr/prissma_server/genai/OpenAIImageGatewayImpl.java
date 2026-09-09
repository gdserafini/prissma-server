package br.pucpr.prissma_server.genai;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Component
public class OpenAIImageGatewayImpl implements OpenAIImageGateway {

    private final RestClient restClient;
    private final OpenAIImageProperties properties;

    public OpenAIImageGatewayImpl(RestClient openAIImageRestClient,
                                  OpenAIImageProperties properties) {
        this.restClient = openAIImageRestClient;
        this.properties = properties;
    }

    @Override
    public byte[] edit(MultipartFile rawImage,
                       MultipartFile floorPlan,
                       String prompt,
                       EnvironmentPreviewRequest.GenerationMode generationMode) throws IOException {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new ResponseStatusException(PAYLOAD_TOO_LARGE,
                    "A integração de imagens está sem OPENAI_API_KEY configurada");
        }

        List<Map<String, String>> images = new ArrayList<>();
        images.add(Map.of("image_url", asDataUrl(rawImage)));
        if (floorPlan != null && !floorPlan.isEmpty()) {
            images.add(Map.of("image_url", asDataUrl(floorPlan)));
        }

        Map<String, Object> payload = Map.of(
                "model", properties.getModel(),
                "prompt", prompt,
                "images", images,
                "quality", generationMode == EnvironmentPreviewRequest.GenerationMode.FINAL ? "high" : "low",
                "size", "1536x1024",
                "output_format", "png",
                "n", 1
        );

        try {
            ImageEditResponse response = restClient.post()
                    .uri("/images/edits")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(ImageEditResponse.class);

            if (response == null || response.data() == null || response.data().isEmpty()
                    || response.data().getFirst().b64Json() == null) {
                throw new ResponseStatusException(BAD_GATEWAY,
                        "A API de imagens não retornou uma imagem válida");
            }
            return Base64.getDecoder().decode(response.data().getFirst().b64Json());
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(BAD_GATEWAY,
                    "A API de imagens recusou a solicitação (HTTP " + ex.getStatusCode().value() + ")", ex);
        }
    }

    private String asDataUrl(MultipartFile file) throws IOException {
        if (file.getSize() > properties.getMaxInputBytes()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE,
                    "Cada imagem deve ter no máximo " + properties.getMaxInputBytes() / (1024 * 1024) + " MB");
        }
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(file.getBytes());
    }

    private record ImageEditResponse(List<ImageData> data) { }

    private record ImageData(@JsonProperty("b64_json") String b64Json) { }
}
