package br.pucpr.prissma_server.diary;

import org.springframework.data.domain.Page;
import java.util.List;

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
