package com.officedesk.dto.ticket;

import com.officedesk.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketStatusUpdateRequest {

    @NotNull
    private TicketStatus status;

    private String resolutionNote;
}
