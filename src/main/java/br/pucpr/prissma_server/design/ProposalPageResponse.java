package br.pucpr.prissma_server.design;

import java.util.List;

public record ProposalPageResponse(
        List<ProposalResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
