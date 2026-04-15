package com.fellowpick.token;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Data access layer for email verification tokens.
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    // Looks up a verification token by its string value.
    Optional<EmailVerificationToken> findByToken(String token);
}
