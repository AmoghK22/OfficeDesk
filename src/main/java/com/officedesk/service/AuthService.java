package com.officedesk.service;

import com.officedesk.dto.auth.AuthResponse;
import com.officedesk.dto.auth.LoginRequest;
import com.officedesk.dto.auth.RegisterRequest;
import com.officedesk.entity.Department;
import com.officedesk.entity.User;
import com.officedesk.enums.Role;
import com.officedesk.exception.DuplicateEmailException;
import com.officedesk.repository.DepartmentRepository;
import com.officedesk.repository.UserRepository;
import com.officedesk.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final DepartmentRepository deptRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepo, DepartmentRepository deptRepo,
                       PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.deptRepo = deptRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new DuplicateEmailException("Email already registered: " + req.getEmail());
        }

        Department dept = deptRepo.findById(req.getDepartmentId())
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));

        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(Role.EMPLOYEE)
                .department(dept)
                .isActive(true)
                .build();

        user = userRepo.save(user);
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .role(user.getRole().name())
                .userId(user.getId())
                .name(user.getName())
                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                .build();
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!user.isActive()) {
            throw new IllegalArgumentException("Account has been deactivated. Contact administrator.");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .role(user.getRole().name())
                .userId(user.getId())
                .name(user.getName())
                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                .build();
    }
}
