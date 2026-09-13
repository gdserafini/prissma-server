package br.pucpr.prissma_server.design;

/**
 * Disparado depois que a prévia foi gravada em {@code PROCESSING}.
 *
 * Carrega só o id: quem processa recarrega tudo do banco, porque roda em outra
 * thread e depois do commit — entidades da transação anterior não valem mais lá.
 */
public record EnvironmentPreviewRequestedEvent(Long previewId) {
}
