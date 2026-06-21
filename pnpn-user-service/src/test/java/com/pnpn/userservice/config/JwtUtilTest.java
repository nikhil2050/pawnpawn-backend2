package com.pnpn.userservice.config;

import com.pnpn.shared.config.JwtUtil;
import com.pnpn.shared.enums.UserType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JwtUtil}.
 * Covers token generation, parsing, claim extraction, and validation
 * including expired / malformed / tampered token scenarios.
 */
class JwtUtilTest {

    private static final String SECRET = "my-secret-key-for-testing-purposes-only-32-chars!!";
    private static final long EXPIRATION_MS = 900000; // 15 min

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION_MS);
    }

    @Test
    void generateAccessToken_shouldReturnValidJwt() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId, UserType.INFLUENCER);

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3, "JWT must have 3 parts");
    }

    @Test
    void generateAccessToken_shouldEncodeUserIdInSubject() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId, UserType.FOLLOWER);

        assertEquals(userId, jwtUtil.getUserIdFromToken(token));
    }

    @Test
    void generateAccessToken_shouldEncodeUserTypeInClaim() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId, UserType.INFLUENCER);

        assertEquals(UserType.INFLUENCER, jwtUtil.getUserTypeFromToken(token));
    }

    @Test
    void parseToken_shouldReturnClaimsForValidToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId, UserType.ADMIN);

        Claims claims = jwtUtil.parseToken(token);

        assertEquals(userId.toString(), claims.getSubject());
        assertEquals("ADMIN", claims.get("userType", String.class));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    void getUserIdFromToken_shouldReturnCorrectId() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId, UserType.FOLLOWER);

        assertEquals(userId, jwtUtil.getUserIdFromToken(token));
    }

    @Test
    void getUserTypeFromToken_shouldReturnCorrectType() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId, UserType.INFLUENCER);

        assertEquals(UserType.INFLUENCER, jwtUtil.getUserTypeFromToken(token));
    }

    @Test
    void isTokenExpired_shouldReturnFalseForValidToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId, UserType.FOLLOWER);

        assertFalse(jwtUtil.isTokenExpired(token));
    }

    @Test
    void isTokenExpired_shouldReturnTrueForExpiredToken() throws Exception {
        JwtUtil shortLived = new JwtUtil(SECRET, -1); // already expired
        UUID userId = UUID.randomUUID();
        String token = shortLived.generateAccessToken(userId, UserType.FOLLOWER);

        assertTrue(shortLived.isTokenExpired(token));
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId, UserType.FOLLOWER);

        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_shouldReturnFalseForExpiredToken() {
        JwtUtil shortLived = new JwtUtil(SECRET, -1);
        UUID userId = UUID.randomUUID();
        String token = shortLived.generateAccessToken(userId, UserType.FOLLOWER);

        assertFalse(shortLived.validateToken(token));
    }

    @Test
    void validateToken_shouldReturnFalseForMalformedToken() {
        assertFalse(jwtUtil.validateToken("not.a.jwt"));
    }

    @Test
    void validateToken_shouldReturnFalseForTamperedToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId, UserType.FOLLOWER);

        String tampered = token.substring(0, token.lastIndexOf('.')) + ".tampered";

        assertFalse(jwtUtil.validateToken(tampered));
    }

    @Test
    void validateToken_shouldReturnFalseForEmptyString() {
        assertFalse(jwtUtil.validateToken(""));
    }

    @Test
    void validateToken_shouldReturnFalseForTokenSignedWithDifferentKey() throws Exception {
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "a-different-secret-key-that-is-long-enough-for-hmac!!".getBytes(StandardCharsets.UTF_8));
        UUID userId = UUID.randomUUID();

        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("userType", "FOLLOWER")
                .issuedAt(new java.util.Date())
                .expiration(new java.util.Date(System.currentTimeMillis() + 900000))
                .signWith(otherKey)
                .compact();

        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    void tokensForDifferentUsers_shouldHaveDifferentSubjects() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        String tokenA = jwtUtil.generateAccessToken(userA, UserType.INFLUENCER);
        String tokenB = jwtUtil.generateAccessToken(userB, UserType.FOLLOWER);

        assertEquals(userA, jwtUtil.getUserIdFromToken(tokenA));
        assertEquals(userB, jwtUtil.getUserIdFromToken(tokenB));
    }

    @Test
    void tokensForDifferentRoles_shouldHaveDifferentTypeClaims() {
        UUID userId = UUID.randomUUID();

        String influencerToken = jwtUtil.generateAccessToken(userId, UserType.INFLUENCER);
        String followerToken = jwtUtil.generateAccessToken(userId, UserType.FOLLOWER);

        assertEquals(UserType.INFLUENCER, jwtUtil.getUserTypeFromToken(influencerToken));
        assertEquals(UserType.FOLLOWER, jwtUtil.getUserTypeFromToken(followerToken));
    }
}
