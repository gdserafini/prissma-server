package br.pucpr.prissma_server.genai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvironmentPromptBuilderTest {

    @Test
    void includesFloorPlanRulesAndUserChoices() {
        EnvironmentPreviewRequest request = new EnvironmentPreviewRequest(
                EnvironmentPreviewRequest.EnvironmentType.LIVING_ROOM,
                EnvironmentPreviewRequest.DesignStyle.MODERN,
                List.of(EnvironmentPreviewRequest.ColorPalette.OFF_WHITE,
                        EnvironmentPreviewRequest.ColorPalette.NATURAL_WOOD),
                EnvironmentPreviewRequest.Lighting.WARM_INDIRECT,
                EnvironmentPreviewRequest.Flooring.LIGHT_PORCELAIN,
                EnvironmentPreviewRequest.GenerationMode.PREVIEW,
                "Adicionar painel ripado"
        );

        String prompt = EnvironmentPromptBuilder.from(request)
                .addPrimaryReference()
                .addFloorPlanReference()
                .addDesignChoices()
                .addAdditionalInstructions()
                .addRealismRequirements()
                .build();

        assertTrue(prompt.contains("BLUE"));
        assertTrue(prompt.contains("living room"));
        assertTrue(prompt.contains("Adicionar painel ripado"));
        assertTrue(prompt.contains("must not appear"));
    }
}
