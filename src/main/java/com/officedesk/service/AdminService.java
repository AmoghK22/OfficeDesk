package com.officedesk.service;

import com.officedesk.dto.admin.*;
import com.officedesk.entity.Department;
import com.officedesk.entity.SlaConfig;
import com.officedesk.entity.User;
import com.officedesk.enums.Priority;
import com.officedesk.enums.Role;
import com.officedesk.exception.DuplicateEmailException;
import com.officedesk.exception.ResourceNotFoundException;
import com.officedesk.repository.DepartmentRepository;
import com.officedesk.repository.SlaConfigRepository;
import com.officedesk.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepo;
    private final DepartmentRepository deptRepo;
    private final SlaConfigRepository slaConfigRepo;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UserRepository userRepo, DepartmentRepository deptRepo,
                        SlaConfigRepository slaConfigRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.deptRepo = deptRepo;
        this.slaConfigRepo = slaConfigRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<AdminUserResponse> getAllUsers(Pageable pageable) {
        return userRepo.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional
    public AdminUserResponse createUser(AdminCreateUserRequest req) {
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new DuplicateEmailException("Email already registered: " + req.getEmail());
        }

        User.UserBuilder builder = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole())
                .isActive(true);

        if (req.getDepartmentId() != null) {
            Department dept = deptRepo.findById(req.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
            builder.department(dept);
        }

        User user = userRepo.save(builder.build());
        return mapToResponse(user);
    }

    @Transactional
    public void deactivateUser(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setActive(false);
        userRepo.save(user);
    }

    @Transactional
    public void activateUser(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setActive(true);
        userRepo.save(user);
    }

    @Transactional
    public DepartmentResponse createDepartment(DeptCreateRequest req) {
        Department dept = Department.builder()
                .name(req.getName())
                .build();

        if (req.getHeadUserId() != null) {
            User head = userRepo.findById(req.getHeadUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            dept.setHead(head);
        }

        dept = deptRepo.save(dept);

        // Create default SLA configs for all priorities
        for (Priority p : Priority.values()) {
            slaConfigRepo.save(SlaConfig.builder()
                    .department(dept)
                    .priority(p)
                    .resolutionHours(p == Priority.CRITICAL ? 4 : p == Priority.HIGH ? 24 : p == Priority.MEDIUM ? 48 : 72)
                    .build());
        }

        return DepartmentResponse.builder()
                .id(dept.getId())
                .name(dept.getName().name())
                .headName(dept.getHead() != null ? dept.getHead().getName() : null)
                .build();
    }

    @Transactional
    public SlaConfigResponse updateSlaConfig(Long deptId, SlaUpdateRequest req) {
        Department dept = deptRepo.findById(deptId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        SlaConfig config = slaConfigRepo.findByDepartmentIdAndPriority(deptId, req.getPriority())
                .orElseThrow(() -> new ResourceNotFoundException("SLA config not found"));

        config.setResolutionHours(req.getResolutionHours());
        config = slaConfigRepo.save(config);

        return SlaConfigResponse.builder()
                .id(config.getId())
                .departmentName(dept.getName().name())
                .priority(config.getPriority().name())
                .resolutionHours(config.getResolutionHours())
                .build();
    }

    public SlaConfigResponse createSlaConfig(Long deptId, SlaUpdateRequest req) {
        Department dept = deptRepo.findById(deptId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        SlaConfig config = SlaConfig.builder()
                .department(dept)
                .priority(req.getPriority())
                .resolutionHours(req.getResolutionHours())
                .build();

        config = slaConfigRepo.save(config);

        return SlaConfigResponse.builder()
                .id(config.getId())
                .departmentName(dept.getName().name())
                .priority(config.getPriority().name())
                .resolutionHours(config.getResolutionHours())
                .build();
    }

    @Transactional
    public void assignDeptHead(Long deptId, Long userId) {
        Department dept = deptRepo.findById(deptId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        User head = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        dept.setHead(head);
        deptRepo.save(dept);
    }

    private AdminUserResponse mapToResponse(User u) {
        return AdminUserResponse.builder()
                .id(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .role(u.getRole().name())
                .departmentName(u.getDepartment() != null ? u.getDepartment().getName().name() : null)
                .isActive(u.isActive())
                .createdAt(u.getCreatedAt())
                .build();
    }

    public List<DepartmentResponse> getAllDepartments() {
        return deptRepo.findAll().stream().map(d -> DepartmentResponse.builder()
                .id(d.getId())
                .name(d.getName().name())
                .headName(d.getHead() != null ? d.getHead().getName() : null)
                .build()).toList();
    }

    public List<SlaConfigResponse> getDeptSlaConfigs(Long deptId) {
        Department dept = deptRepo.findById(deptId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        return slaConfigRepo.findByDepartmentId(deptId).stream()
                .map(c -> SlaConfigResponse.builder()
                        .id(c.getId())
                        .departmentName(dept.getName().name())
                        .priority(c.getPriority().name())
                        .resolutionHours(c.getResolutionHours())
                        .build())
                .toList();
    }
}
