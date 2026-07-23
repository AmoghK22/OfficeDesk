package com.officedesk.dto.ticket;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingRequest {

    @NotNull @Min(1) @Max(5)
    private Integer rating;

    private String feedback;
}
