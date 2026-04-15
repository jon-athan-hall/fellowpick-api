package com.fellowpick.auth.dto;

import jakarta.validation.constraints.NotBlank;

// Request body for confirming an email verification token.
public record VerifyRequest(
        @NotBlank(message = "Token is required")
        String token
) {
}
