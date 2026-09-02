package br.pucpr.prissma_server.diary;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Envelope proprio da pagina em vez de serializar o Page do Spring Data
 * diretamente: o JSON do PageImpl nao tem contrato estavel entre versoes
 * (e o proprio Spring emite warning ao serializa-lo).
 */
public record DiaryEntryPageResponse(
        List<DiaryEntryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static DiaryEntryPageResponse from(Page<DiaryEntry> source) {
        return new DiaryEntryPageResponse(
                source.getContent().stream().map(DiaryEntryResponse::from).toList(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.isFirst(),
                source.isLast()
        );
    }
}
