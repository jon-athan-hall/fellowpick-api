package com.fellowpick.auth;

import com.fellowpick.config.PasswordResetProperties;
import com.fellowpick.exception.TokenRefreshException;
import com.fellowpick.token.PasswordResetToken;
import com.fellowpick.token.PasswordResetTokenRepository;
import com.fellowpick.user.User;
import com.fellowpick.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

// Handles password reset token creation, email dispatch, and password update.
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetProperties properties;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${app.mail.from:}")
    private String mailFrom;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                UserRepository userRepository,
                                JavaMailSender mailSender,
                                PasswordEncoder passwordEncoder,
                                PasswordResetProperties properties) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    // Creates a reset token and emails (or logs) a reset link to the user.
    @Transactional
    public void requestReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("No account found with email: " + email));

        String tokenValue = UUID.randomUUID().toString();

        PasswordResetToken token = new PasswordResetToken();
        token.setToken(tokenValue);
        token.setUser(user);
        token.setExpiresAt(Instant.now().plusMillis(properties.expirationMs()));
        tokenRepository.save(token);

        String resetLink = properties.frontendBaseUrl() + "/reset-password?token=" + tokenValue;

        // Log to console for local development when no mail host is configured.
        if (mailHost == null || mailHost.isBlank()) {
            log.info("===== PASSWORD RESET EMAIL =====");
            log.info("To: {}", user.getEmail());
            log.info("Subject: Reset your password");
            log.info("Link: {}", resetLink);
            log.info("================================");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        if (!mailFrom.isBlank()) {
            message.setFrom(mailFrom);
        }
        message.setTo(user.getEmail());
        message.setSubject("Reset your password");
        message.setText("Click the link below to reset your password:\n\n"
                + resetLink + "\n\n"
                + "This link expires in 15 minutes. If you didn't request this, ignore this email.");

        mailSender.send(message);
    }

    // Validates the reset token and updates the user's password.
    @Transactional
    public void resetPassword(String tokenValue, String newPassword) {
        PasswordResetToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new TokenRefreshException("Invalid reset token"));

        if (token.getUsedAt() != null) {
            throw new TokenRefreshException("Reset token has already been used");
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new TokenRefreshException("Reset token has expired");
        }

        token.setUsedAt(Instant.now());
        tokenRepository.save(token);

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
