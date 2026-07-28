package com.officedesk.service;

import com.officedesk.dto.auth.LoginRequest;
import com.officedesk.dto.auth.RegisterRequest;
import com.officedesk.dto.auth.VerifyEmailRequest;
import com.officedesk.entity.Department;
import com.officedesk.entity.User;
import com.officedesk.enums.DepartmentName;
import com.officedesk.enums.Role;
import com.officedesk.exception.DuplicateEmailException;
import com.officedesk.exception.EmailNotVerifiedException;
import com.officedesk.repository.DepartmentRepository;
import com.officedesk.repository.UserRepository;
import com.officedesk.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepo;
    @Mock private DepartmentRepository deptRepo;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private EmailService emailService;

    @InjectMocks private AuthService authService;

    private Department department;
    private User user;

    @BeforeEach
    void setUp() {
        department = Department.builder().id(1L).name(DepartmentName.IT).build();
        user = User.builder().id(1L).name("Rahul").email("rahul@test.com")
                .password("encoded").role(Role.EMPLOYEE).department(department)
                .isActive(true).isVerified(true).build();
    }

    @Test
    @DisplayName("Register - success returns message and email")
    void register_success() {
        RegisterRequest req = RegisterRequest.builder().name("Rahul").email("rahul@test.com").password("pass123").departmentId(1L).build();

        when(userRepo.findByEmail("rahul@test.com")).thenReturn(Optional.empty());
        when(deptRepo.findById(1L)).thenReturn(Optional.of(department));
        when(passwordEncoder.encode("pass123")).thenReturn("encoded");
        when(userRepo.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        Map<String, Object> res = authService.register(req);

        assertThat(res).containsKey("message");
        assertThat(res).containsEntry("email", "rahul@test.com");
        verify(userRepo).save(any(User.class));
    }

    @Test
    @DisplayName("Register - duplicate email with verified user throws DuplicateEmailException")
    void register_duplicateEmail_throws() {
        User verifiedUser = User.builder().id(1L).name("Rahul").email("rahul@test.com")
                .password("encoded").role(Role.EMPLOYEE).department(department)
                .isActive(true).isVerified(true).build();
        RegisterRequest req = RegisterRequest.builder().name("Rahul").email("rahul@test.com").password("pass123").departmentId(1L).build();
        when(userRepo.findByEmail("rahul@test.com")).thenReturn(Optional.of(verifiedUser));

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(DuplicateEmailException.class);
        verify(userRepo, never()).save(any());
    }

    @Test
    @DisplayName("Register - department not found throws IllegalArgumentException")
    void register_deptNotFound_throws() {
        RegisterRequest req = RegisterRequest.builder().name("Rahul").email("rahul@test.com").password("pass123").departmentId(99L).build();
        when(userRepo.findByEmail("rahul@test.com")).thenReturn(Optional.empty());
        when(deptRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Login - success")
    void login_success() {
        LoginRequest req = LoginRequest.builder().email("rahul@test.com").password("pass123").build();
        when(userRepo.findByEmail("rahul@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass123", "encoded")).thenReturn(true);
        when(jwtUtil.generateToken(1L, "rahul@test.com", "EMPLOYEE")).thenReturn("jwt-token");

        var res = authService.login(req);

        assertThat(res.getAccessToken()).isEqualTo("jwt-token");
        assertThat(res.getRole()).isEqualTo("EMPLOYEE");
    }

    @Test
    @DisplayName("Login - unverified user throws EmailNotVerifiedException")
    void login_notVerified_throws() {
        User unverified = User.builder().id(2L).name("Rahul").email("rahul@test.com")
                .password("encoded").role(Role.EMPLOYEE).department(department)
                .isActive(true).isVerified(false).build();
        LoginRequest req = LoginRequest.builder().email("rahul@test.com").password("pass123").build();
        when(userRepo.findByEmail("rahul@test.com")).thenReturn(Optional.of(unverified));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(EmailNotVerifiedException.class)
                .hasMessageContaining("verify your email");
    }

    @Test
    @DisplayName("Login - wrong password throws IllegalArgumentException")
    void login_wrongPassword_throws() {
        LoginRequest req = LoginRequest.builder().email("rahul@test.com").password("wrong").build();
        when(userRepo.findByEmail("rahul@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("Login - deactivated account throws IllegalArgumentException")
    void login_deactivated_throws() {
        user.setActive(false);
        LoginRequest req = LoginRequest.builder().email("rahul@test.com").password("pass123").build();
        when(userRepo.findByEmail("rahul@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deactivated");
    }

    @Test
    @DisplayName("Login - nonexistent user throws IllegalArgumentException")
    void login_userNotFound_throws() {
        LoginRequest req = LoginRequest.builder().email("nobody@test.com").password("pass123").build();
        when(userRepo.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Verify email - success")
    void verifyEmail_success() {
        User unverified = User.builder().id(1L).name("Rahul").email("rahul@test.com")
                .password("encoded").role(Role.EMPLOYEE).department(department)
                .isActive(true).isVerified(false)
                .verificationCode("123456")
                .verificationCodeExpiry(LocalDateTime.now().plusMinutes(10))
                .build();
        when(userRepo.findByEmail("rahul@test.com")).thenReturn(Optional.of(unverified));
        when(userRepo.save(any(User.class))).thenReturn(unverified);

        VerifyEmailRequest req = VerifyEmailRequest.builder().email("rahul@test.com").code("123456").build();
        String result = authService.verifyEmail(req);

        assertThat(result).contains("verified");
        verify(userRepo).save(any(User.class));
    }

    @Test
    @DisplayName("Verify email - wrong code throws IllegalArgumentException")
    void verifyEmail_wrongCode_throws() {
        User unverified = User.builder().id(1L).name("Rahul").email("rahul@test.com")
                .password("encoded").role(Role.EMPLOYEE).department(department)
                .isActive(true).isVerified(false)
                .verificationCode("123456")
                .verificationCodeExpiry(LocalDateTime.now().plusMinutes(10))
                .build();
        when(userRepo.findByEmail("rahul@test.com")).thenReturn(Optional.of(unverified));

        VerifyEmailRequest req = VerifyEmailRequest.builder().email("rahul@test.com").code("999999").build();
        assertThatThrownBy(() -> authService.verifyEmail(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid verification code");
    }

    @Test
    @DisplayName("Resend verification - success")
    void resendVerification_success() {
        User unverified = User.builder().id(1L).name("Rahul").email("rahul@test.com")
                .password("encoded").role(Role.EMPLOYEE).department(department)
                .isActive(true).isVerified(false).build();
        when(userRepo.findByEmail("rahul@test.com")).thenReturn(Optional.of(unverified));
        when(userRepo.save(any(User.class))).thenReturn(unverified);

        Map<String, Object> result = authService.resendVerificationCode("rahul@test.com");

        assertThat(result).containsKey("message");
        verify(emailService).sendVerificationEmail(eq("rahul@test.com"), anyString());
    }
}
