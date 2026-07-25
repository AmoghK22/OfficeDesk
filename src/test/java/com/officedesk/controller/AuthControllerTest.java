package com.officedesk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.officedesk.BaseIntegrationTest;
import com.officedesk.dto.auth.LoginRequest;
import com.officedesk.dto.auth.RegisterRequest;
import com.officedesk.entity.Department;
import com.officedesk.entity.User;
import com.officedesk.enums.DepartmentName;
import com.officedesk.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest extends BaseIntegrationTest {

    private Department dept;

    @BeforeEach
    void setUp() {
        dept = deptRepo.findByName(DepartmentName.IT).orElseGet(
                () -> deptRepo.save(Department.builder().name(DepartmentName.IT).build()));
    }

    @Test
    @DisplayName("POST /api/auth/register - success")
    void register_success() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .name("Rahul").email("rahul@test.com").password("pass123").departmentId(dept.getId()).build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.name").value("Rahul"));
    }

    @Test
    @DisplayName("POST /api/auth/register - duplicate email returns 409")
    void register_duplicateEmail() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .name("Rahul").email("rahul@test.com").password("pass123").departmentId(dept.getId()).build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andExpect(status().isCreated());

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
    @DisplayName("POST /api/auth/login - success")
    void login_success() throws Exception {
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    @DisplayName("POST /api/auth/login - wrong password returns 400")
    void login_wrongPassword() throws Exception {
        RegisterRequest regReq = RegisterRequest.builder()
                .name("Rahul").email("rahul@test.com").password("pass123").departmentId(dept.getId()).build();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = LoginRequest.builder().email("rahul@test.com").password("wrongpassword").build();
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(loginReq)))
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
