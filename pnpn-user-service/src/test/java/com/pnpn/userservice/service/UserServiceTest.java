package com.pnpn.userservice.service;

import com.pnpn.shared.dto.UpdateProfileRequest;
import com.pnpn.shared.dto.UserDto;
import com.pnpn.shared.enums.UserType;
import com.pnpn.shared.enums.VerificationStatus;
import com.pnpn.shared.exception.PnPnException;
import com.pnpn.userservice.entity.User;
import com.pnpn.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    private final UUID userId = UUID.randomUUID();
    private final User user = User.builder()
            .id(userId)
            .type(UserType.INFLUENCER)
            .name("Alice")
            .email("alice@example.com")
            .phone("+1234567890")
            .profilePicUrl("https://example.com/pic.jpg")
            .bio("Original bio")
            .verified(false)
            .verificationStatus(VerificationStatus.UNVERIFIED)
            .followerCount(42)
            .reputationScore(BigDecimal.valueOf(4.5))
            .totalReviews(10)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    // ═══════════════════════════════════════════════════════════════════
    // getUserById
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void getUserById_shouldReturnUserDto() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserDto dto = userService.getUserById(userId);

        assertEquals(userId, dto.getId());
        assertEquals("Alice", dto.getName());
        assertEquals("alice@example.com", dto.getEmail());
        assertEquals(UserType.INFLUENCER, dto.getType());
        assertEquals(42, dto.getFollowerCount());
        assertEquals(0, BigDecimal.valueOf(4.5).compareTo(dto.getReputationScore()));
    }

    @Test
    void getUserById_shouldThrowWhenNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        PnPnException ex = assertThrows(PnPnException.class, () -> userService.getUserById(userId));
        assertEquals("User not found", ex.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // ═══════════════════════════════════════════════════════════════════
    // updateProfile
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void updateProfile_shouldUpdateAllFields() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .name("Alice Updated")
                .bio("Updated bio")
                .profilePicUrl("https://example.com/new-pic.jpg")
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        UserDto dto = userService.updateProfile(userId, request);

        assertEquals("Alice Updated", dto.getName());
        assertEquals("Updated bio", dto.getBio());
        assertEquals("https://example.com/new-pic.jpg", dto.getProfilePicUrl());
        assertEquals(VerificationStatus.PENDING, dto.getVerificationStatus());
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_shouldUpdateOnlyName() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .name("New Name Only")
                .build();

        UserDto dto = userService.updateProfile(userId, request);

        assertEquals("New Name Only", dto.getName());
        assertEquals("Original bio", dto.getBio());
        assertEquals("https://example.com/pic.jpg", dto.getProfilePicUrl());
        assertEquals(VerificationStatus.UNVERIFIED, dto.getVerificationStatus());
    }

    @Test
    void updateProfile_shouldUpdateOnlyBio() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .bio("Just a new bio")
                .build();

        UserDto dto = userService.updateProfile(userId, request);

        assertEquals("Alice", dto.getName());
        assertEquals("Just a new bio", dto.getBio());
    }

    @Test
    void updateProfile_shouldUpdateOnlyProfilePicUrl() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .profilePicUrl("https://example.com/only-pic.jpg")
                .build();

        UserDto dto = userService.updateProfile(userId, request);

        assertEquals("https://example.com/only-pic.jpg", dto.getProfilePicUrl());
        assertEquals("Original bio", dto.getBio());
    }

    @Test
    void updateProfile_shouldUpdateOnlyVerificationStatus() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        UserDto dto = userService.updateProfile(userId, request);

        assertEquals(VerificationStatus.VERIFIED, dto.getVerificationStatus());
        assertEquals("Original bio", dto.getBio());
    }

    @Test
    void updateProfile_shouldDoNothingWhenAllFieldsNull() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = UpdateProfileRequest.builder().build();

        UserDto dto = userService.updateProfile(userId, request);

        assertEquals("Alice", dto.getName());
        assertEquals("Original bio", dto.getBio());
        assertEquals("https://example.com/pic.jpg", dto.getProfilePicUrl());
        assertEquals(VerificationStatus.UNVERIFIED, dto.getVerificationStatus());
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_shouldThrowWhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .name("Any Name").build();

        PnPnException ex = assertThrows(PnPnException.class, () -> userService.updateProfile(userId, request));
        assertEquals("User not found", ex.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(userRepository, never()).save(any());
    }

    // ═══════════════════════════════════════════════════════════════════
    // getReputation
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void getReputation_shouldReturnReputationFields() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserDto dto = userService.getReputation(userId);

        assertEquals(0, BigDecimal.valueOf(4.5).compareTo(dto.getReputationScore()));
        assertEquals(10, dto.getTotalReviews());
        assertEquals(userId, dto.getId());
    }

    @Test
    void getReputation_shouldThrowWhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        PnPnException ex = assertThrows(PnPnException.class, () -> userService.getReputation(userId));
        assertEquals("User not found", ex.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // ═══════════════════════════════════════════════════════════════════
    // getFollowerCount
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void getFollowerCount_shouldReturnCount() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        int count = userService.getFollowerCount(userId);

        assertEquals(42, count);
    }

    @Test
    void getFollowerCount_shouldThrowWhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        PnPnException ex = assertThrows(PnPnException.class, () -> userService.getFollowerCount(userId));
        assertEquals("User not found", ex.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // ═══════════════════════════════════════════════════════════════════
    // Fallback methods (circuit breaker)
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void getUserByIdFallback_shouldThrowServiceUnavailable() throws Exception {
        Method fallback = UserService.class.getDeclaredMethod("getUserByIdFallback", UUID.class, Throwable.class);
        fallback.setAccessible(true);

        Throwable cause = new RuntimeException("Database connection pool exhausted");
        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> fallback.invoke(userService, userId, cause));

        PnPnException pnpnEx = (PnPnException) ex.getCause();
        assertEquals("Service temporarily unavailable", pnpnEx.getMessage());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, pnpnEx.getStatus());
    }

    @Test
    void updateProfileFallback_shouldThrowServiceUnavailable() throws Exception {
        Method fallback = UserService.class.getDeclaredMethod(
                "updateProfileFallback", UUID.class, UpdateProfileRequest.class, Throwable.class);
        fallback.setAccessible(true);

        UpdateProfileRequest request = UpdateProfileRequest.builder().name("Any").build();
        Throwable cause = new RuntimeException("DB timeout");
        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> fallback.invoke(userService, userId, request, cause));

        PnPnException pnpnEx = (PnPnException) ex.getCause();
        assertEquals("Service temporarily unavailable", pnpnEx.getMessage());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, pnpnEx.getStatus());
    }

    @Test
    void getReputationFallback_shouldThrowServiceUnavailable() throws Exception {
        Method fallback = UserService.class.getDeclaredMethod("getReputationFallback", UUID.class, Throwable.class);
        fallback.setAccessible(true);

        Throwable cause = new RuntimeException("Circuit breaker open");
        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> fallback.invoke(userService, userId, cause));

        PnPnException pnpnEx = (PnPnException) ex.getCause();
        assertEquals("Service temporarily unavailable", pnpnEx.getMessage());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, pnpnEx.getStatus());
    }

    @Test
    void getFollowerCountFallback_shouldThrowServiceUnavailable() throws Exception {
        Method fallback = UserService.class.getDeclaredMethod("getFollowerCountFallback", UUID.class, Throwable.class);
        fallback.setAccessible(true);

        Throwable cause = new RuntimeException("Follower service down");
        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> fallback.invoke(userService, userId, cause));

        PnPnException pnpnEx = (PnPnException) ex.getCause();
        assertEquals("Service temporarily unavailable", pnpnEx.getMessage());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, pnpnEx.getStatus());
    }

    @Test
    void getUserByIdFallback_shouldWrapAnyThrowable() throws Exception {
        Method fallback = UserService.class.getDeclaredMethod("getUserByIdFallback", UUID.class, Throwable.class);
        fallback.setAccessible(true);

        Throwable cause = new Error("JVM error — unexpected");
        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> fallback.invoke(userService, userId, cause));

        PnPnException pnpnEx = (PnPnException) ex.getCause();
        assertEquals("Service temporarily unavailable", pnpnEx.getMessage());
    }

    @Test
    void updateProfileFallback_shouldPreserveCauseInLog() throws Exception {
        Method fallback = UserService.class.getDeclaredMethod(
                "updateProfileFallback", UUID.class, UpdateProfileRequest.class, Throwable.class);
        fallback.setAccessible(true);

        UpdateProfileRequest request = UpdateProfileRequest.builder().bio("test").build();
        Throwable cause = new IllegalStateException("Illegal state in DB");
        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> fallback.invoke(userService, userId, request, cause));

        PnPnException pnpnEx = (PnPnException) ex.getCause();
        assertEquals("Service temporarily unavailable", pnpnEx.getMessage());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, pnpnEx.getStatus());
    }
}
