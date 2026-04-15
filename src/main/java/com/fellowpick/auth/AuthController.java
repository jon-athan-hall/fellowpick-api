package com.fellowpick.auth;

import com.fellowpick.auth.dto.*;
import com.fellowpick.config.RefreshTokenProperties;
import com.fellowpick.user.User;
import com.fellowpick.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Supports two refresh token delivery modes, configured via auth.refresh-token.delivery:
 * - "cookie": refresh token is sent as a secure HTTP-only cookie (production)
 * - "body": refresh token is included in the JSON response (Postman/testing)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final UserRepository userRepository;
    private final RefreshTokenProperties refreshTokenProperties;

    public AuthController(AuthService authService,
                          EmailVerificationService emailVerificationService,
                          PasswordResetService passwordResetService,
                          UserRepository userRepository,
                          RefreshTokenProperties refreshTokenProperties) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
        this.passwordResetService = passwordResetService;
        this.userRepository = userRepository;
        this.refreshTokenProperties = refreshTokenProperties;
    }

    // Registers a new user and returns JWT access + refresh tokens.
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletResponse response) {
        AuthResponse authResponse = authService.register(request);
        return buildAuthResponse(authResponse, response);
    }

    // Authenticates a user by email/password and returns JWT access + refresh tokens.
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);
        return buildAuthResponse(authResponse, response);
    }

    // Rotates refresh token and issues a new access token.
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody(required = false) RefreshRequest request,
                                                HttpServletRequest httpRequest,
                                                HttpServletResponse httpResponse) {
        String refreshToken = extractRefreshToken(request, httpRequest);
        AuthResponse authResponse = authService.refresh(refreshToken);
        return buildAuthResponse(authResponse, httpResponse);
    }

    // Sends a password reset email to the given address.
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.ok(new MessageResponse("Password reset email sent"));
    }

    // Resets the user's password using a valid reset token.
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(new MessageResponse("Password reset successfully"));
    }

    // Confirms the user's email address using a verification token.
    @PostMapping("/verify")
    public ResponseEntity<MessageResponse> verify(@Valid @RequestBody VerifyRequest request) {
        emailVerificationService.verify(request.token());
        return ResponseEntity.ok(new MessageResponse("Email verified successfully"));
    }

    // Re-sends the verification email for the currently authenticated user.
    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (user.isVerified()) {
            return ResponseEntity.ok(new MessageResponse("Email is already verified"));
        }

        emailVerificationService.sendVerificationEmail(user);
        return ResponseEntity.ok(new MessageResponse("Verification email sent"));
    }

    // Revokes the refresh token and clears the cookie if in cookie mode.
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@RequestBody(required = false) RefreshRequest request,
                                                  HttpServletRequest httpRequest,
                                                  HttpServletResponse httpResponse) {
        String refreshToken = extractRefreshToken(request, httpRequest);
        authService.logout(refreshToken);

        if (refreshTokenProperties.isCookieDelivery()) {
            clearRefreshTokenCookie(httpResponse);
        }

        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    // Reads the refresh token from cookie or request body based on delivery mode.
    private String extractRefreshToken(RefreshRequest request, HttpServletRequest httpRequest) {
        if (refreshTokenProperties.isCookieDelivery()) {
            return readCookie(httpRequest, refreshTokenProperties.cookieName());
        }
        return request != null ? request.refreshToken() : null;
    }

    // Finds a cookie by name in the incoming request.
    private String readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    // Wraps the auth response, setting a cookie or including the refresh token in the body.
    private ResponseEntity<AuthResponse> buildAuthResponse(AuthResponse authResponse,
                                                           HttpServletResponse response) {
        if (refreshTokenProperties.isCookieDelivery()) {
            setRefreshTokenCookie(response, authResponse.refreshToken());
            AuthResponse bodyWithoutRefresh = new AuthResponse(
                    authResponse.accessToken(),
                    null,
                    authResponse.expiresIn()
            );
            return ResponseEntity.ok(bodyWithoutRefresh);
        }
        return ResponseEntity.ok(authResponse);
    }

    // Writes the refresh token as a secure HTTP-only cookie on the response.
    private void setRefreshTokenCookie(HttpServletResponse response, String tokenValue) {
        ResponseCookie cookie = ResponseCookie.from(refreshTokenProperties.cookieName(), tokenValue)
                .httpOnly(true)
                .secure(refreshTokenProperties.cookieSecure())
                .sameSite(refreshTokenProperties.cookieSameSite())
                .path(refreshTokenProperties.cookiePath())
                .maxAge(refreshTokenProperties.cookieMaxAgeSeconds())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // Expires the refresh token cookie to clear it from the browser.
    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(refreshTokenProperties.cookieName(), "")
                .httpOnly(true)
                .secure(refreshTokenProperties.cookieSecure())
                .sameSite(refreshTokenProperties.cookieSameSite())
                .path(refreshTokenProperties.cookiePath())
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
