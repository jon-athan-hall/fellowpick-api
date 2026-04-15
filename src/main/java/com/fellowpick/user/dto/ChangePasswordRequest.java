package com.fellowpick.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Inbound request DTO for changing a user's password.
public record ChangePasswordRequest(
        // Optional: only required when a user changes their own password.
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String newPassword
) {
}
