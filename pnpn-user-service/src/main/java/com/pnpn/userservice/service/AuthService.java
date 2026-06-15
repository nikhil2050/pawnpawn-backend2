package com.pnpn.userservice.service;

import com.pnpn.shared.dto.UserDto;
import com.pnpn.shared.dto.auth.LoginRequest;
import com.pnpn.shared.dto.auth.LoginResponse;
import com.pnpn.shared.dto.auth.OtpRequest;
import com.pnpn.shared.dto.auth.OtpVerifyRequest;
import com.pnpn.shared.dto.auth.RegisterRequest;
import com.pnpn.shared.enums.VerificationStatus;
import com.pnpn.shared.exception.PnPnException;
import com.pnpn.userservice.config.JwtUtil;
import com.pnpn.userservice.dto.LogoutRequest;
import com.pnpn.userservice.dto.RefreshTokenRequest;
import com.pnpn.userservice.entity.User;
import com.pnpn.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    private static final String OTP_PREFIX = "otp:";
    private static final String REFRESH_PREFIX = "refresh:";
    private static final long OTP_TTL_MINUTES = 5;
    private static final long REFRESH_TTL_DAYS = 7;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Sign Up (Register) a new user with email + password.
     * Validates input, checks for duplicates, hashes the password, and returns JWT + refresh token on success.
     */
    public LoginResponse register(RegisterRequest request) {
        validatePassword(request.getPassword());

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new PnPnException("Email already registered", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .type(request.getType())        // Influencer or Follower or Admin
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))    // BCrypt hash of the user's password — never stored in plain text
                .verified(false)
                .verificationStatus(VerificationStatus.UNVERIFIED)
                .followerCount(0)
                .reputationScore(BigDecimal.ZERO)
                .totalReviews(0)
                .build();

        user = userRepository.save(user);
        log.info("User registered: {} ({})", user.getEmail(), user.getType());
        return buildLoginResponse(user);
    }

    /**
     * Login with email + password.
     * Validates credentials and returns JWT + refresh token.
     */
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new PnPnException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new PnPnException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        log.info("User logged in: {}", user.getEmail());
        return buildLoginResponse(user);
    }

    /**
     * Send a 4-digit OTP code to the user's email for passwordless login.
     */
    public String sendOtp(OtpRequest request) {
        userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new PnPnException("User not found", HttpStatus.NOT_FOUND));

        String code = String.format("%04d", RANDOM.nextInt(10000));
        String key = OTP_PREFIX + request.getEmail();

        redisTemplate.opsForValue().set(key, code, OTP_TTL_MINUTES, TimeUnit.MINUTES);
        log.info("OTP sent to {}: {} (expires in {} min)", request.getEmail(), code, OTP_TTL_MINUTES);

        return code;
    }

    /**
     * Verify the OTP code for the given email.
     * If valid, log the user in and return JWT + refresh token.
     */
    public void verifyOtp(OtpVerifyRequest request) {
        String key = OTP_PREFIX + request.getEmail();
        String stored = redisTemplate.opsForValue().get(key);

        if (stored == null) {
            throw new PnPnException("OTP expired or not requested", HttpStatus.UNAUTHORIZED);
        }

        if (!stored.equals(request.getCode())) {
            throw new PnPnException("Invalid OTP code", HttpStatus.UNAUTHORIZED);
        }

        redisTemplate.delete(key);
        log.info("OTP verified for {}", request.getEmail());
    }

    /**
     * Refresh the access token using a valid refresh token.
     * Issues a new JWT and refresh token pair.
     */
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        if (request.getRefreshToken() == null || request.getUserId() == null) {
            throw new PnPnException("Refresh token and userId are required", HttpStatus.BAD_REQUEST);
        }

        String key = REFRESH_PREFIX + request.getUserId();
        String stored = redisTemplate.opsForValue().get(key);

        if (stored == null || !stored.equals(request.getRefreshToken())) {
            throw new PnPnException("Invalid or expired refresh token", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new PnPnException("User not found", HttpStatus.NOT_FOUND));

        redisTemplate.delete(key);
        log.info("Token refreshed for user: {}", user.getEmail());
        return buildLoginResponse(user);
    }

    /**
     * Logout the user by invalidating the refresh token in Redis.
     */
    public void logout(LogoutRequest request) {
        if (request.getUserId() != null) {
            String key = REFRESH_PREFIX + request.getUserId();
            String stored = redisTemplate.opsForValue().get(key);

            if (stored != null && stored.equals(request.getRefreshToken())) {
                redisTemplate.delete(key);
                log.info("User logged out: {}", request.getUserId());
            }
        }
    }

    /**
     * Password validation rules:
     * - Minimum 8 characters
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one digit
     * - At least one special character
     */
    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new PnPnException("Password must be at least 8 characters", HttpStatus.BAD_REQUEST);
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new PnPnException("Password must contain at least one uppercase letter", HttpStatus.BAD_REQUEST);
        }
        if (!password.matches(".*[a-z].*")) {
            throw new PnPnException("Password must contain at least one lowercase letter", HttpStatus.BAD_REQUEST);
        }
        if (!password.matches(".*\\d.*")) {
            throw new PnPnException("Password must contain at least one digit", HttpStatus.BAD_REQUEST);
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new PnPnException("Password must contain at least one special character", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Shared Response Builder
     * Helper method to build a LoginResponse containing the JWT access token, refresh token, and user details.
     */
    private LoginResponse buildLoginResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getType());
        String refreshToken = UUID.randomUUID().toString();

        String key = REFRESH_PREFIX + user.getId();
        redisTemplate.opsForValue().set(key, refreshToken, REFRESH_TTL_DAYS, TimeUnit.DAYS);

        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .type(user.getType())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .profilePicUrl(user.getProfilePicUrl())
                .bio(user.getBio())
                .verified(user.getVerified())
                .verificationStatus(user.getVerificationStatus())
                .governmentIdUrl(user.getGovernmentIdUrl())
                .selfieUrl(user.getSelfieUrl())
                .followerCount(user.getFollowerCount())
                .reputationScore(user.getReputationScore())
                .totalReviews(user.getTotalReviews())
                .fcmToken(user.getFcmToken())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();

        return LoginResponse.builder()
                .userId(user.getId())
                .type(user.getType())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(900000L)
                .user(userDto)
                .build();
    }
}
