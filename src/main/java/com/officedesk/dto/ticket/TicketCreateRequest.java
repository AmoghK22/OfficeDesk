package com.officedesk.dto.ticket;

import com.officedesk.enums.Priority;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketCreateRequest {

    @NotBlank @Size(max = 200)
    private String title;

    @NotBlank
    private String description;

    @NotNull
    private Priority priority;

    @NotBlank
    private String category;
}
