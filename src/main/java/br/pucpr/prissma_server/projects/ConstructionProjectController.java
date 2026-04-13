package br.pucpr.prissma_server.projects;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/obra")
public class ConstructionProjectController {

    private static final String[] TITULOS_OBRA = {
            "Residencial Vista Verde", "Edifício Comercial Centro", "Condomínio Parque das Flores",
            "Galpão Industrial Norte", "Centro Médico São Lucas", "Torre Empresarial Alfa",
            "Conjunto Habitacional Bela Vista", "Escola Municipal João Paulo II",
            "Hotel Boutique Araucária", "Shopping Center Meridional"
    };

    private static final String[][] ETAPAS_CATALOG = {
            {"Fundação",    "Execução das fundações diretas e indiretas da obra"},
            {"Estrutura",   "Execução da estrutura de concreto armado"},
            {"Alvenaria",   "Levantamento das paredes e divisórias internas"},
            {"Cobertura",   "Instalação da estrutura metálica e telhamento"},
            {"Instalações Elétricas",  "Distribuição elétrica, quadros e iluminação"},
            {"Instalações Hidráulicas","Redes de água fria, quente e esgoto"},
            {"Revestimentos","Aplicação de reboco, cerâmica e pintura"},
            {"Acabamentos", "Esquadrias, louças, metais e paisagismo"}
    };

    private static final String[][][] TAREFAS_POR_ETAPA = {
            // Fundação
            {{"Sondagem do terreno",       "Realizar sondagem SPT para análise do solo",             "HIGH"},
             {"Escavação",                 "Escavação mecânica para sapatas e blocos",                "HIGH"},
             {"Concretagem das sapatas",   "Concretar sapatas conforme projeto estrutural",           "HIGH"},
             {"Impermeabilização",         "Impermeabilização das fundações com manta asfáltica",     "MEDIUM"}},
            // Estrutura
            {{"Montagem das formas",       "Montagem das formas de madeira para pilares e vigas",     "MEDIUM"},
             {"Armação de aço",            "Corte, dobramento e montagem das armaduras",              "HIGH"},
             {"Concretagem dos pilares",   "Lançamento e adensamento do concreto nos pilares",        "HIGH"},
             {"Desforma",                  "Retirada das formas após cura do concreto",               "LOW"}},
            // Alvenaria
            {{"Assentamento de blocos",    "Assentamento de blocos cerâmicos nas paredes externas",   "MEDIUM"},
             {"Vergas e contravergas",     "Instalação de vergas em vãos de portas e janelas",        "LOW"},
             {"Chapisco e emboço",         "Aplicação de chapisco e emboço nas paredes",              "MEDIUM"},
             {"Divisórias internas",       "Execução das divisórias internas conforme projeto",       "MEDIUM"}},
            // Cobertura
            {{"Estrutura metálica",        "Montagem da estrutura metálica da cobertura",             "HIGH"},
             {"Telhamento",                "Instalação das telhas cerâmicas ou metálicas",            "HIGH"},
             {"Calhas e rufos",            "Instalação de calhas, rufos e cumeeiras",                 "MEDIUM"},
             {"Impermeabilização laje",    "Impermeabilização da laje de cobertura",                  "MEDIUM"}},
            // Instalações Elétricas
            {{"Passagem de eletrodutos",   "Embutimento de eletrodutos nas paredes e laje",           "MEDIUM"},
             {"Fiação",                    "Passagem dos cabos elétricos conforme projeto",           "HIGH"},
             {"Quadros elétricos",         "Montagem e ligação dos quadros de distribuição",          "HIGH"},
             {"Teste e energização",       "Testes de continuidade e energização do circuito",        "HIGH"}},
            // Instalações Hidráulicas
            {{"Ramais de água fria",       "Instalação das tubulações de água fria",                  "HIGH"},
             {"Ramais de esgoto",          "Instalação das tubulações de esgoto e ventilação",        "HIGH"},
             {"Caixas e reservatórios",    "Instalação de caixas sifonadas e reservatório d'água",    "MEDIUM"},
             {"Testes hidrostáticos",      "Realização de testes de pressão nas tubulações",          "HIGH"}},
            // Revestimentos
            {{"Reboco",                    "Aplicação de reboco desempenado nas paredes",             "MEDIUM"},
             {"Cerâmica e porcelanato",    "Assentamento de cerâmica em pisos e paredes molhadas",    "MEDIUM"},
             {"Massa corrida",             "Aplicação de massa corrida para regularização",           "LOW"},
             {"Pintura",                   "Pintura interna e externa com tinta acrílica",            "LOW"}},
            // Acabamentos
            {{"Esquadrias",                "Instalação de portas, janelas e portões",                 "MEDIUM"},
             {"Louças e metais",           "Instalação de peças sanitárias e torneiras",              "LOW"},
             {"Limpeza final de obra",     "Limpeza e remoção de entulho residual",                   "LOW"},
             {"Paisagismo",                "Execução do paisagismo e áreas externas",                 "LOW"}}
    };

    private static final String[] STATUS_OBRA = {"PLANNING", "IN_PROGRESS", "IN_PROGRESS", "IN_PROGRESS", "COMPLETED"};

