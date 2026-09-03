package br.pucpr.prissma_server.diary;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum DiaryEntryType {
    OCCURRENCE,
    DELIVERY,
    WORKFORCE,
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
