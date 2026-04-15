package com.fellowpick.auth;

import com.fellowpick.config.JwtProperties;
import com.fellowpick.role.Role;
import com.fellowpick.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// Unit tests for JwtTokenService covering JWT claim generation and expiration.
class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
        jwtDecoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();

        JwtProperties properties = new JwtProperties("test-issuer", 900000);
        jwtTokenService = new JwtTokenService(encoder, properties);
    }

    // Verifies that a generated JWT contains subject, email, name, issuer, and roles claims.
    @Test
    void generateAccessToken_shouldContainExpectedClaims() {
        User user = createTestUser();

        String token = jwtTokenService.generateAccessToken(user);

        assertNotNull(token);
        Jwt decoded = jwtDecoder.decode(token);

        assertEquals("11111111-1111-1111-1111-111111111111", decoded.getSubject());
        assertEquals("test@example.com", decoded.getClaimAsString("email"));
        assertEquals("Test User", decoded.getClaimAsString("name"));
        assertEquals("test-issuer", decoded.getClaimAsString("iss"));

        List<String> roles = decoded.getClaimAsStringList("roles");
        assertTrue(roles.contains("ROLE_USER"));
    }

    // Verifies that the JWT expiration is set after the issued-at time.
    @Test
    void generateAccessToken_shouldHaveCorrectExpiration() {
        User user = createTestUser();

        String token = jwtTokenService.generateAccessToken(user);
        Jwt decoded = jwtDecoder.decode(token);

        assertNotNull(decoded.getIssuedAt());
        assertNotNull(decoded.getExpiresAt());
        assertTrue(decoded.getExpiresAt().isAfter(decoded.getIssuedAt()));
    }

    // Verifies that getExpirationMs returns the value from JwtProperties.
    @Test
    void getExpirationMs_shouldReturnConfiguredValue() {
        assertEquals(900000, jwtTokenService.getExpirationMs());
    }

    // Creates a test user with one ROLE_USER role.
    private User createTestUser() {
        User user = new User();
        user.setId("11111111-1111-1111-1111-111111111111");
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPassword("encoded-password");

        Role role = new Role("ROLE_USER");
        role.setId("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        user.setRoles(Set.of(role));

        return user;
    }
}
