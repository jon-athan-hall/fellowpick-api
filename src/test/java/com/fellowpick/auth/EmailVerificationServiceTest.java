package com.fellowpick.auth;

import com.fellowpick.config.VerificationProperties;
import com.fellowpick.exception.TokenRefreshException;
import com.fellowpick.token.EmailVerificationToken;
import com.fellowpick.token.EmailVerificationTokenRepository;
import com.fellowpick.user.User;
import com.fellowpick.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Unit tests for EmailVerificationService covering token creation, email sending, and verification.
@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        VerificationProperties properties = new VerificationProperties(86400000, "http://localhost:5173");
        // @InjectMocks doesn't pick up record-only constructor args mixed with field-injected @Value, so set manually.
        emailVerificationService = new EmailVerificationService(
                tokenRepository, userRepository, mailSender, properties);

        testUser = new User();
        testUser.setId("11111111-1111-1111-1111-111111111111");
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setVerified(false);
    }

    // Verifies that when mail host is blank, the token is saved but no email is sent.
    @Test
    void sendVerificationEmail_whenMailHostBlank_shouldSaveTokenAndSkipSend() {
        ReflectionTestUtils.setField(emailVerificationService, "mailHost", "");

        emailVerificationService.sendVerificationEmail(testUser);

        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(captor.capture());
        EmailVerificationToken saved = captor.getValue();

        assertNotNull(saved.getToken());
        assertEquals(testUser, saved.getUser());
        assertTrue(saved.getExpiresAt().isAfter(Instant.now()));

        verifyNoInteractions(mailSender);
    }

    // Verifies that when mail host is configured, a verification email is sent to the user.
    @Test
    void sendVerificationEmail_whenMailHostConfigured_shouldSendMail() {
        ReflectionTestUtils.setField(emailVerificationService, "mailHost", "smtp.example.com");

        emailVerificationService.sendVerificationEmail(testUser);

        verify(tokenRepository).save(any(EmailVerificationToken.class));

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sent = messageCaptor.getValue();

        assertNotNull(sent.getTo());
        assertEquals("test@example.com", sent.getTo()[0]);
        assertEquals("Verify your email", sent.getSubject());
        assertNotNull(sent.getText());
        assertTrue(sent.getText().contains("http://localhost:5173/verify?token="));
    }

    // Verifies that a valid token marks the user as verified.
    @Test
    void verify_validToken_shouldMarkUserVerified() {
        EmailVerificationToken token = validToken();
        when(tokenRepository.findByToken("good-token")).thenReturn(Optional.of(token));

        emailVerificationService.verify("good-token");

        assertNotNull(token.getConfirmedAt());
        assertTrue(testUser.isVerified());
        verify(tokenRepository).save(token);
        verify(userRepository).save(testUser);
    }

    // Verifies that a nonexistent token throws TokenRefreshException.
    @Test
    void verify_invalidToken_shouldThrow() {
        when(tokenRepository.findByToken("nonexistent")).thenReturn(Optional.empty());

        assertThrows(TokenRefreshException.class, () -> emailVerificationService.verify("nonexistent"));
        verify(userRepository, never()).save(any());
    }

    // Verifies that an expired token throws TokenRefreshException.
    @Test
    void verify_expiredToken_shouldThrow() {
        EmailVerificationToken token = validToken();
        token.setExpiresAt(Instant.now().minusSeconds(60));
        when(tokenRepository.findByToken("expired")).thenReturn(Optional.of(token));

        assertThrows(TokenRefreshException.class, () -> emailVerificationService.verify("expired"));
        assertFalse(testUser.isVerified());
    }

    // Verifies that an already-confirmed token throws TokenRefreshException.
    @Test
    void verify_alreadyConfirmed_shouldThrow() {
        EmailVerificationToken token = validToken();
        token.setConfirmedAt(Instant.now().minusSeconds(60));
        when(tokenRepository.findByToken("used")).thenReturn(Optional.of(token));

        assertThrows(TokenRefreshException.class, () -> emailVerificationService.verify("used"));
        verify(userRepository, never()).save(any());
    }

    // Creates a non-expired, unused verification token for testUser.
    private EmailVerificationToken validToken() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setId(1L);
        token.setToken("good-token");
        token.setUser(testUser);
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        return token;
    }
}
