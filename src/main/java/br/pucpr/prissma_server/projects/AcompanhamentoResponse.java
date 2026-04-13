package br.pucpr.prissma_server.projects;

import java.util.List;

public class AcompanhamentoResponse {

    private Long obraId;
    private String titulo;
    private String status;
    private int totalEtapas;
    private int etapasConcluidas;
    private int totalTarefas;
    private int tarefasConcluidas;
    private List<AcompanhamentoStageResponse> etapas;

    public AcompanhamentoResponse() {
    }

    public AcompanhamentoResponse(Long obraId, String titulo, String status,
                                  int totalEtapas, int etapasConcluidas,
                                  int totalTarefas, int tarefasConcluidas,
                                  List<AcompanhamentoStageResponse> etapas) {
        this.obraId = obraId;
        this.titulo = titulo;
        this.status = status;
        this.totalEtapas = totalEtapas;
        this.etapasConcluidas = etapasConcluidas;
        this.totalTarefas = totalTarefas;
        this.tarefasConcluidas = tarefasConcluidas;
        this.etapas = etapas;
    }

    public Long getObraId() { return obraId; }
    public String getTitulo() { return titulo; }
    public String getStatus() { return status; }
    public int getTotalEtapas() { return totalEtapas; }
    public int getEtapasConcluidas() { return etapasConcluidas; }
    public int getTotalTarefas() { return totalTarefas; }
    public int getTarefasConcluidas() { return tarefasConcluidas; }
    public List<AcompanhamentoStageResponse> getEtapas() { return etapas; }
}
