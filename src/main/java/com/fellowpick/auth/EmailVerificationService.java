package com.fellowpick.auth;

import com.fellowpick.config.VerificationProperties;
import com.fellowpick.exception.TokenRefreshException;
import com.fellowpick.token.EmailVerificationToken;
import com.fellowpick.token.EmailVerificationTokenRepository;
import com.fellowpick.user.User;
import com.fellowpick.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

// Manages email verification token creation, email dispatch, and token confirmation.
@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    static final String SUBJECT = "Confirm your Fellowpick account";

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final VerificationProperties verificationProperties;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${app.mail.from:}")
    private String mailFrom;

    public EmailVerificationService(EmailVerificationTokenRepository tokenRepository,
                                    UserRepository userRepository,
                                    JavaMailSender mailSender,
                                    VerificationProperties verificationProperties) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
        this.verificationProperties = verificationProperties;
    }

    // Creates a verification token and emails (or logs) a verification link to the user.
    public void sendVerificationEmail(User user) {
        String tokenValue = UUID.randomUUID().toString();

        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken(tokenValue);
        token.setUser(user);
        token.setExpiresAt(Instant.now().plusMillis(verificationProperties.expirationMs()));
        tokenRepository.save(token);

        String verificationLink = verificationProperties.frontendBaseUrl()
                + "/verify?token=" + tokenValue;

        // Log to console for local development when no mail host is configured.
        if (mailHost == null || mailHost.isBlank()) {
            log.info("===== VERIFICATION EMAIL =====");
            log.info("To: {}", user.getEmail());
            log.info("Subject: {}", SUBJECT);
            log.info("Link: {}", verificationLink);
            log.info("==============================");
            return;
        }

        mailSender.send(AuthEmail.build(mailSender, mailFrom, user.getEmail(), SUBJECT,
                user.getName(),
                "Thanks for signing up for Fellowpick. Confirm your email address to finish setting up your account.",
                "Confirm email",
                verificationLink,
                "This link expires in 24 hours. If you didn't create a Fellowpick account, you can ignore this email."));
        log.info("Verification email accepted by SMTP relay for user {}", user.getId());
    }

    // Validates the token and marks the user's email as verified.
    @Transactional
    public void verify(String tokenValue) {
        EmailVerificationToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new TokenRefreshException("Invalid verification token"));

        if (token.getConfirmedAt() != null) {
            throw new TokenRefreshException("Email already verified");
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new TokenRefreshException("Verification token has expired");
        }

        token.setConfirmedAt(Instant.now());
        tokenRepository.save(token);

        User user = token.getUser();
        user.setVerified(true);
        userRepository.save(user);
    }
}
