package br.pucpr.prissma_server.genai;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Builder GoF responsável por montar o prompt em seções previsíveis. */
public final class EnvironmentPromptBuilder {

    private static final Map<EnvironmentPreviewRequest.EnvironmentType, String> ENVIRONMENTS = Map.of(
            EnvironmentPreviewRequest.EnvironmentType.LIVING_ROOM, "living room",
            EnvironmentPreviewRequest.EnvironmentType.BEDROOM, "bedroom",
            EnvironmentPreviewRequest.EnvironmentType.KITCHEN, "kitchen",
            EnvironmentPreviewRequest.EnvironmentType.BATHROOM, "bathroom",
            EnvironmentPreviewRequest.EnvironmentType.DINING_ROOM, "dining room",
            EnvironmentPreviewRequest.EnvironmentType.OFFICE, "home office",
            EnvironmentPreviewRequest.EnvironmentType.BALCONY, "balcony",
            EnvironmentPreviewRequest.EnvironmentType.GARAGE, "garage",
            EnvironmentPreviewRequest.EnvironmentType.OTHER, "residential interior environment"
    );

    private static final Map<EnvironmentPreviewRequest.DesignStyle, String> STYLES = Map.of(
            EnvironmentPreviewRequest.DesignStyle.MODERN, "modern design with clean lines and contemporary materials",
            EnvironmentPreviewRequest.DesignStyle.MINIMALIST, "minimalist design with simple forms and little decoration",
            EnvironmentPreviewRequest.DesignStyle.CONTEMPORARY, "contemporary design using current architectural elements",
            EnvironmentPreviewRequest.DesignStyle.INDUSTRIAL, "industrial design with metal, concrete and functional elements",
            EnvironmentPreviewRequest.DesignStyle.CLASSIC, "classic design with elegant materials and balanced proportions",
            EnvironmentPreviewRequest.DesignStyle.RUSTIC, "rustic design emphasizing natural materials and warm textures"
    );

    private static final Map<EnvironmentPreviewRequest.Lighting, String> LIGHTING = Map.of(
            EnvironmentPreviewRequest.Lighting.NATURAL, "predominantly natural daylight",
            EnvironmentPreviewRequest.Lighting.WARM, "warm artificial lighting",
            EnvironmentPreviewRequest.Lighting.COOL, "cool artificial lighting",
            EnvironmentPreviewRequest.Lighting.NEUTRAL, "neutral balanced artificial lighting",
            EnvironmentPreviewRequest.Lighting.WARM_INDIRECT, "warm indirect architectural lighting with soft illumination",
            EnvironmentPreviewRequest.Lighting.COOL_INDIRECT, "cool indirect architectural lighting with soft illumination"
    );

    private static final Map<EnvironmentPreviewRequest.Flooring, String> FLOORING = Map.of(
            EnvironmentPreviewRequest.Flooring.LIGHT_PORCELAIN, "light-colored porcelain tile flooring",
            EnvironmentPreviewRequest.Flooring.DARK_PORCELAIN, "dark-colored porcelain tile flooring",
            EnvironmentPreviewRequest.Flooring.WOOD, "natural wood flooring",
            EnvironmentPreviewRequest.Flooring.LAMINATE, "wood-look laminate flooring",
            EnvironmentPreviewRequest.Flooring.MARBLE, "natural marble flooring",
            EnvironmentPreviewRequest.Flooring.POLISHED_CONCRETE, "polished concrete flooring",
            EnvironmentPreviewRequest.Flooring.CERAMIC, "ceramic tile flooring"
    );

    private static final Map<EnvironmentPreviewRequest.ColorPalette, String> COLORS = Map.of(
            EnvironmentPreviewRequest.ColorPalette.WHITE, "white",
            EnvironmentPreviewRequest.ColorPalette.OFF_WHITE, "warm off-white",
            EnvironmentPreviewRequest.ColorPalette.BEIGE, "neutral beige",
            EnvironmentPreviewRequest.ColorPalette.LIGHT_GRAY, "light gray",
            EnvironmentPreviewRequest.ColorPalette.DARK_GRAY, "dark gray",
            EnvironmentPreviewRequest.ColorPalette.BLACK, "black accents",
            EnvironmentPreviewRequest.ColorPalette.NATURAL_WOOD, "natural wood tones",
            EnvironmentPreviewRequest.ColorPalette.DARK_WOOD, "dark wood tones",
            EnvironmentPreviewRequest.ColorPalette.GREEN, "muted green accents",
            EnvironmentPreviewRequest.ColorPalette.BLUE, "muted blue accents"
    );

    private final EnvironmentPreviewRequest request;
    private final StringBuilder prompt = new StringBuilder();

    private EnvironmentPromptBuilder(EnvironmentPreviewRequest request) {
        this.request = Objects.requireNonNull(request);
    }

    public static EnvironmentPromptBuilder from(EnvironmentPreviewRequest request) {
        return new EnvironmentPromptBuilder(request);
    }

    public EnvironmentPromptBuilder addPrimaryReference() {
        prompt.append("""
                Create a photorealistic architectural visualization of the same real
                residential environment shown in Image 1.

                Image 1 is the primary visual and structural reference. Preserve room
                geometry, wall positions, ceiling proportions, doors, windows, openings,
                visible structural elements, camera position, angle, perspective and scale.
                Do not redesign, enlarge, shrink or structurally modify the room.

                """);
        return this;
    }

    public EnvironmentPromptBuilder addNoFloorPlanReference() {
        prompt.append("""
                No floor plan was provided. Base spatial organization on the geometry
                visible in Image 1.

                """);
        return this;
    }

    public EnvironmentPromptBuilder addFloorPlanReference() {
        prompt.append("""
                Image 2 is the floor plan associated with Image 1. A translucent BLUE
                highlight identifies the area to visualize. Use the highlighted area as
                a secondary spatial reference and use its furniture for approximate
                organization. Ignore rooms outside the highlighted area.

                The blue highlight is an annotation and must not appear in the result.
                Preserve Image 1 as the primary source of real geometry and perspective.
                If the images conflict, prioritize the geometry in Image 1.

                """);
        return this;
    }

    public EnvironmentPromptBuilder addDesignChoices() {
        String colors = request.colors().stream()
                .map(COLORS::get)
                .collect(Collectors.joining(", "));
        prompt.append("""
                Target environment: %s.
                Design style: %s.
                Main color palette: %s.
                Lighting: %s.
                Flooring: %s.

                Apply these choices naturally. Do not force every color onto every surface.
                Lighting must remain physically plausible and compatible with the openings.

                """.formatted(
                ENVIRONMENTS.get(request.environment()),
                STYLES.get(request.style()),
                colors,
                LIGHTING.get(request.lighting()),
                FLOORING.get(request.flooring())));
        return this;
    }

    public EnvironmentPromptBuilder addAdditionalInstructions() {
        String instructions = request.additionalInstructions();
        prompt.append("Additional user instructions: ")
                .append(instructions == null || instructions.isBlank()
                        ? "Use professional architectural judgment consistent with the selected style."
                        : instructions.trim())
                .append("\n\n");
        return this;
    }

    public EnvironmentPromptBuilder addRealismRequirements() {
        prompt.append("""
                Maintain realistic furniture dimensions, circulation space, material
                textures, shadows, reflections, lighting, perspective, scale and
                construction feasibility. Do not invent rooms, move doors or windows,
                create impossible structures or change the camera viewpoint.

                Produce a professional conceptual architectural preview. It is not a
                technical construction drawing.
                """);
        return this;
    }

    public String build() {
        return prompt.toString().trim();
    }
}
