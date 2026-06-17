package com.pnpn.userservice.service;

import com.pnpn.shared.dto.UpdateProfileRequest;
import com.pnpn.shared.dto.UserDto;
import com.pnpn.shared.exception.PnPnException;
import com.pnpn.userservice.entity.User;
import com.pnpn.userservice.repository.UserRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @CircuitBreaker(name = "user-service-cb", fallbackMethod = "getUserByIdFallback")
    public UserDto getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new PnPnException("User not found", HttpStatus.NOT_FOUND));
        return toUserDto(user);
    }

    @CircuitBreaker(name = "user-service-cb", fallbackMethod = "updateProfileFallback")
    public UserDto updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new PnPnException("User not found", HttpStatus.NOT_FOUND));

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getProfilePicUrl() != null) {
            user.setProfilePicUrl(request.getProfilePicUrl());
        }
        if (request.getVerificationStatus() != null) {
            user.setVerificationStatus(request.getVerificationStatus());
        }

        user = userRepository.save(user);
        log.info("Profile updated for user: {}", userId);
        return toUserDto(user);
    }

    @CircuitBreaker(name = "user-service-cb", fallbackMethod = "getReputationFallback")
    public UserDto getReputation(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new PnPnException("User not found", HttpStatus.NOT_FOUND));
        return toUserDto(user);
    }

    @CircuitBreaker(name = "user-service-cb", fallbackMethod = "getFollowerCountFallback")
    public int getFollowerCount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new PnPnException("User not found", HttpStatus.NOT_FOUND));
        return user.getFollowerCount();
    }

    /**
     * All Fallback methods for circuit breaker.
     * These will be called when the main method fails (e.g. due to database issues).
     */

    private UserDto getUserByIdFallback(UUID userId, Throwable t) {
        log.error("Fallback for getUserById({}): {}", userId, t.getMessage());
        throw new PnPnException("Service temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE);
    }

    private UserDto updateProfileFallback(UUID userId, UpdateProfileRequest request, Throwable t) {
        log.error("Fallback for updateProfile({}): {}", userId, t.getMessage());
        throw new PnPnException("Service temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE);
    }

    private UserDto getReputationFallback(UUID userId, Throwable t) {
        log.error("Fallback for getReputation({}): {}", userId, t.getMessage());
        throw new PnPnException("Service temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE);
    }

    private int getFollowerCountFallback(UUID userId, Throwable t) {
        log.error("Fallback for getFollowerCount({}): {}", userId, t.getMessage());
        throw new PnPnException("Service temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE);
    }

    private UserDto toUserDto(User user) {
        return UserDto.builder()
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
    }
}
