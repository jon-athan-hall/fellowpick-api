package com.fellowpick.auth.dto;

import jakarta.validation.constraints.NotBlank;

// Request body for refreshing an access token using a refresh token.
public record RefreshRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
