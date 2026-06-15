package com.pnpn.userservice.service;

import com.pnpn.shared.dto.auth.LoginRequest;
import com.pnpn.shared.dto.auth.LoginResponse;
import com.pnpn.shared.dto.auth.OtpRequest;
import com.pnpn.shared.dto.auth.OtpVerifyRequest;
import com.pnpn.shared.dto.auth.RegisterRequest;
import com.pnpn.shared.enums.UserType;
import com.pnpn.shared.enums.VerificationStatus;
import com.pnpn.shared.exception.PnPnException;
import com.pnpn.userservice.config.JwtUtil;
import com.pnpn.shared.dto.auth.LogoutRequest;
import com.pnpn.shared.dto.auth.RefreshTokenRequest;

import com.pnpn.userservice.entity.User;
import com.pnpn.userservice.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private AuthService authService;

    private final UUID userId = UUID.randomUUID();
    private final String email = "alice@example.com";
    private final String password = "Pass1234!";
    private final String accessToken = "jwt.token.here";
    private final User user = User.builder()
            .id(userId)
            .type(UserType.INFLUENCER)
            .name("Alice")
            .email(email)
            .passwordHash("$2a$10$hashed")
            .verified(false)
            .verificationStatus(VerificationStatus.UNVERIFIED)
            .followerCount(0)
            .reputationScore(BigDecimal.ZERO)
            .totalReviews(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtUtil, redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ═══════════════════════════════════════════════════════════════════
    // register
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void register_shouldSucceed() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alice").email(email).password(password).type(UserType.INFLUENCER).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn("$2a$10$encoded");
        when(userRepository.save(any())).thenReturn(user);
        when(jwtUtil.generateAccessToken(userId, UserType.INFLUENCER)).thenReturn(accessToken);

        LoginResponse response = authService.register(request);

        assertEquals(userId, response.getUserId());
        assertEquals(UserType.INFLUENCER, response.getType());
        assertEquals(accessToken, response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        verify(userRepository).save(any());
    }

    @Test
    void register_shouldThrowWhenEmailExists() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alice").email(email).password(password).type(UserType.INFLUENCER).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        PnPnException ex = assertThrows(PnPnException.class, () -> authService.register(request));
        assertEquals("Email already registered", ex.getMessage());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Ab1!",           // < 8 chars
            "nouppercase1!",  // no uppercase
            "NOLOWERCASE1!",  // no lowercase
            "NoSpecialChar1", // no special char
            "NoDigitHere@!",  // no digit
    })
    void register_shouldRejectWeakPasswords(String weakPassword) {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alice").email(email).password(weakPassword).type(UserType.INFLUENCER).build();

        assertThrows(PnPnException.class, () -> authService.register(request));
        verifyNoInteractions(userRepository);
    }

    // ═══════════════════════════════════════════════════════════════════
    // login
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void login_shouldSucceed() {
        LoginRequest request = LoginRequest.builder().email(email).password(password).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, user.getPasswordHash())).thenReturn(true);
        when(jwtUtil.generateAccessToken(userId, UserType.INFLUENCER)).thenReturn(accessToken);

        LoginResponse response = authService.login(request);

        assertEquals(userId, response.getUserId());
        assertEquals(accessToken, response.getAccessToken());
    }

    @Test
    void login_shouldThrowWhenEmailNotFound() {
        LoginRequest request = LoginRequest.builder().email("unknown@example.com").password(password).build();

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        PnPnException ex = assertThrows(PnPnException.class, () -> authService.login(request));
        assertEquals("Invalid email or password", ex.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void login_shouldThrowWhenWrongPassword() {
        LoginRequest request = LoginRequest.builder().email(email).password("WrongPass1!").build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass1!", user.getPasswordHash())).thenReturn(false);

        PnPnException ex = assertThrows(PnPnException.class, () -> authService.login(request));
        assertEquals("Invalid email or password", ex.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    // ═══════════════════════════════════════════════════════════════════
    // sendOtp
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void sendOtp_shouldStoreCodeInRedis() {
        OtpRequest request = OtpRequest.builder().email(email).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        String code = authService.sendOtp(request);

        assertNotNull(code);
        assertEquals(4, code.length());
        assertTrue(code.matches("\\d{4}"));

        verify(valueOps).set(eq("otp:" + email), eq(code), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    void sendOtp_shouldThrowWhenUserNotFound() {
        OtpRequest request = OtpRequest.builder().email("unknown@example.com").build();

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        PnPnException ex = assertThrows(PnPnException.class, () -> authService.sendOtp(request));
        assertEquals("User not found", ex.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // ═══════════════════════════════════════════════════════════════════
    // verifyOtp
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void verifyOtp_shouldSucceed() {
        OtpVerifyRequest request = OtpVerifyRequest.builder().email(email).code("1234").build();

        when(valueOps.get("otp:" + email)).thenReturn("1234");

        assertDoesNotThrow(() -> authService.verifyOtp(request));
        verify(valueOps).get("otp:" + email);
    }

    @Test
    void verifyOtp_shouldThrowWhenExpired() {
        OtpVerifyRequest request = OtpVerifyRequest.builder().email(email).code("1234").build();

        when(valueOps.get("otp:" + email)).thenReturn(null);

        PnPnException ex = assertThrows(PnPnException.class, () -> authService.verifyOtp(request));
        assertEquals("OTP expired or not requested", ex.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void verifyOtp_shouldThrowWhenWrongCode() {
        OtpVerifyRequest request = OtpVerifyRequest.builder().email(email).code("9999").build();

        when(valueOps.get("otp:" + email)).thenReturn("1234");

        PnPnException ex = assertThrows(PnPnException.class, () -> authService.verifyOtp(request));
        assertEquals("Invalid OTP code", ex.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    // ═══════════════════════════════════════════════════════════════════
    // refreshToken
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void refreshToken_shouldSucceed() {
        String refreshToken = UUID.randomUUID().toString();
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .userId(userId).refreshToken(refreshToken).build();

        when(valueOps.get("refresh:" + userId)).thenReturn(refreshToken);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken(userId, UserType.INFLUENCER)).thenReturn(accessToken);

        LoginResponse response = authService.refreshToken(request);

        assertEquals(accessToken, response.getAccessToken());
        assertNotNull(response.getRefreshToken());
    }

    @Test
    void refreshToken_shouldThrowWhenMissingFields() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .userId(null).refreshToken(null).build();

        PnPnException ex = assertThrows(PnPnException.class, () -> authService.refreshToken(request));
        assertEquals("Refresh token and userId are required", ex.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void refreshToken_shouldThrowWhenTokenNotFound() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .userId(userId).refreshToken("invalid-token").build();

        when(valueOps.get("refresh:" + userId)).thenReturn(null);

        PnPnException ex = assertThrows(PnPnException.class, () -> authService.refreshToken(request));
        assertEquals("Invalid or expired refresh token", ex.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void refreshToken_shouldThrowWhenTokenMismatch() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .userId(userId).refreshToken("wrong-token").build();

        when(valueOps.get("refresh:" + userId)).thenReturn("stored-token");

        PnPnException ex = assertThrows(PnPnException.class, () -> authService.refreshToken(request));
        assertEquals("Invalid or expired refresh token", ex.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void refreshToken_shouldThrowWhenUserNotFound() {
        String refreshToken = UUID.randomUUID().toString();
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .userId(userId).refreshToken(refreshToken).build();

        when(valueOps.get("refresh:" + userId)).thenReturn(refreshToken);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        PnPnException ex = assertThrows(PnPnException.class, () -> authService.refreshToken(request));
        assertEquals("User not found", ex.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // ═══════════════════════════════════════════════════════════════════
    // logout
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void logout_shouldDeleteRefreshToken() {
        String refreshToken = UUID.randomUUID().toString();
        LogoutRequest request = LogoutRequest.builder()
                .userId(userId).refreshToken(refreshToken).build();

        when(valueOps.get("refresh:" + userId)).thenReturn(refreshToken);

        authService.logout(request);

        verify(redisTemplate).delete("refresh:" + userId);
    }

    @Test
    void logout_shouldDoNothingWhenUserIdIsNull() {
        LogoutRequest request = LogoutRequest.builder().userId(null).refreshToken("some-token").build();

        authService.logout(request);

        verifyNoInteractions(valueOps);
    }

    @Test
    void logout_shouldDoNothingWhenRefreshTokenMissing() {
        LogoutRequest request = LogoutRequest.builder()
                .userId(userId).refreshToken("some-token").build();

        when(valueOps.get("refresh:" + userId)).thenReturn(null);

        authService.logout(request);

        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void logout_shouldDoNothingWhenTokenMismatch() {
        LogoutRequest request = LogoutRequest.builder()
                .userId(userId).refreshToken("wrong-token").build();

        when(valueOps.get("refresh:" + userId)).thenReturn("stored-token");

        authService.logout(request);

        verify(redisTemplate, never()).delete(anyString());
    }

    // ═══════════════════════════════════════════════════════════════════
    // Password validation (additional edge cases)
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void register_shouldRejectNullPassword() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alice").email(email).password(null).type(UserType.INFLUENCER).build();

        PnPnException ex = assertThrows(PnPnException.class, () -> authService.register(request));
        assertEquals("Password must be at least 8 characters", ex.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }
}
