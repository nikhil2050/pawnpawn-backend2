package com.pnpn.userservice.config;

import com.pnpn.shared.enums.UserType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Utility component for JWT creation, parsing, and validation.
 * <p>
 * Token structure (signed with HMAC-SHA256):
 * <pre>
 * Header:  { "alg": "HS256" }
 * Payload: {
 *   "sub":       "&lt;userId&gt;",          // UUID as string
 *   "userType":  "INFLUENCER",        // custom claim for role
 *   "iat":       &lt;issued-at-epoch&gt;,
 *   "exp":       &lt;expiry-epoch&gt;       // 15 min from iat
 * }
 * </pre>
 * <p>
 * The signing key is derived from the {@code jwt.secret} property using
 * HMAC-SHA256. In production this must be a strong, unique secret stored
 * securely (e.g., AWS Secrets Manager / environment variable).
 *
 * @see JwtAuthenticationFilter
 * @see SecurityConfig
 */
@Slf4j
@Component
public class JwtUtil {

    /** HMAC-SHA256 key derived from the configured secret string. */
    private final SecretKey secretKey;

    /** Access token TTL in milliseconds (default 900000 = 15 min). */
    private final long accessTokenExpiration;

    /**
     * Constructs a JwtUtil with the configured secret and expiration.
     * The secret is converted to an HMAC-SHA256 key via
     * {@link Keys#hmacShaKeyFor(byte[])} which derives a valid key
     * of at least 256 bits from the raw secret bytes.
     *
     * @param secret                the signing secret (from {@code jwt.secret})
     * @param accessTokenExpiration token lifetime in ms (from {@code jwt.access-token-expiration})
     */
    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
    }

    /**
     * Creates a signed JWT access token embedding the user's ID and role.
     * The token is self-contained — downstream services can extract the
     * userId and userType without querying the database.
     *
     * @param userId   the authenticated user's UUID (stored in {@code sub})
     * @param userType the user's role (stored in the {@code userType} claim)
     * @return a compact, URL-safe JWT string
     */
    public String generateAccessToken(UUID userId, UserType userType) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("userType", userType.name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpiration))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Parses a signed JWT and returns its claims payload.
     * Verification is performed against the configured secret key;
     * throws an exception if the signature is invalid or the token is expired.
     *
     * @param token the raw JWT string
     * @return the parsed {@link Claims} object
     * @throws io.jsonwebtoken.JwtException if parsing or verification fails
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts the user's UUID from the {@code sub} claim of a valid token.
     *
     * @param token a validated JWT string
     * @return the user's UUID
     */
    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(parseToken(token).getSubject());
    }

    /**
     * Extracts the user's role from the {@code userType} claim of a valid token.
     *
     * @param token a validated JWT string
     * @return the user's {@link UserType}
     */
    public UserType getUserTypeFromToken(String token) {
        return UserType.valueOf(parseToken(token).get("userType", String.class));
    }

    /**
     * Checks whether a token has expired without throwing.
     *
     * @param token a JWT string (may be expired)
     * @return {@code true} if the token's {@code exp} claim is in the past
     */
    public boolean isTokenExpired(String token) {
        try {
            parseToken(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Validates the token's signature and structure returning a boolean
     * rather than throwing. Logs a warning for each failure mode.
     * <p>
     * Failure modes handled:
     * <ul>
     *   <li><b>Expired</b> — token has passed its {@code exp} claim</li>
     *   <li><b>Malformed</b> — token does not have the expected format</li>
     *   <li><b>Unsupported</b> — token uses an unsupported algorithm</li>
     *   <li><b>Empty</b> — token string is blank</li>
     * </ul>
     *
     * @param token the raw JWT string to validate
     * @return {@code true} if the token is structurally and cryptographically valid
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("JWT signature does not match: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string empty: {}", e.getMessage());
        }
        return false;
    }
}
