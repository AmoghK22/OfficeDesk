package com.officedesk.dto.ticket;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    private Long id;
    private String postedByName;
    private String comment;
    @JsonProperty("isInternal")
    private boolean isInternal;
    private LocalDateTime createdAt;
}
