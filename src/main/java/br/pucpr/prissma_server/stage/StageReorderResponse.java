package br.pucpr.prissma_server.stage;

import java.util.List;

public record StageReorderResponse(String message, List<StageResponse> stages) {
}
