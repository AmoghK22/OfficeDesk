package com.officedesk.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {

    private Long id;
    private String name;
    private String email;
    private String role;
    private String departmentName;
    @JsonProperty("isActive")
    private boolean isActive;
    private LocalDateTime createdAt;
}
