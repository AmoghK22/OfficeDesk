package com.officedesk.dto.ticket;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {

    private Long id;
    private String ticketNo;
    private String title;
    private String description;
    private String status;
    private String priority;
    private String category;
    private String departmentName;
    private Long departmentId;
    private String raisedByName;
    private Long raisedById;
    private String assignedToName;
    private Long assignedToId;
    private LocalDateTime slaDeadline;
    private boolean slaBreached;
    private boolean escalated;
    private String resolutionNote;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
    private Long slaHours;
}
