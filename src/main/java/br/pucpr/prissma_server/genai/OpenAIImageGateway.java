package br.pucpr.prissma_server.genai;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface OpenAIImageGateway {

    byte[] edit(MultipartFile rawImage,
                MultipartFile floorPlan,
                String prompt,
                EnvironmentPreviewRequest.GenerationMode generationMode) throws IOException;
}
