package com.pnpn.shared.config;

import com.pnpn.shared.enums.UserType;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET = "test-secret-key-for-unit-tests-must-be-at-least-256-bits";
    private static final long EXPIRATION_MS = 900000L;

    private JwtUtil jwtUtil;
    private JwtUtil expiredJwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION_MS);
        expiredJwtUtil = new JwtUtil(SECRET, -1000L);
    }

    @Test
    void testGenerateAndParseToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId, UserType.INFLUENCER);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        var claims = jwtUtil.parseToken(token);
        assertEquals(userId.toString(), claims.getSubject());
        assertEquals("INFLUENCER", claims.get("userType", String.class));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    void testGetUserIdFromToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId, UserType.FOLLOWER);

        assertEquals(userId, jwtUtil.getUserIdFromToken(token));
    }

    @Test
    void testGetUserTypeFromToken() {
        UUID userId = UUID.randomUUID();

        String influencerToken = jwtUtil.generateAccessToken(userId, UserType.INFLUENCER);
        assertEquals(UserType.INFLUENCER, jwtUtil.getUserTypeFromToken(influencerToken));

        String followerToken = jwtUtil.generateAccessToken(userId, UserType.FOLLOWER);
        assertEquals(UserType.FOLLOWER, jwtUtil.getUserTypeFromToken(followerToken));

        String adminToken = jwtUtil.generateAccessToken(userId, UserType.ADMIN);
        assertEquals(UserType.ADMIN, jwtUtil.getUserTypeFromToken(adminToken));
    }

    @Test
    void testTokenExpirationIsSetCorrectly() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId, UserType.INFLUENCER);

        var claims = jwtUtil.parseToken(token);
        long now = System.currentTimeMillis();
        long exp = claims.getExpiration().getTime();
        long iat = claims.getIssuedAt().getTime();

        assertTrue(exp > now);
        assertTrue(exp - iat <= EXPIRATION_MS + 1000);
    }

    @Test
    void testIsTokenExpiredWithValidToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId, UserType.INFLUENCER);

        assertFalse(jwtUtil.isTokenExpired(token));
    }

    @Test
    void testIsTokenExpiredWithExpiredToken() {
        UUID userId = UUID.randomUUID();
        String token = expiredJwtUtil.generateAccessToken(userId, UserType.INFLUENCER);

        assertTrue(expiredJwtUtil.isTokenExpired(token));
    }

    @Test
    void testIsTokenExpiredWithMalformedTokenThrowsException() {
        assertThrows(MalformedJwtException.class,
                () -> jwtUtil.isTokenExpired("malformed-jwt-token"));
    }

    @Test
    void testValidateTokenWithValidToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId, UserType.INFLUENCER);

        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void testValidateTokenWithExpiredToken() {
        UUID userId = UUID.randomUUID();
        String token = expiredJwtUtil.generateAccessToken(userId, UserType.INFLUENCER);

        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    void testValidateTokenWithMalformedToken() {
        assertFalse(jwtUtil.validateToken("malformed-jwt-token"));
    }

    @Test
    void testValidateTokenWithEmptyToken() {
        assertFalse(jwtUtil.validateToken(""));
    }

    @Test
    void testValidateTokenWithNullToken() {
        assertFalse(jwtUtil.validateToken(null));
    }

    @Test
    void testValidateTokenWithInvalidSignature() {
        JwtUtil differentKeyUtil = new JwtUtil(
                "a-different-secret-key-that-is-also-256-bits-long!!!!!!", EXPIRATION_MS);
        UUID userId = UUID.randomUUID();
        String token = differentKeyUtil.generateAccessToken(userId, UserType.INFLUENCER);

        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    void testValidateTokenWithUnsignedToken() {
        assertFalse(jwtUtil.validateToken("eyJhbGciOiJub25lIn0.eyJzdWIiOiIxMjM0NTY3ODkwIn0."));
    }

    @Test
    void testMultipleUserIdsGenerateDifferentSubjects() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        String token1 = jwtUtil.generateAccessToken(userId1, UserType.INFLUENCER);
        String token2 = jwtUtil.generateAccessToken(userId2, UserType.INFLUENCER);

        UUID extracted1 = jwtUtil.getUserIdFromToken(token1);
        UUID extracted2 = jwtUtil.getUserIdFromToken(token2);

        assertEquals(userId1, extracted1);
        assertEquals(userId2, extracted2);
        assertNotEquals(extracted1, extracted2);
    }
}
