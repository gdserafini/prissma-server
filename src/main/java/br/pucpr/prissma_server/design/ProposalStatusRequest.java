package br.pucpr.prissma_server.design;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Aprovar, pedir ajustes ou mandar para análise uma versão da proposta. */
public class ProposalStatusRequest {

    @NotBlank(message = "O status é obrigatório")
    private String status;

    @Size(max = 2000, message = "O comentário deve ter no máximo 2000 caracteres")
    private String comment;

    public ProposalStatusRequest() {
    }

    public ProposalStatusRequest(String status, String comment) {
        this.status = status;
        this.comment = comment;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
