package com.pnpn.shared.dto;

import com.pnpn.shared.enums.UserType;
import com.pnpn.shared.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private UUID id;
    private UserType type;
    private String name;
    private String email;
    private String phone;
    private String profilePicUrl;
    private String bio;
    private boolean verified;
    private VerificationStatus verificationStatus;
    private String governmentIdUrl;
    private String selfieUrl;
    private int followerCount;
    private BigDecimal reputationScore;
    private int totalReviews;
    private String fcmToken;
    private Instant createdAt;
    private Instant updatedAt;
}
