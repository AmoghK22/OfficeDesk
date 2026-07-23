package com.officedesk.controller;

import com.officedesk.dto.admin.*;
import com.officedesk.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAllUsers(PageRequest.of(page, size)));
    }

    @PostMapping("/users")
    public ResponseEntity<AdminUserResponse> createUser(@Valid @RequestBody AdminCreateUserRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createUser(req));
    }

    @PutMapping("/users/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        adminService.deactivateUser(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{id}/activate")
    public ResponseEntity<Void> activateUser(@PathVariable Long id) {
        adminService.activateUser(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/departments")
    public ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody DeptCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createDepartment(req));
    }

    @PutMapping("/sla/{deptId}")
    public ResponseEntity<SlaConfigResponse> updateSla(@PathVariable Long deptId,
                                                       @Valid @RequestBody SlaUpdateRequest req) {
        return ResponseEntity.ok(adminService.updateSlaConfig(deptId, req));
    }

    @PostMapping("/sla/{deptId}")
    public ResponseEntity<SlaConfigResponse> createSla(@PathVariable Long deptId,
                                                       @Valid @RequestBody SlaUpdateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createSlaConfig(deptId, req));
    }

    @PutMapping("/dept/{deptId}/head/{userId}")
    public ResponseEntity<Void> assignDeptHead(@PathVariable Long deptId, @PathVariable Long userId) {
        adminService.assignDeptHead(deptId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sla/{deptId}")
    public ResponseEntity<List<SlaConfigResponse>> getDeptSlaConfigs(@PathVariable Long deptId) {
        return ResponseEntity.ok(adminService.getDeptSlaConfigs(deptId));
    }
}
