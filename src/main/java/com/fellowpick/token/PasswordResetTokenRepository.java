package com.fellowpick.token;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Data access layer for password reset tokens.
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    // Looks up a password reset token by its string value.
    Optional<PasswordResetToken> findByToken(String token);
}
