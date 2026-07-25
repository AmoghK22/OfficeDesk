package com.officedesk.dto.ticket;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardStats {
    private long total;
    private long open;
    private long inProgress;
    private long resolved;
    private long closed;
    private long breached;
    private double avgRating;
}
