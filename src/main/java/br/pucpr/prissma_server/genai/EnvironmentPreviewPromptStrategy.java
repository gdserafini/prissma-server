package br.pucpr.prissma_server.genai;

public interface EnvironmentPreviewPromptStrategy {

    String generate(EnvironmentPreviewPromptContext context);
}
