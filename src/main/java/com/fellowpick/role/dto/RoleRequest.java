package com.fellowpick.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Inbound request DTO for creating or updating a role.
public record RoleRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 50, message = "Name must be at most 50 characters")
        String name
) {
}
