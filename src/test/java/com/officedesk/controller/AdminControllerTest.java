package com.officedesk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.officedesk.BaseIntegrationTest;
import com.officedesk.dto.admin.AdminCreateUserRequest;
import com.officedesk.dto.admin.DeptCreateRequest;
import com.officedesk.dto.admin.SlaUpdateRequest;
import com.officedesk.entity.Department;
import com.officedesk.entity.SlaConfig;
import com.officedesk.entity.User;
import com.officedesk.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminControllerTest extends BaseIntegrationTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private User admin;
    private String adminToken;
    private User employee;
    private String empToken;
    private Department dept;

    @BeforeEach
    void setUp() {
        dept = deptRepo.findByName(DepartmentName.IT).orElseGet(
                () -> deptRepo.save(Department.builder().name(DepartmentName.IT).build()));

        for (Priority p : Priority.values()) {
            if (slaConfigRepo.findByDepartmentIdAndPriority(dept.getId(), p).isEmpty()) {
                slaConfigRepo.save(SlaConfig.builder()
                        .department(dept).priority(p)
                        .resolutionHours(p == Priority.CRITICAL ? 4 : p == Priority.HIGH ? 24 : p == Priority.MEDIUM ? 48 : 72)
                        .build());
            }
        }

        admin = userRepo.save(User.builder()
                .name("Admin").email("admin@test.com").password("encoded")
                .role(Role.SUPER_ADMIN).isActive(true).build());
        adminToken = tokenFor(admin);

        employee = userRepo.save(User.builder()
                .name("Rahul").email("rahul@test.com").password("encoded")
                .role(Role.EMPLOYEE).department(dept).isActive(true).build());
        empToken = tokenFor(employee);
    }

    @Test
    @DisplayName("GET /api/admin/users - admin gets all users")
    void getAllUsers_success() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("GET /api/admin/users - employee forbidden")
    void getAllUsers_forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + empToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/admin/users - create user")
    void createUser_success() throws Exception {
        AdminCreateUserRequest req = AdminCreateUserRequest.builder()
                .name("Agent1").email("agent1@test.com").password("pass123")
                .role(Role.AGENT).departmentId(dept.getId()).build();

        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Agent1"))
                .andExpect(jsonPath("$.role").value("AGENT"));
    }

    @Test
    @DisplayName("POST /api/admin/users - duplicate email returns 409")
    void createUser_duplicateEmail() throws Exception {
        AdminCreateUserRequest req = AdminCreateUserRequest.builder()
                .name("Agent1").email("admin@test.com").password("pass123")
                .role(Role.AGENT).departmentId(dept.getId()).build();

        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT /api/admin/users/{id}/deactivate - deactivates user")
    void deactivateUser() throws Exception {
        mockMvc.perform(put("/api/admin/users/" + employee.getId() + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/admin/users/{id}/activate - activates user")
    void activateUser() throws Exception {
        mockMvc.perform(put("/api/admin/users/" + employee.getId() + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/admin/departments - create department with default SLA configs")
    void createDepartment() throws Exception {
        DeptCreateRequest req = DeptCreateRequest.builder().name(DepartmentName.FINANCE).build();

        mockMvc.perform(post("/api/admin/departments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("FINANCE"));
    }

    @Test
    @DisplayName("GET /api/admin/sla/{deptId} - get SLA configs for department")
    void getSlaConfigs() throws Exception {
        mockMvc.perform(get("/api/admin/sla/" + dept.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    @DisplayName("PUT /api/admin/sla/{deptId} - update SLA config")
    void updateSlaConfig() throws Exception {
        SlaUpdateRequest req = SlaUpdateRequest.builder().priority(Priority.HIGH).resolutionHours(12).build();

        mockMvc.perform(put("/api/admin/sla/" + dept.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolutionHours").value(12))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    @DisplayName("PUT /api/admin/sla/{deptId} - invalid hours rejected")
    void updateSlaConfig_invalidHours() throws Exception {
        SlaUpdateRequest req = SlaUpdateRequest.builder().priority(Priority.HIGH).resolutionHours(0).build();

        mockMvc.perform(put("/api/admin/sla/" + dept.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Any admin endpoint - employee gets 403")
    void nonAdmin_forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + empToken))
                .andExpect(status().isForbidden());

        AdminCreateUserRequest validReq = AdminCreateUserRequest.builder()
                .name("Agent1").email("agent1@test.com").password("pass123")
                .role(Role.AGENT).departmentId(dept.getId()).build();
        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + empToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(validReq)))
                .andExpect(status().isForbidden());

        SlaUpdateRequest slaReq = SlaUpdateRequest.builder().priority(Priority.HIGH).resolutionHours(12).build();
        mockMvc.perform(put("/api/admin/sla/" + dept.getId())
                        .header("Authorization", "Bearer " + empToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(slaReq)))
                .andExpect(status().isForbidden());
    }
}
