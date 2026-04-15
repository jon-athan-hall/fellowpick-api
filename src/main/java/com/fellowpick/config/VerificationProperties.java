package com.fellowpick.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Binds email verification config (expiration, frontend URL) from application properties.
@ConfigurationProperties(prefix = "auth.verification")
public record VerificationProperties(
        long expirationMs,
        String frontendBaseUrl
) {
}
