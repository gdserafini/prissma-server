package br.pucpr.prissma_server.genai;

public record EnvironmentPreviewPromptContext(
        EnvironmentPreviewRequest request,
        boolean hasFloorPlan
) {
}
