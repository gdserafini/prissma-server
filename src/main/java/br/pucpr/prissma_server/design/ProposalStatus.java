package br.pucpr.prissma_server.design;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Status de uma versão de proposta.
 *
 * São os quatro badges da tela, nesta ordem: Rascunho, Em análise, Aprovada e
 * Ajustes pedidos. Os nomes vêm da V4 e ficaram — trocá-los só renomearia o
 * mesmo conceito.
 */
public enum ProposalStatus {
    DRAFT,
    PENDING_REVIEW,
    APPROVED,
    REJECTED;

    public static ProposalStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O status é obrigatório");
        }
        try {
            return ProposalStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O status deve ser DRAFT, PENDING_REVIEW, APPROVED ou REJECTED");
        }
    }
}
