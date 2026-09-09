package br.pucpr.prissma_server.genai;

import org.springframework.stereotype.Component;

@Component
public class EnvironmentPreviewPromptStrategyFactory {

    private final PhotoOnlyPromptStrategy photoOnly;
    private final FloorPlanPromptStrategy floorPlan;

    public EnvironmentPreviewPromptStrategyFactory(PhotoOnlyPromptStrategy photoOnly,
                                                   FloorPlanPromptStrategy floorPlan) {
        this.photoOnly = photoOnly;
        this.floorPlan = floorPlan;
    }

    public EnvironmentPreviewPromptStrategy forFloorPlan(boolean hasFloorPlan) {
        return hasFloorPlan ? floorPlan : photoOnly;
    }
}
