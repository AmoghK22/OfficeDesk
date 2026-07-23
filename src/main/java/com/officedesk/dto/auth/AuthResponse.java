package com.officedesk.dto.auth;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String tokenType;
    private String role;
    private Long userId;
    private String name;
    private Long departmentId;
}
