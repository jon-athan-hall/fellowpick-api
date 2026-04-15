package com.fellowpick.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fellowpick.role.Role;
import com.fellowpick.role.RoleRepository;
import com.fellowpick.user.dto.AddRoleRequest;
import com.fellowpick.user.dto.ChangePasswordRequest;
import com.fellowpick.user.dto.UpdateUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Integration tests for /api/users endpoints covering CRUD, password change, roles, and soft delete.
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User user;
    private User otherUser;
    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        // The H2 in-memory DB is shared across DirtiesContext reloads, so wipe any leftover user data.
        jdbcTemplate.execute("DELETE FROM refresh_tokens");
        jdbcTemplate.execute("DELETE FROM email_verification_tokens");
        jdbcTemplate.execute("DELETE FROM password_reset_tokens");
        jdbcTemplate.execute("DELETE FROM user_roles");
        jdbcTemplate.execute("DELETE FROM users");

        userRole = roleRepository.findByName("ROLE_USER").orElseThrow();
        adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();

        user = new User();
        user.setName("Alice");
        user.setEmail("alice@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setVerified(true);
        user.setRoles(new HashSet<>(List.of(userRole)));
        user = userRepository.save(user);

        otherUser = new User();
        otherUser.setName("Bob");
        otherUser.setEmail("bob@example.com");
        otherUser.setPassword(passwordEncoder.encode("password123"));
        otherUser.setVerified(true);
        otherUser.setRoles(new HashSet<>(List.of(userRole)));
        otherUser = userRepository.save(otherUser);
    }

    // ── Auth helpers ────────────────────────────────────────────────────

    // Returns a mock JWT request processor with ROLE_USER authority for the given user ID.
    private org.springframework.test.web.servlet.request.RequestPostProcessor asUser(String id) {
        return jwt().jwt(j -> j.subject(id).claim("roles", List.of("ROLE_USER")))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    // Returns a mock JWT request processor with ROLE_USER and ROLE_ADMIN authorities.
    private org.springframework.test.web.servlet.request.RequestPostProcessor asAdmin(String id) {
        return jwt().jwt(j -> j.subject(id)
                        .claim("roles", List.of("ROLE_USER", "ROLE_ADMIN")))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    // ── GET /api/users/{id} ─────────────────────────────────────────────

    // Verifies that a user can fetch their own profile.
    @Test
    void getUserById_self_shouldReturnUser() throws Exception {
        mockMvc.perform(get("/api/users/{id}", user.getId()).with(asUser(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.name").value("Alice"));
    }

    // Verifies that a user cannot fetch another user's profile.
    @Test
    void getUserById_otherUser_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/users/{id}", otherUser.getId()).with(asUser(user.getId())))
                .andExpect(status().isForbidden());
    }

    // Verifies that an admin can fetch any user's profile.
    @Test
    void getUserById_admin_canReadAnyone() throws Exception {
        mockMvc.perform(get("/api/users/{id}", otherUser.getId()).with(asAdmin(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("bob@example.com"));
    }

    // Verifies that an unauthenticated request to fetch a user returns 401.
    @Test
    void getUserById_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users/{id}", user.getId()))
                .andExpect(status().isUnauthorized());
    }

    // Verifies that fetching a nonexistent user returns 404.
    @Test
    void getUserById_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/users/{id}", "00000000-0000-0000-0000-000000000000").with(asAdmin(user.getId())))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/users (list) ───────────────────────────────────────────

    // Verifies that an admin can list all users as a paginated response.
    @Test
    void getAllUsers_admin_shouldReturnPage() throws Exception {
        mockMvc.perform(get("/api/users").with(asAdmin(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    // Verifies that a non-admin user gets 403 when listing all users.
    @Test
    void getAllUsers_nonAdmin_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/users").with(asUser(user.getId())))
                .andExpect(status().isForbidden());
    }

    // ── PUT /api/users/{id} ─────────────────────────────────────────────

    // Verifies that a user can update their own name.
    @Test
    void updateUser_self_shouldUpdateName() throws Exception {
        var request = new UpdateUserRequest("Alice Updated", null);

        mockMvc.perform(put("/api/users/{id}", user.getId())
                        .with(asUser(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Updated"));
    }

    // Verifies that updating a user with an invalid email returns 400.
    @Test
    void updateUser_invalidEmail_shouldReturn400() throws Exception {
        var request = new UpdateUserRequest(null, "not-an-email");

        mockMvc.perform(put("/api/users/{id}", user.getId())
                        .with(asUser(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // Verifies that a user cannot update another user's profile.
    @Test
    void updateUser_otherUser_shouldReturn403() throws Exception {
        var request = new UpdateUserRequest("Hacker", null);

        mockMvc.perform(put("/api/users/{id}", otherUser.getId())
                        .with(asUser(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // Verifies that updating email to one already in use returns 409.
    @Test
    void updateUser_emailAlreadyExists_shouldReturn409() throws Exception {
        var request = new UpdateUserRequest(null, "bob@example.com");

        mockMvc.perform(put("/api/users/{id}", user.getId())
                        .with(asUser(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // ── PUT /api/users/{id}/password ────────────────────────────────────

    // Verifies that a user can change their password with the correct current password.
    @Test
    void changePassword_selfWithCorrect_shouldSucceed() throws Exception {
        var request = new ChangePasswordRequest("password123", "newPassword123");

        mockMvc.perform(put("/api/users/{id}/password", user.getId())
                        .with(asUser(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password updated successfully"));
    }

    // Verifies that changing password with the wrong current password returns 401.
    @Test
    void changePassword_selfWithWrong_shouldReturn401() throws Exception {
        var request = new ChangePasswordRequest("wrongpass", "newPassword123");

        mockMvc.perform(put("/api/users/{id}/password", user.getId())
                        .with(asUser(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // Verifies that an admin can change another user's password without providing the current one.
    @Test
    void changePassword_adminBypassCurrent_shouldSucceed() throws Exception {
        var request = new ChangePasswordRequest(null, "newPassword123");

        mockMvc.perform(put("/api/users/{id}/password", otherUser.getId())
                        .with(asAdmin(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // Verifies that a too-short new password returns 400.
    @Test
    void changePassword_tooShort_shouldReturn400() throws Exception {
        var request = new ChangePasswordRequest("password123", "short");

        mockMvc.perform(put("/api/users/{id}/password", user.getId())
                        .with(asUser(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/users/{id}/roles ───────────────────────────────────────

    // Verifies that an admin can add a role to a user.
    @Test
    void addRole_admin_shouldSucceed() throws Exception {
        var request = new AddRoleRequest(adminRole.getId());

        mockMvc.perform(put("/api/users/{id}/roles", user.getId())
                        .with(asAdmin(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", org.hamcrest.Matchers.hasItem("ROLE_ADMIN")));
    }

    // Verifies that a non-admin user gets 403 when adding a role.
    @Test
    void addRole_nonAdmin_shouldReturn403() throws Exception {
        var request = new AddRoleRequest(adminRole.getId());

        mockMvc.perform(put("/api/users/{id}/roles", user.getId())
                        .with(asUser(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /api/users/{id}/roles/{roleId} ──────────────────────────

    // Verifies that an admin can remove a role from a user.
    @Test
    void removeRole_admin_shouldSucceed() throws Exception {
        mockMvc.perform(delete("/api/users/{id}/roles/{roleId}", user.getId(), userRole.getId())
                        .with(asAdmin(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles.length()").value(0));
    }

    // ── DELETE /api/users/{id} ──────────────────────────────────────────

    // Verifies that a user can soft-delete their own account.
    @Test
    void deleteUser_self_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", user.getId()).with(asUser(user.getId())))
                .andExpect(status().isNoContent());
    }

    // Verifies that a user cannot delete another user's account.
    @Test
    void deleteUser_otherUser_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", otherUser.getId()).with(asUser(user.getId())))
                .andExpect(status().isForbidden());
    }

    // Verifies that an admin can delete any user's account.
    @Test
    void deleteUser_admin_canDeleteAnyone() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", otherUser.getId()).with(asAdmin(user.getId())))
                .andExpect(status().isNoContent());
    }

    // ── POST /api/users/{id}/restore ───────────────────────────────────

    // Verifies that an admin can restore a soft-deleted user.
    @Test
    void restoreUser_admin_shouldRestore() throws Exception {
        // Soft-delete first.
        mockMvc.perform(delete("/api/users/{id}", otherUser.getId()).with(asAdmin(user.getId())))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/users/{id}/restore", otherUser.getId()).with(asAdmin(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("bob@example.com"));
    }

    // Verifies that a non-admin user gets 403 when restoring a deleted user.
    @Test
    void restoreUser_nonAdmin_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/users/{id}/restore", otherUser.getId()).with(asUser(user.getId())))
                .andExpect(status().isForbidden());
    }
}
