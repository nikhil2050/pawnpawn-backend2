package com.pnpn.userservice.controller;

import com.pnpn.shared.dto.UserDto;
import com.pnpn.shared.exception.PnPnException;
import com.pnpn.userservice.entity.User;
import com.pnpn.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class SampleUserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new PnPnException("User not found", HttpStatus.NOT_FOUND));

        UserDto dto = UserDto.builder()
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

        return ResponseEntity.ok(dto);
    }
}
