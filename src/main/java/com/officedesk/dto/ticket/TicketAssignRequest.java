package com.officedesk.dto.ticket;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketAssignRequest {

    @NotNull
    private Long agentId;
}
