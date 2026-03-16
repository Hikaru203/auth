package com.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@auth-service.local}")
    private String fromEmail;

    @Async
    public void sendPasswordResetEmail(String email, String name, String token) {
        String resetUrl = "http://localhost:3000/reset-password?token=" + token;
        String subject = "Password Reset Request";
        String body = String.format("""
                Hello %s,
                
                You requested a password reset. Click the link below to reset your password:
                
                %s
                
                This link expires in 1 hour.
                
                If you did not request this, please ignore this email.
                
                Auth Service
                """, name, resetUrl);
        sendEmail(email, subject, body);
    }

    @Async
    public void sendAccountLockedEmail(String email, String name) {
        String subject = "Account Locked";
        String body = String.format("""
                Hello %s,
                
                Your account has been temporarily locked due to multiple failed login attempts.
                
                It will be automatically unlocked after 30 minutes.
                If you did not attempt to login, please contact support immediately.
                
                Auth Service
                """, name);
        sendEmail(email, subject, body);
    }

    @Async
    public void sendApiKeyExpiryWarning(String email, String name, String keyName, long daysUntilExpiry) {
        String subject = "API Key Expiring Soon";
        String body = String.format("""
                Hello %s,
                
                Your API key "%s" will expire in %d day(s).
                
                Please log in to regenerate your API key before it expires.
                
                Auth Service
                """, name, keyName, daysUntilExpiry);
        sendEmail(email, subject, body);
    }

    @Async
    public void sendWelcomeEmail(String email, String name) {
        String subject = "Welcome to Auth Service";
        String body = String.format("""
                Hello %s,
                
                Your account has been created successfully.
                
                You can now log in using your credentials.
                
                Auth Service
                """, name);
        sendEmail(email, subject, body);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
