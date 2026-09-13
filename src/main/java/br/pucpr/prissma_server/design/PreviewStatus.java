package br.pucpr.prissma_server.design;

/** Ciclo de vida do job de geração de prévia. É o que a tela consulta em polling. */
public enum PreviewStatus {
    PROCESSING,
    READY,
    FAILED
}
