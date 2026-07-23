package com.officedesk.dto.ticket;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityResponse {

    private Long id;
    private String action;
    private String description;
    private String performedByName;
    private LocalDateTime createdAt;
}
