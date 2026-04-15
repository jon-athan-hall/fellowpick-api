package com.fellowpick.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fellowpick.auth.dto.ForgotPasswordRequest;
import com.fellowpick.auth.dto.LoginRequest;
import com.fellowpick.auth.dto.RefreshRequest;
import com.fellowpick.auth.dto.RegisterRequest;
import com.fellowpick.auth.dto.ResetPasswordRequest;
import com.fellowpick.auth.dto.VerifyRequest;
import com.fellowpick.token.EmailVerificationToken;
import com.fellowpick.token.EmailVerificationTokenRepository;
import com.fellowpick.token.PasswordResetToken;
import com.fellowpick.token.PasswordResetTokenRepository;
import com.fellowpick.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.function.BooleanSupplier;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Integration tests for all /api/auth endpoints (register, login, refresh, logout, verify, password reset).
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private UserRepository userRepository;

    // Verifies that a valid registration returns access and refresh tokens.
    @Test
    void register_shouldReturnTokens() throws Exception {
        var request = new RegisterRequest("Test User", "test@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    // Verifies that registering with an already-used email returns 409 Conflict.
    @Test
    void register_duplicateEmail_shouldReturn409() throws Exception {
        var request = new RegisterRequest("Test User", "dup@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // Verifies that invalid registration input (blank name, bad email, short password) returns 400.
    @Test
    void register_invalidInput_shouldReturn400() throws Exception {
        var request = new RegisterRequest("", "not-an-email", "short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // Verifies that login with correct credentials returns access and refresh tokens.
    @Test
    void login_shouldReturnTokens() throws Exception {
        register("login@example.com");

        var loginRequest = new LoginRequest("login@example.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    // Verifies that login with a wrong password returns 401 Unauthorized.
    @Test
    void login_badCredentials_shouldReturn401() throws Exception {
        register("bad@example.com");

        var loginRequest = new LoginRequest("bad@example.com", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    // Verifies that a valid refresh token returns new access and refresh tokens.
    @Test
    void refresh_shouldReturnNewTokens() throws Exception {
        String refreshToken = registerAndGetRefreshToken("refresh@example.com");

        var refreshRequest = new RefreshRequest(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").value(not(refreshToken)));
    }

    // Verifies that reusing a rotated (revoked) refresh token returns 401.
    @Test
    void refresh_withRevokedToken_shouldReturn401() throws Exception {
        String refreshToken = registerAndGetRefreshToken("revoked@example.com");

        // Use refresh token once (rotates it, revoking the old one)
        var refreshRequest = new RefreshRequest(refreshToken);
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)));

        // Try to use the old (now revoked) token again
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());
    }

    // Verifies that logout revokes the refresh token so it can no longer be used.
    @Test
    void logout_shouldRevokeRefreshToken() throws Exception {
        String refreshToken = registerAndGetRefreshToken("logout@example.com");

        var logoutRequest = new RefreshRequest(refreshToken);

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        // Verify refresh token is no longer valid
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isUnauthorized());
    }

    // Verifies that a protected endpoint returns 200 when given a valid access token.
    @Test
    void protectedEndpoint_withValidToken_shouldReturn200() throws Exception {
        String accessToken = registerAndGetAccessToken("protected@example.com");

        mockMvc.perform(get("/actuator/health")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    // Verifies that a protected endpoint returns 401 when no token is provided.
    @Test
    void protectedEndpoint_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isUnauthorized());
    }

    // ── /api/auth/verify ───────────────────────────────────────────────

    // Verifies that a valid verification token marks the user's email as verified.
    @Test
    void verify_validToken_shouldMarkVerified() throws Exception {
        register("verify@example.com");
        EmailVerificationToken token = latestVerificationToken();

        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyRequest(token.getToken()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully"));

        assertTrueLater(
                () -> userRepository.findByEmail("verify@example.com").orElseThrow().isVerified());
    }

    // Verifies that a nonexistent verification token returns 401.
    @Test
    void verify_invalidToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyRequest("nonexistent"))))
                .andExpect(status().isUnauthorized());
    }

    // Verifies that an expired verification token returns 401.
    @Test
    void verify_expiredToken_shouldReturn401() throws Exception {
        register("expired@example.com");
        EmailVerificationToken token = latestVerificationToken();
        token.setExpiresAt(Instant.now().minusSeconds(60));
        emailVerificationTokenRepository.save(token);

        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyRequest(token.getToken()))))
                .andExpect(status().isUnauthorized());
    }

    // Verifies that using a verification token a second time returns 401.
    @Test
    void verify_alreadyConfirmed_shouldReturn401() throws Exception {
        register("alreadyverified@example.com");
        EmailVerificationToken token = latestVerificationToken();

        // First verify - succeeds.
        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyRequest(token.getToken()))))
                .andExpect(status().isOk());

        // Second verify - rejected.
        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyRequest(token.getToken()))))
                .andExpect(status().isUnauthorized());
    }

    // Verifies that a blank verification token returns 400.
    @Test
    void verify_blankToken_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyRequest(""))))
                .andExpect(status().isBadRequest());
    }

    // ── /api/auth/forgot-password ───────────────────────────────────────

    // Verifies that forgot-password with a registered email returns 200.
    @Test
    void forgotPassword_validEmail_shouldReturnOk() throws Exception {
        register("forgot@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ForgotPasswordRequest("forgot@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset email sent"));
    }

    // Verifies that forgot-password with an unknown email returns 404.
    @Test
    void forgotPassword_unknownEmail_shouldReturn404() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ForgotPasswordRequest("nobody@example.com"))))
                .andExpect(status().isNotFound());
    }

    // Verifies that forgot-password with a malformed email returns 400.
    @Test
    void forgotPassword_invalidEmail_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ForgotPasswordRequest("not-an-email"))))
                .andExpect(status().isBadRequest());
    }

    // ── /api/auth/reset-password ────────────────────────────────────────

    // Verifies that reset-password with a valid token updates the password.
    @Test
    void resetPassword_validToken_shouldChangePassword() throws Exception {
        register("reset@example.com");
        requestPasswordReset("reset@example.com");
        PasswordResetToken token = latestResetToken();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResetPasswordRequest(token.getToken(), "newPassword123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully"));

        // Login with the new password should succeed.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("reset@example.com", "newPassword123"))))
                .andExpect(status().isOk());
    }

    // Verifies that reset-password with a nonexistent token returns 401.
    @Test
    void resetPassword_invalidToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResetPasswordRequest("nonexistent", "newPassword123"))))
                .andExpect(status().isUnauthorized());
    }

    // Verifies that reusing a reset token a second time returns 401.
    @Test
    void resetPassword_reusedToken_shouldReturn401() throws Exception {
        register("reused@example.com");
        requestPasswordReset("reused@example.com");
        PasswordResetToken token = latestResetToken();

        // First use succeeds.
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResetPasswordRequest(token.getToken(), "newPassword123"))))
                .andExpect(status().isOk());

        // Second use rejected.
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResetPasswordRequest(token.getToken(), "anotherPassword123"))))
                .andExpect(status().isUnauthorized());
    }

    // Verifies that reset-password with a too-short password returns 400.
    @Test
    void resetPassword_shortPassword_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResetPasswordRequest("any-token", "short"))))
                .andExpect(status().isBadRequest());
    }

    // ── /api/auth/resend-verification ───────────────────────────────────

    // Verifies that an unverified authenticated user can request a new verification email.
    @Test
    void resendVerification_authenticatedUnverified_shouldReturnOk() throws Exception {
        String accessToken = registerAndGetAccessToken("resend@example.com");

        mockMvc.perform(post("/api/auth/resend-verification")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Verification email sent"));
    }

    // Verifies that resending verification for an already-verified user returns an "already verified" message.
    @Test
    void resendVerification_alreadyVerified_shouldReturnAlreadyVerifiedMessage() throws Exception {
        String accessToken = registerAndGetAccessToken("alreadydone@example.com");

        // Mark verified directly.
        var user = userRepository.findByEmail("alreadydone@example.com").orElseThrow();
        user.setVerified(true);
        userRepository.save(user);

        mockMvc.perform(post("/api/auth/resend-verification")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email is already verified"));
    }

    // ── Helper Methods ──────────────────────────────────────────────────

    // Returns the most recently saved email verification token from the database.
    private EmailVerificationToken latestVerificationToken() {
        List<EmailVerificationToken> all = emailVerificationTokenRepository.findAll();
        return all.getLast();
    }

    // Returns the most recently saved password reset token from the database.
    private PasswordResetToken latestResetToken() {
        List<PasswordResetToken> all = passwordResetTokenRepository.findAll();
        return all.getLast();
    }

    // Sends a forgot-password request for the given email.
    private void requestPasswordReset(String email) throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(email))));
    }

    // Asserts that the given condition is true, throwing if not.
    private void assertTrueLater(BooleanSupplier condition) {
        if (!condition.getAsBoolean()) {
            throw new AssertionError("user should be verified");
        }
    }


    // Registers a new user with the given email and a default password.
    private void register(String email) throws Exception {
        var request = new RegisterRequest("Test User", email, "password123");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    // Registers a new user and returns the refresh token from the response.
    private String registerAndGetRefreshToken(String email) throws Exception {
        var request = new RegisterRequest("Test User", email, "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        var json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("refreshToken").asText();
    }

    // Registers a new user and returns the access token from the response.
    private String registerAndGetAccessToken(String email) throws Exception {
        var request = new RegisterRequest("Test User", email, "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        var json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }
}
