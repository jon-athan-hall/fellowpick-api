package com.fellowpick.role;

import com.fellowpick.role.dto.RoleRequest;
import com.fellowpick.role.dto.RoleResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Admin-only REST endpoints for CRUD operations on roles.
@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    // Lists every role in the system.
    @GetMapping
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    // Returns a single role by its ID.
    @GetMapping("/{id}")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable String id) {
        return ResponseEntity.ok(roleService.getRoleById(id));
    }

    // Creates a new role with a unique name.
    @PostMapping
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody RoleRequest request) {
        return ResponseEntity.ok(roleService.createRole(request));
    }

    // Renames an existing role.
    @PutMapping("/{id}")
    public ResponseEntity<RoleResponse> updateRole(@PathVariable String id,
                                                   @Valid @RequestBody RoleRequest request) {
        return ResponseEntity.ok(roleService.updateRole(id, request));
    }

    // Deletes a role if it is not assigned to any users.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable String id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
}
