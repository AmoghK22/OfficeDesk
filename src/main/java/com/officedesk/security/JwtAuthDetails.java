package com.officedesk.security;

import lombok.Getter;

@Getter
public class JwtAuthDetails {

    private final Long userId;
    private final String email;
    private final String role;

    public JwtAuthDetails(Long userId, String email, String role) {
        this.userId = userId;
        this.email = email;
        this.role = role;
    }
}
