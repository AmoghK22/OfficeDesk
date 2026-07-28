package com.officedesk.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("OfficeDesk - Email Verification Code");
        message.setText("Your verification code is: " + code + "\n\nThis code will expire in 15 minutes.\n\nIf you did not register for OfficeDesk, please ignore this email.");
        mailSender.send(message);
        log.info("Verification email sent to {}", toEmail);
    }

    public void sendResetPasswordEmail(String toEmail, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("OfficeDesk - Password Reset Token");
        message.setText("Your password reset token is: " + token + "\n\nThis token will expire in 15 minutes.\n\nIf you did not request a password reset, please ignore this email.");
        mailSender.send(message);
        log.info("Password reset email sent to {}", toEmail);
    }
}
