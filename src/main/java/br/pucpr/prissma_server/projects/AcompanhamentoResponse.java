package br.pucpr.prissma_server.projects;

import java.util.List;
import java.util.Map;

public class AcompanhamentoResponse {

    private Long obraId;
    private String titulo;
    private String status;
    private int totalEtapas;
    private int etapasConcluidas;
    private int totalTarefas;
    private int tarefasConcluidas;
    private Map<String, Long> stageStatusCounts;
    private Map<String, Long> taskStatusCounts;
    private List<AcompanhamentoStageResponse> etapas;

    public AcompanhamentoResponse() {
    }

    public AcompanhamentoResponse(Long obraId, String titulo, String status,
                                  int totalEtapas, int etapasConcluidas,
                                  int totalTarefas, int tarefasConcluidas,
                                  Map<String, Long> stageStatusCounts,
                                  Map<String, Long> taskStatusCounts,
                                  List<AcompanhamentoStageResponse> etapas) {
        this.obraId = obraId;
        this.titulo = titulo;
        this.status = status;
        this.totalEtapas = totalEtapas;
        this.etapasConcluidas = etapasConcluidas;
        this.totalTarefas = totalTarefas;
        this.tarefasConcluidas = tarefasConcluidas;
        this.stageStatusCounts = stageStatusCounts;
        this.taskStatusCounts = taskStatusCounts;
        this.etapas = etapas;
    }

    public Long getObraId() { return obraId; }
    public String getTitulo() { return titulo; }
    public String getStatus() { return status; }
    public int getTotalEtapas() { return totalEtapas; }
    public int getEtapasConcluidas() { return etapasConcluidas; }
    public int getTotalTarefas() { return totalTarefas; }
    public int getTarefasConcluidas() { return tarefasConcluidas; }
    public Map<String, Long> getStageStatusCounts() { return stageStatusCounts; }
    public Map<String, Long> getTaskStatusCounts() { return taskStatusCounts; }
    public List<AcompanhamentoStageResponse> getEtapas() { return etapas; }
}
