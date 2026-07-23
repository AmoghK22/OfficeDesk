package com.officedesk.dto.admin;

import com.officedesk.enums.Priority;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaUpdateRequest {

    @NotNull
    private Priority priority;

    @NotNull @Min(1)
    private Integer resolutionHours;
}
