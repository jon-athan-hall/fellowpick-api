package com.fellowpick.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Binds password reset config (expiration, frontend URL) from application properties.
@ConfigurationProperties(prefix = "auth.password-reset")
public record PasswordResetProperties(
        long expirationMs,
        String frontendBaseUrl
) {
}
