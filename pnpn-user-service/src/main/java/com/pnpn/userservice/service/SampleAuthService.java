package com.pnpn.userservice.service;

import com.pnpn.shared.dto.UserDto;
import com.pnpn.shared.dto.auth.LoginRequest;
import com.pnpn.shared.dto.auth.LoginResponse;
import com.pnpn.shared.dto.auth.RegisterRequest;
import com.pnpn.shared.enums.VerificationStatus;
import com.pnpn.shared.exception.PnPnException;
import com.pnpn.userservice.config.JwtUtil;
import com.pnpn.userservice.entity.User;
import com.pnpn.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class SampleAuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new PnPnException("Email already registered", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .type(request.getType())
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .verified(false)
                .verificationStatus(VerificationStatus.UNVERIFIED)
                .followerCount(0)
                .reputationScore(BigDecimal.ZERO)
                .totalReviews(0)
                .build();

        user = userRepository.save(user);
        return buildLoginResponse(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new PnPnException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new PnPnException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        return buildLoginResponse(user);
    }

    private LoginResponse buildLoginResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getType());

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
                .refreshToken("placeholder-refresh-token")
                .expiresIn(900000L)
                .user(userDto)
                .build();
    }
}
