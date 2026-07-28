package com.officedesk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.officedesk.BaseIntegrationTest;
import com.officedesk.dto.auth.LoginRequest;
import com.officedesk.dto.auth.RegisterRequest;
import com.officedesk.dto.auth.VerifyEmailRequest;
import com.officedesk.entity.Department;
import com.officedesk.enums.DepartmentName;
import com.officedesk.enums.Role;
import com.officedesk.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest extends BaseIntegrationTest {

    @Autowired private UserRepository userRepository;

    private Department dept;

    @BeforeEach
    void setUp() {
        dept = deptRepo.findByName(DepartmentName.IT).orElseGet(
                () -> deptRepo.save(Department.builder().name(DepartmentName.IT).build()));
    }

    @Test
    @DisplayName("POST /api/auth/register - success returns message and email")
    void register_success() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .name("Rahul").email("rahul@test.com").password("pass123").departmentId(dept.getId()).build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.email").value("rahul@test.com"));
    }

    @Test
    @DisplayName("POST /api/auth/register - duplicate email from verified user returns 409")
    void register_duplicateEmail() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .name("Rahul").email("rahul@test.com").password("pass123").departmentId(dept.getId()).build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andExpect(status().isCreated());

        var user = userRepository.findByEmail("rahul@test.com").orElseThrow();
        user.setVerified(true);
        userRepository.save(user);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/auth/register - validation error for short password")
    void register_validationError() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .name("Rahul").email("rahul@test.com").password("123").departmentId(dept.getId()).build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login - requires email verification first")
    void login_requiresVerification() throws Exception {
        RegisterRequest regReq = RegisterRequest.builder()
                .name("Rahul").email("rahul@test.com").password("pass123").departmentId(dept.getId()).build();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = LoginRequest.builder().email("rahul@test.com").password("pass123").build();
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(loginReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/auth/login - verified user can login")
    void login_verifiedUser() throws Exception {
        RegisterRequest regReq = RegisterRequest.builder()
                .name("Rahul").email("rahul@test.com").password("pass123").departmentId(dept.getId()).build();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        var user = userRepository.findByEmail("rahul@test.com").orElseThrow();
        user.setVerified(true);
        userRepository.save(user);

        LoginRequest loginReq = LoginRequest.builder().email("rahul@test.com").password("pass123").build();
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    @DisplayName("POST /api/auth/verify-email - success")
    void verifyEmail_success() throws Exception {
        RegisterRequest regReq = RegisterRequest.builder()
                .name("Rahul").email("rahul@test.com").password("pass123").departmentId(dept.getId()).build();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        var user = userRepository.findByEmail("rahul@test.com").orElseThrow();
        VerifyEmailRequest verifyReq = VerifyEmailRequest.builder()
                .email("rahul@test.com").code(user.getVerificationCode()).build();

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully. You can now log in."));
    }

    @Test
    @DisplayName("POST /api/auth/verify-email - wrong code returns 400")
    void verifyEmail_wrongCode() throws Exception {
        RegisterRequest regReq = RegisterRequest.builder()
                .name("Rahul").email("rahul@test.com").password("pass123").departmentId(dept.getId()).build();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        VerifyEmailRequest verifyReq = VerifyEmailRequest.builder()
                .email("rahul@test.com").code("999999").build();

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(verifyReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/auth/departments - returns department list")
    void getDepartments() throws Exception {
        mockMvc.perform(get("/api/auth/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("IT"));
    }

    @Test
    @DisplayName("POST /api/auth/register - without department returns 400")
    void register_withoutDept() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .name("Rahul").email("rahul@test.com").password("pass123").departmentId(null).build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
