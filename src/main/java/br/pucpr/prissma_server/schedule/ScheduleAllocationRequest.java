package br.pucpr.prissma_server.schedule;

import java.math.BigDecimal;

public class ScheduleAllocationRequest {

    private BigDecimal allocatedHours;

    public ScheduleAllocationRequest() {}

    public ScheduleAllocationRequest(BigDecimal allocatedHours) {
        this.allocatedHours = allocatedHours;
    }

    public BigDecimal getAllocatedHours() { return allocatedHours; }
    public void setAllocatedHours(BigDecimal allocatedHours) { this.allocatedHours = allocatedHours; }
}
