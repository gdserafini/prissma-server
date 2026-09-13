package br.pucpr.prissma_server.genai;

public interface OpenAIImageGateway {

    byte[] edit(ImageBytes rawImage,
                ImageBytes floorPlan,
                String prompt,
                EnvironmentPreviewRequest.GenerationMode generationMode);
}
