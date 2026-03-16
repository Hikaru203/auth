package com.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "app.security")
@Data
public class SecurityProperties {

    private AccountLockout accountLockout = new AccountLockout();
    private PasswordPolicy passwordPolicy = new PasswordPolicy();
    private RateLimit rateLimit = new RateLimit();
    private ApiKeySettings apiKey = new ApiKeySettings();
    private Cors cors = new Cors();

    @Data
    public static class AccountLockout {
        private int maxFailedAttempts = 5;
        private Duration lockoutDuration = Duration.ofMinutes(30);

        public int getLockoutDurationMinutes() {
            return (int) lockoutDuration.toMinutes();
        }
    }

    @Data
    public static class Cors {
        private String allowedOrigins = "*";
        private String allowedMethods = "GET,POST,PUT,DELETE,PATCH,OPTIONS";
        private String allowedHeaders = "*";
        private boolean allowCredentials = true;
        private long maxAge = 3600;
    }

    @Data
    public static class PasswordPolicy {
        private int minLength = 8;
        private boolean requireUppercase = true;
        private boolean requireLowercase = true;
        private boolean requireDigit = true;
        private boolean requireSpecial = true;
        private int maxAgeDays = 90;
    }

    @Data
    public static class RateLimit {
        private int loginRequestsPerMinute = 10;
        private int apiRequestsPerMinute = 100;
    }

    @Data
    public static class ApiKeySettings {
        private int maxKeysPerUser = 10;
        private int defaultExpiryDays = 365;
        private String prefix = "ak_";
    }
}
