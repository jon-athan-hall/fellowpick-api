package com.fellowpick.user.dto;

import jakarta.validation.constraints.NotBlank;

// Inbound request DTO for assigning a role to a user by role ID.
public record AddRoleRequest(
        @NotBlank(message = "Role ID is required")
        String roleId
) {
}
