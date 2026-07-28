package com.officedesk.service;

import com.officedesk.dto.auth.AuthResponse;
import com.officedesk.dto.auth.ForgotPasswordRequest;
import com.officedesk.dto.auth.LoginRequest;
import com.officedesk.dto.auth.RegisterRequest;
import com.officedesk.dto.auth.ResetPasswordRequest;
import com.officedesk.dto.auth.VerifyEmailRequest;
import com.officedesk.entity.Department;
import com.officedesk.entity.User;
import com.officedesk.enums.Role;
import com.officedesk.exception.DuplicateEmailException;
import com.officedesk.exception.EmailNotVerifiedException;
import com.officedesk.exception.ResourceNotFoundException;
import com.officedesk.exception.UnauthorizedException;
import com.officedesk.repository.DepartmentRepository;
import com.officedesk.repository.UserRepository;
import com.officedesk.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepo;
    private final DepartmentRepository deptRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    public AuthService(UserRepository userRepo, DepartmentRepository deptRepo,
                       PasswordEncoder passwordEncoder, JwtUtil jwtUtil, EmailService emailService) {
        this.userRepo = userRepo;
        this.deptRepo = deptRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
    }

    public java.util.Map<String, Object> register(RegisterRequest req) {
        java.util.Optional<User> existing = userRepo.findByEmail(req.getEmail());
        if (existing.isPresent()) {
            User old = existing.get();
            if (old.isVerified()) {
                throw new DuplicateEmailException("Email already registered: " + req.getEmail());
            }
            // Delete old unverified user so they can re-register
            userRepo.delete(old);
        }

        Department dept = deptRepo.findById(req.getDepartmentId())
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));

        String code = generateVerificationCode();

        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(Role.EMPLOYEE)
                .department(dept)
                .isActive(true)
                .isVerified(false)
                .verificationCode(code)
                .verificationCodeExpiry(LocalDateTime.now().plusMinutes(15))
                .build();

        user = userRepo.save(user);

        try {
            emailService.sendVerificationEmail(user.getEmail(), code);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", user.getEmail(), e.getMessage());
        }

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "Registration successful. Please check your email for the verification code.");
        response.put("email", user.getEmail());
        return response;
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!user.isActive()) {
            throw new IllegalArgumentException("Account has been deactivated. Contact administrator.");
        }

        if (!user.isVerified()) {
            throw new EmailNotVerifiedException("Please verify your email before logging in. Check your inbox for the verification code.");
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

    public String verifyEmail(VerifyEmailRequest req) {
        User user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with this email"));

        if (user.isVerified()) {
            return "Email is already verified. You can now log in.";
        }

        if (user.getVerificationCode() == null || user.getVerificationCodeExpiry() == null) {
            throw new UnauthorizedException("No verification code found. Please request a new one.");
        }

        if (user.getVerificationCodeExpiry().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Verification code has expired. Please request a new one.");
        }

        if (!user.getVerificationCode().equals(req.getCode())) {
            throw new IllegalArgumentException("Invalid verification code. Please try again.");
        }

        user.setVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiry(null);
        userRepo.save(user);

        return "Email verified successfully. You can now log in.";
    }

    public java.util.Map<String, Object> resendVerificationCode(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with this email"));

        if (user.isVerified()) {
            return java.util.Map.of("message", "Email is already verified. You can log in.");
        }

        String code = generateVerificationCode();
        user.setVerificationCode(code);
        user.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(15));
        userRepo.save(user);

        try {
            emailService.sendVerificationEmail(user.getEmail(), code);
        } catch (Exception e) {
            log.error("Failed to resend verification email to {}: {}", user.getEmail(), e.getMessage());
        }

        return java.util.Map.of(
                "message", "Verification code resent. Please check your email.",
                "email", user.getEmail()
        );
    }

    public java.util.Map<String, Object> forgotPassword(ForgotPasswordRequest req) {
        User user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with this email"));

        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        userRepo.save(user);

        try {
            emailService.sendResetPasswordEmail(user.getEmail(), resetToken);
        } catch (Exception e) {
            log.error("Failed to send reset password email to {}: {}", user.getEmail(), e.getMessage());
        }

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "Password reset token sent to your email. Please check your inbox.");
        response.put("email", user.getEmail());
        return response;
    }

    public void resetPassword(ResetPasswordRequest req) {
        User user = userRepo.findByResetToken(req.getToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired reset token"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Reset token has expired. Please request a new one.");
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepo.save(user);
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}
