package br.pucpr.prissma_server.genai;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record EnvironmentPreviewRequest(
        @NotNull EnvironmentType environment,
        @NotNull DesignStyle style,
        @NotEmpty @Size(max = 5) List<ColorPalette> colors,
        @NotNull Lighting lighting,
        @NotNull Flooring flooring,
        @NotNull GenerationMode generationMode,
        @Size(max = 1_000) String additionalInstructions
) {

    public enum EnvironmentType {
        LIVING_ROOM, BEDROOM, KITCHEN, BATHROOM, DINING_ROOM, OFFICE, BALCONY, GARAGE, OTHER
    }

    public enum DesignStyle {
        MODERN, MINIMALIST, CONTEMPORARY, INDUSTRIAL, CLASSIC, RUSTIC
    }

    public enum ColorPalette {
        WHITE, OFF_WHITE, BEIGE, LIGHT_GRAY, DARK_GRAY, BLACK,
        NATURAL_WOOD, DARK_WOOD, GREEN, BLUE
    }

    public enum Lighting {
        NATURAL, WARM, COOL, NEUTRAL, WARM_INDIRECT, COOL_INDIRECT
    }

    public enum Flooring {
        LIGHT_PORCELAIN, DARK_PORCELAIN, WOOD, LAMINATE,
        MARBLE, POLISHED_CONCRETE, CERAMIC
    }

    public enum GenerationMode {
        PREVIEW, FINAL
    }
}