    @GetMapping("/{id}/acompanhamento")
    public ResponseEntity<AcompanhamentoResponse> getAcompanhamento(@PathVariable Long id) {
        Random rng = new Random(id);

        String titulo = TITULOS_OBRA[rng.nextInt(TITULOS_OBRA.length)];
        String statusObra = STATUS_OBRA[rng.nextInt(STATUS_OBRA.length)];

        int numEtapas = 3 + rng.nextInt(3); // 3 a 5 etapas
        int[] etapaIndexes = escolherIndicesUnicos(rng, ETAPAS_CATALOG.length, numEtapas);

        LocalDate dataBase = LocalDate.of(2024, 1, 1).plusDays(rng.nextInt(60));
        List<AcompanhamentoStageResponse> etapas = new ArrayList<>();
        long taskIdCounter = 1L;

        for (int ordem = 0; ordem < numEtapas; ordem++) {
            int ei = etapaIndexes[ordem];
            String nomeEtapa = ETAPAS_CATALOG[ei][0];
            String descEtapa = ETAPAS_CATALOG[ei][1];

            LocalDate inicioEtapa = dataBase;
            int duracaoEtapa = 20 + rng.nextInt(30);
            LocalDate fimEtapa = inicioEtapa.plusDays(duracaoEtapa);

            String statusEtapa = resolverStatusEtapa(ordem, numEtapas, statusObra, rng);

            int numTarefas = 2 + rng.nextInt(3); // 2 a 4 tarefas
            String[] tarefasDisponiveis[] = TAREFAS_POR_ETAPA[ei];
            int[] tarefaIndexes = escolherIndicesUnicos(rng, tarefasDisponiveis.length, Math.min(numTarefas, tarefasDisponiveis.length));

            List<AcompanhamentoTaskResponse> tarefas = new ArrayList<>();
            LocalDate inicioTarefa = inicioEtapa;

            for (int ti = 0; ti < tarefaIndexes.length; ti++) {
                String[] t = tarefasDisponiveis[tarefaIndexes[ti]];
                int duracaoTarefa = 5 + rng.nextInt(10);
                LocalDate fimTarefa = inicioTarefa.plusDays(duracaoTarefa);

                String statusTarefa = resolverStatusTarefa(ti, tarefaIndexes.length, statusEtapa, rng);
                String prioridade = t[2];

                tarefas.add(new AcompanhamentoTaskResponse(
                        taskIdCounter++, t[0], t[1], prioridade, statusTarefa,
                        inicioTarefa, fimTarefa));

                inicioTarefa = fimTarefa.plusDays(1);
            }

            etapas.add(new AcompanhamentoStageResponse(
                    (long) (ordem + 1), nomeEtapa, descEtapa, ordem + 1, statusEtapa,
                    inicioEtapa, fimEtapa, tarefas));

            dataBase = fimEtapa.plusDays(1 + rng.nextInt(5));
        }

        int totalTarefas = etapas.stream().mapToInt(e -> e.getTasks().size()).sum();
        long tarefasConcluidas = etapas.stream()
                .flatMap(e -> e.getTasks().stream())
                .filter(t -> "DONE".equals(t.getStatus()))
                .count();
        long etapasConcluidas = etapas.stream()
                .filter(e -> "DONE".equals(e.getStatus()))
                .count();

        AcompanhamentoResponse response = new AcompanhamentoResponse(
                id, titulo, statusObra,
                etapas.size(), (int) etapasConcluidas,
                totalTarefas, (int) tarefasConcluidas,
                etapas);

        return ResponseEntity.ok(response);
    }

    private String resolverStatusEtapa(int ordem, int total, String statusObra, Random rng) {
        if ("COMPLETED".equals(statusObra)) return "DONE";
        if ("PLANNING".equals(statusObra)) return "PLANNED";
        // IN_PROGRESS: etapas anteriores concluídas, a atual em andamento, as próximas planejadas
        int etapaAtiva = 1 + rng.nextInt(Math.max(1, total - 1));
        if (ordem < etapaAtiva) return "DONE";
        if (ordem == etapaAtiva) return "IN_PROGRESS";
        return "PLANNED";
    }

    private String resolverStatusTarefa(int index, int total, String statusEtapa, Random rng) {
        if ("DONE".equals(statusEtapa)) return "DONE";
        if ("PLANNED".equals(statusEtapa)) return "TODO";
        // IN_PROGRESS: algumas concluídas, uma em andamento, restantes pendentes
        int tarefaAtiva = rng.nextInt(total);
        if (index < tarefaAtiva) return "DONE";
        if (index == tarefaAtiva) return "IN_PROGRESS";
        return "TODO";
    }

    private int[] escolherIndicesUnicos(Random rng, int universo, int quantidade) {
        List<Integer> pool = new ArrayList<>();
        for (int i = 0; i < universo; i++) pool.add(i);
        int[] resultado = new int[quantidade];
        for (int i = 0; i < quantidade; i++) {
            int pos = rng.nextInt(pool.size());
            resultado[i] = pool.remove(pos);
        }
        return resultado;
    }
}
