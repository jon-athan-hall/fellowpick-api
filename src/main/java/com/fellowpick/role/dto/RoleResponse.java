package com.fellowpick.role.dto;

// Response DTO exposing a role's ID and name.
public record RoleResponse(
        String id,
        String name
) {
}
