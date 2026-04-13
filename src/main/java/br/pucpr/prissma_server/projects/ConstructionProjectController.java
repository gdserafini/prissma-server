package br.pucpr.prissma_server.projects;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/obra")
public class ConstructionProjectController {

    @GetMapping("/{id}/acompanhamento")
    public ResponseEntity<AcompanhamentoResponse> getAcompanhamento(@PathVariable Long id) {

        List<AcompanhamentoTaskResponse> tarefasFundacao = List.of(
                new AcompanhamentoTaskResponse(1L,
                        "Sondagem do terreno",
                        "Realizar sondagem SPT para análise do solo",
                        "HIGH", "DONE",
                        LocalDate.of(2024, 1, 10),
                        LocalDate.of(2024, 1, 15)),
                new AcompanhamentoTaskResponse(2L,
                        "Escavação",
                        "Escavação mecânica para sapatas e blocos",
                        "HIGH", "DONE",
                        LocalDate.of(2024, 1, 16),
                        LocalDate.of(2024, 1, 25)),
                new AcompanhamentoTaskResponse(3L,
                        "Concretagem das sapatas",
                        "Concretar sapatas conforme projeto estrutural",
                        "HIGH", "IN_PROGRESS",
                        LocalDate.of(2024, 1, 26),
                        LocalDate.of(2024, 2, 5))
        );

        List<AcompanhamentoTaskResponse> tarefasEstrutura = List.of(
                new AcompanhamentoTaskResponse(4L,
                        "Montagem das formas",
                        "Montagem das formas de madeira para pilares e vigas",
                        "MEDIUM", "TODO",
                        LocalDate.of(2024, 2, 6),
                        LocalDate.of(2024, 2, 15)),
                new AcompanhamentoTaskResponse(5L,
                        "Armação de aço",
                        "Corte, dobramento e montagem das armaduras",
                        "HIGH", "TODO",
                        LocalDate.of(2024, 2, 10),
                        LocalDate.of(2024, 2, 20)),
                new AcompanhamentoTaskResponse(6L,
                        "Concretagem dos pilares",
                        "Lançamento e adensamento do concreto nos pilares",
                        "HIGH", "TODO",
                        LocalDate.of(2024, 2, 21),
                        LocalDate.of(2024, 3, 5))
        );

        List<AcompanhamentoTaskResponse> tarefasAlvenaria = List.of(
                new AcompanhamentoTaskResponse(7L,
                        "Assentamento de blocos",
                        "Assentamento de blocos cerâmicos nas paredes externas",
                        "MEDIUM", "TODO",
                        LocalDate.of(2024, 3, 6),
                        LocalDate.of(2024, 3, 20)),
                new AcompanhamentoTaskResponse(8L,
                        "Vergas e contravergas",
                        "Instalação de vergas e contravergas em vãos de portas e janelas",
                        "LOW", "TODO",
                        LocalDate.of(2024, 3, 15),
                        LocalDate.of(2024, 3, 25))
        );

        List<AcompanhamentoStageResponse> etapas = List.of(
                new AcompanhamentoStageResponse(1L,
                        "Fundação",
                        "Execução das fundações diretas e indiretas da obra",
                        1, "IN_PROGRESS",
                        LocalDate.of(2024, 1, 10),
                        LocalDate.of(2024, 2, 5),
                        tarefasFundacao),
                new AcompanhamentoStageResponse(2L,
                        "Estrutura",
                        "Execução da estrutura de concreto armado",
                        2, "PLANNED",
                        LocalDate.of(2024, 2, 6),
                        LocalDate.of(2024, 3, 5),
                        tarefasEstrutura),
                new AcompanhamentoStageResponse(3L,
                        "Alvenaria",
                        "Levantamento das paredes e divisórias internas",
                        3, "PLANNED",
                        LocalDate.of(2024, 3, 6),
                        LocalDate.of(2024, 4, 10),
                        tarefasAlvenaria)
        );

        int totalTarefas = tarefasFundacao.size() + tarefasEstrutura.size() + tarefasAlvenaria.size();
        long tarefasConcluidas = etapas.stream()
                .flatMap(e -> e.getTasks().stream())
                .filter(t -> "DONE".equals(t.getStatus()))
                .count();
        long etapasConcluidas = etapas.stream()
                .filter(e -> "DONE".equals(e.getStatus()))
                .count();

        AcompanhamentoResponse response = new AcompanhamentoResponse(
                id,
                "Obra Residencial - Bloco A",
                "IN_PROGRESS",
                etapas.size(),
                (int) etapasConcluidas,
                totalTarefas,
                (int) tarefasConcluidas,
                etapas
        );

        return ResponseEntity.ok(response);
    }
}
