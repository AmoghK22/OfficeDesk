package com.officedesk.dto.auth;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank @Email
    private String email;

    @NotBlank
    private String password;
}
