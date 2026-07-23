package com.officedesk.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReopenRequest {

    @NotBlank
    private String reason;
}
