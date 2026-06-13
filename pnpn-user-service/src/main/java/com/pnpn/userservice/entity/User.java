package com.pnpn.userservice.entity;

import com.pnpn.shared.enums.UserType;
import com.pnpn.shared.enums.VerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email", unique = true),
    @Index(name = "idx_users_phone", columnList = "phone", unique = true),
    @Index(name = "idx_users_type", columnList = "type"),
    @Index(name = "idx_users_verified", columnList = "verified"),
    @Index(name = "idx_users_verification_status", columnList = "verification_status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserType type;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(unique = true)
    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "profile_pic_url")
    private String profilePicUrl;

    private String bio;

    @Column(nullable = false)
    private Boolean verified;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private VerificationStatus verificationStatus;

    @Column(name = "government_id_url")
    private String governmentIdUrl;

    @Column(name = "selfie_url")
    private String selfieUrl;

    @Column(name = "follower_count", nullable = false)
    private Integer followerCount;

    @Column(name = "reputation_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal reputationScore;

    @Column(name = "total_reviews", nullable = false)
    private Integer totalReviews;

    @Column(name = "fcm_token")
    private String fcmToken;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
