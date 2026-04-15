package com.fellowpick.role;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fellowpick.role.dto.RoleRequest;
import com.fellowpick.user.User;
import com.fellowpick.user.UserRepository;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.HashSet;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Integration tests for the /api/roles CRUD endpoints with admin/user authorization checks.
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RoleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Role userRole;

    @BeforeEach
    void setUp() {
        // The H2 in-memory DB persists across context reloads, so wipe leftover state.
        jdbcTemplate.execute("DELETE FROM refresh_tokens");
        jdbcTemplate.execute("DELETE FROM email_verification_tokens");
        jdbcTemplate.execute("DELETE FROM password_reset_tokens");
        jdbcTemplate.execute("DELETE FROM user_roles");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM roles WHERE name NOT IN ('ROLE_USER', 'ROLE_ADMIN')");

        userRole = roleRepository.findByName("ROLE_USER").orElseThrow();
    }

    // Returns a mock JWT request processor with ROLE_ADMIN authority.
    private RequestPostProcessor asAdmin() {
        return jwt().jwt(j -> j.subject("1").claim("roles", List.of("ROLE_ADMIN")))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    // Returns a mock JWT request processor with ROLE_USER authority.
    private RequestPostProcessor asUser() {
        return jwt().jwt(j -> j.subject("1").claim("roles", List.of("ROLE_USER")))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    // ── GET /api/roles ──────────────────────────────────────────────────

    // Verifies that an admin can list all roles.
    @Test
    void getAllRoles_admin_shouldReturnList() throws Exception {
        mockMvc.perform(get("/api/roles").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // Verifies that a non-admin user gets 403 when listing roles.
    @Test
    void getAllRoles_nonAdmin_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/roles").with(asUser()))
                .andExpect(status().isForbidden());
    }

    // Verifies that an unauthenticated request to list roles returns 401.
    @Test
    void getAllRoles_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/roles/{id} ─────────────────────────────────────────────

    // Verifies that an admin can fetch a role by ID.
    @Test
    void getRoleById_admin_shouldReturn() throws Exception {
        mockMvc.perform(get("/api/roles/{id}", userRole.getId()).with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ROLE_USER"));
    }

    // Verifies that fetching a nonexistent role returns 404.
    @Test
    void getRoleById_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/roles/{id}", "00000000-0000-0000-0000-000000000000").with(asAdmin()))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/roles ─────────────────────────────────────────────────

    // Verifies that an admin can create a new role.
    @Test
    void createRole_admin_shouldCreate() throws Exception {
        var request = new RoleRequest("ROLE_MODERATOR");

        mockMvc.perform(post("/api/roles")
                        .with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ROLE_MODERATOR"))
                .andExpect(jsonPath("$.id").isString());
    }

    // Verifies that creating a role with a duplicate name returns 409 Conflict.
    @Test
    void createRole_duplicate_shouldReturn409() throws Exception {
        var request = new RoleRequest("ROLE_USER");

        mockMvc.perform(post("/api/roles")
                        .with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // Verifies that creating a role with a blank name returns 400.
    @Test
    void createRole_blankName_shouldReturn400() throws Exception {
        var request = new RoleRequest("");

        mockMvc.perform(post("/api/roles")
                        .with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // Verifies that a non-admin user gets 403 when creating a role.
    @Test
    void createRole_nonAdmin_shouldReturn403() throws Exception {
        var request = new RoleRequest("ROLE_NEW");

        mockMvc.perform(post("/api/roles")
                        .with(asUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ── PUT /api/roles/{id} ─────────────────────────────────────────────

    // Verifies that an admin can rename an existing role.
    @Test
    void updateRole_admin_shouldUpdate() throws Exception {
        Role role = roleRepository.save(new Role("ROLE_TEMP"));
        var request = new RoleRequest("ROLE_RENAMED");

        mockMvc.perform(put("/api/roles/{id}", role.getId())
                        .with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ROLE_RENAMED"));
    }

    // Verifies that renaming a role to a name already in use returns 409.
    @Test
    void updateRole_collidingName_shouldReturn409() throws Exception {
        var request = new RoleRequest("ROLE_ADMIN");

        mockMvc.perform(put("/api/roles/{id}", userRole.getId())
                        .with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // ── DELETE /api/roles/{id} ──────────────────────────────────────────

    // Verifies that an admin can delete an unused role.
    @Test
    void deleteRole_admin_unused_shouldDelete() throws Exception {
        Role role = roleRepository.save(new Role("ROLE_TEMP"));

        mockMvc.perform(delete("/api/roles/{id}", role.getId()).with(asAdmin()))
                .andExpect(status().isNoContent());
    }

    // Verifies that deleting a role assigned to a user returns 409 Conflict.
    @Test
    void deleteRole_inUse_shouldReturn409() throws Exception {
        // Assign ROLE_USER to a user so it cannot be deleted.
        User u = new User();
        u.setName("Holder");
        u.setEmail("holder@example.com");
        u.setPassword(passwordEncoder.encode("password123"));
        u.setVerified(true);
        u.setRoles(new HashSet<>(List.of(userRole)));
        userRepository.save(u);

        mockMvc.perform(delete("/api/roles/{id}", userRole.getId()).with(asAdmin()))
                .andExpect(status().isConflict());
    }

    // Verifies that deleting a nonexistent role returns 404.
    @Test
    void deleteRole_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/roles/{id}", "00000000-0000-0000-0000-000000000000").with(asAdmin()))
                .andExpect(status().isNotFound());
    }

    // Verifies that a non-admin user gets 403 when deleting a role.
    @Test
    void deleteRole_nonAdmin_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/roles/{id}", userRole.getId()).with(asUser()))
                .andExpect(status().isForbidden());
    }
}
