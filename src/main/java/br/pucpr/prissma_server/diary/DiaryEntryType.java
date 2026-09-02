package br.pucpr.prissma_server.diary;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Tipos de registro do diario da obra (RF17). */
public enum DiaryEntryType {

    /** Ocorrencia: qualquer fato relevante do dia. */
    OCCURRENCE,

    /** Entrega: chegada de material, equipamento ou servico concluido. */
    DELIVERY,

    /** Efetivo: registro da mao de obra presente na obra. */
    WORKFORCE,

    /** Impedimento: algo que travou o andamento (chuva, falta de material...). */
    IMPEDIMENT;

    public static DiaryEntryType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Entry type is required");
        }
        try {
            return DiaryEntryType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Entry type must be one of OCCURRENCE, DELIVERY, WORKFORCE or IMPEDIMENT");
        }
    }
}
