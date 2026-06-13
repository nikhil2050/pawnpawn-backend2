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

/**
 * JPA entity mapping the "users" table.
 * <p>
 * Stores all user profile data including authentication credentials, profile
 * media, verification documents, reputation scores, and engagement metrics.
 * The table is pre-created by {@code init.sql} on first PostgreSQL startup;
 * Hibernate's {@code ddl-auto: update} ensures the schema stays in sync.
 * <p>
 * The {@link Builder} pattern is used to construct instances cleanly,
 * especially during registration flows and test setup.
 *
 * @see com.pnpn.userservice.repository.UserRepository
 * @see com.pnpn.shared.dto.UserDto
 */
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

    /**
     * Account type ({@link UserType}) — determines platform role & permissions.
     * Stored as a string column via {@link EnumType#STRING}.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserType type;

    /** Display name shown on profiles and throughout the marketplace. */
    @Column(nullable = false)
    private String name;

    /**
     * Login identifier — unique, used for authentication and notifications.
     * Indexed via {@code idx_users_email} with a uniqueness constraint.
     */
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Optional phone number for OTP-based login and SMS notifications.
     * Unique when provided.
     */
    @Column(unique = true)
    private String phone;

    /** BCrypt hash of the user's password — never stored in plain text. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** Public URL to the user's profile picture (hosted on MinIO). */
    @Column(name = "profile_pic_url")
    private String profilePicUrl;

    /** Short biography displayed on the user's public profile. */
    private String bio;

    /**
     * Whether the user's identity has been verified.
     * Toggled to {@code true} once {@link VerificationStatus#VERIFIED} is set
     * after manual or automated KYC review.
     */
    @Column(nullable = false)
    private Boolean verified;

    /**
     * Current KYC/verification stage for influencer accounts.
     * Tracks the full lifecycle: UNVERIFIED → PENDING → VERIFIED / REJECTED.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private VerificationStatus verificationStatus;

    /** S3/MinIO URL of the government-issued ID document submitted for KYC. */
    @Column(name = "government_id_url")
    private String governmentIdUrl;

    /** S3/MinIO URL of the user's selfie submitted during KYC. */
    @Column(name = "selfie_url")
    private String selfieUrl;

    /** Number of followers for influencer accounts. */
    @Column(name = "follower_count", nullable = false)
    private Integer followerCount;

    /**
     * Average review rating (0.00 – 5.00) across all received reviews.
     * Precision 5, scale 2 ensures values like 4.85 are stored exactly.
     */
    @Column(name = "reputation_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal reputationScore;

    /** Total number of reviews received (used alongside reputation_score). */
    @Column(name = "total_reviews", nullable = false)
    private Integer totalReviews;

    /** Firebase Cloud Messaging token for push notifications. */
    @Column(name = "fcm_token")
    private String fcmToken;

    /** Timestamp set automatically by Hibernate on first persist — never updated after. */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Timestamp updated automatically by Hibernate on every entity modification. */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
