package com.fellowpick.user;

import com.fellowpick.auth.dto.MessageResponse;
import com.fellowpick.user.dto.AddRoleRequest;
import com.fellowpick.user.dto.ChangePasswordRequest;
import com.fellowpick.user.dto.UpdateUserRequest;
import com.fellowpick.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// REST endpoints for user profile, password, role management, and soft delete/restore.
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Returns a user's profile; accessible by the user themselves or an admin.
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or #id.toString() == authentication.name")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // Returns a paginated list of all users (admin only).
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    // Updates a user's name and/or email.
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or #id.toString() == authentication.name")
    public ResponseEntity<UserResponse> updateUser(@PathVariable String id,
                                                   @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    // Changes a user's password; self-service requires current password verification.
    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or #id.toString() == authentication.name")
    public ResponseEntity<MessageResponse> changePassword(@PathVariable String id,
                                                          @Valid @RequestBody ChangePasswordRequest request,
                                                          Authentication authentication) {
        // Self-service password changes must verify the current password.
        // Admins changing another user's password do not.
        boolean isSelf = id.equals(authentication.getName());
        userService.changePassword(id, request, isSelf);
        return ResponseEntity.ok(new MessageResponse("Password updated successfully"));
    }

    // Assigns a role to a user (admin only).
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserResponse> addRole(@PathVariable String id,
                                                @Valid @RequestBody AddRoleRequest request) {
        return ResponseEntity.ok(userService.addRole(id, request.roleId()));
    }

    // Soft-deletes a user and revokes their refresh tokens.
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or #id.toString() == authentication.name")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // Restores a previously soft-deleted user (admin only).
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserResponse> restoreUser(@PathVariable String id) {
        return ResponseEntity.ok(userService.restoreUser(id));
    }

    // Removes a role from a user (admin only).
    @DeleteMapping("/{id}/roles/{roleId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserResponse> removeRole(@PathVariable String id,
                                                   @PathVariable String roleId) {
        return ResponseEntity.ok(userService.removeRole(id, roleId));
    }
}
