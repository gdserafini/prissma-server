package br.pucpr.prissma_server.schedule;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ScheduleAllocationResponse(
        Long constructionProjectId,
        Long userId,
        String userName,
        LocalDate date,
        BigDecimal allocatedHours
) {
    public static ScheduleAllocationResponse from(ScheduleAllocation allocation) {
        return new ScheduleAllocationResponse(
                allocation.getConstructionProject().getId(),
                allocation.getUser().getId(),
                allocation.getUser().getName(),
                allocation.getAllocationDate(),
                allocation.getHours()
        );
    }
}
