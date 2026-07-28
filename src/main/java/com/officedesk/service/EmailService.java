package com.officedesk.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String SENDGRID_API_URL = "https://api.sendgrid.com/v3/mail/send";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${sendgrid.api-key:}")
    private String apiKey;

    @Value("${sendgrid.from-email:noreply@officedesk.com}")
    private String fromEmail;

    public EmailService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendVerificationEmail(String toEmail, String code) {
        String subject = "OfficeDesk - Email Verification Code";
        String body = "Your verification code is: " + code + "\n\nThis code will expire in 15 minutes.\n\nIf you did not register for OfficeDesk, please ignore this email.";
        sendEmail(toEmail, subject, body);
    }

    public void sendResetPasswordEmail(String toEmail, String token) {
        String subject = "OfficeDesk - Password Reset Token";
        String body = "Your password reset token is: " + token + "\n\nThis token will expire in 15 minutes.\n\nIf you did not request a password reset, please ignore this email.";
        sendEmail(toEmail, subject, body);
    }

    private void sendEmail(String toEmail, String subject, String body) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("SENDGRID_API_KEY not configured. Skipping email to {}. Subject: {}", toEmail, subject);
            return;
        }

        try {
            Map<String, Object> email = Map.of(
                    "personalizations", List.of(Map.of(
                            "to", List.of(Map.of("email", toEmail)),
                            "subject", subject
                    )),
                    "from", Map.of("email", fromEmail, "name", "OfficeDesk"),
                    "content", List.of(Map.of("type", "text/plain", "value", body))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(email), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(SENDGRID_API_URL, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Email sent to {} - Subject: {}", toEmail, subject);
            } else {
                log.error("SendGrid returned {} for email to {}", response.getStatusCode(), toEmail);
            }
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }
}
