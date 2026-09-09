package br.pucpr.prissma_server.genai;

import org.springframework.stereotype.Component;

@Component
public class PhotoOnlyPromptStrategy implements EnvironmentPreviewPromptStrategy {

    @Override
    public String generate(EnvironmentPreviewPromptContext context) {
        return EnvironmentPromptBuilder.from(context.request())
                .addPrimaryReference()
                .addNoFloorPlanReference()
                .addDesignChoices()
                .addAdditionalInstructions()
                .addRealismRequirements()
                .build();
    }
}
