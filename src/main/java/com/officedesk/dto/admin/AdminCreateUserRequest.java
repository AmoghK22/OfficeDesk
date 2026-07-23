package com.officedesk.dto.admin;

import com.officedesk.enums.Role;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreateUserRequest {

    @NotBlank
    private String name;

    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 6)
    private String password;

    @NotNull
    private Role role;

    @NotNull(message = "Department is required for Agent and Department Head roles")
    private Long departmentId;
}
