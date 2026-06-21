package com.pnpn.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pnpn.shared.dto.UpdateProfileRequest;
import com.pnpn.shared.dto.UserDto;
import com.pnpn.shared.enums.UserType;
import com.pnpn.shared.enums.VerificationStatus;
import com.pnpn.shared.exception.GlobalExceptionHandler;
import com.pnpn.shared.exception.PnPnException;
import com.pnpn.shared.config.JwtUtil;
import com.pnpn.userservice.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtil jwtUtil;

    private final UUID userId = UUID.randomUUID();
    private final UserDto userDto = UserDto.builder()
            .id(userId)
            .type(UserType.INFLUENCER)
            .name("Alice")
            .email("alice@example.com")
            .phone("+1234567890")
            .profilePicUrl("https://example.com/pic.jpg")
            .bio("A great influencer")
            .verified(false)
            .verificationStatus(VerificationStatus.UNVERIFIED)
            .followerCount(42)
            .reputationScore(BigDecimal.valueOf(4.5))
            .totalReviews(10)
            .build();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null,
                        List.of(new SimpleGrantedAuthority("ROLE_FOLLOWER"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ═══════════════════════════════════════════════════════════════════
    // GET /api/users/me
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void getCurrentUser_shouldReturn200() throws Exception {
        when(userService.getUserById(userId)).thenReturn(userDto);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.type").value("INFLUENCER"))
                .andExpect(jsonPath("$.followerCount").value(42));
    }

    @Test
    void getCurrentUser_shouldReturn401WhenAuthenticationIsNull() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));

        verifyNoInteractions(userService);
    }

    @Test
    void getCurrentUser_shouldReturn401WhenNotAuthenticated() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(null, null));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));

        verifyNoInteractions(userService);
    }

    // ═══════════════════════════════════════════════════════════════════
    // GET /api/users/{id}
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void getUserById_shouldReturn200() throws Exception {
        when(userService.getUserById(userId)).thenReturn(userDto);

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    void getUserById_shouldReturn404WhenNotFound() throws Exception {
        when(userService.getUserById(userId))
                .thenThrow(new PnPnException("User not found", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    // ═══════════════════════════════════════════════════════════════════
    // PUT /api/users/{id}/profile
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void updateProfile_shouldReturn200() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .name("Alice Updated")
                .bio("Updated bio")
                .profilePicUrl("https://example.com/new-pic.jpg")
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        UserDto updated = UserDto.builder()
                .id(userId)
                .type(UserType.INFLUENCER)
                .name("Alice Updated")
                .bio("Updated bio")
                .profilePicUrl("https://example.com/new-pic.jpg")
                .verificationStatus(VerificationStatus.PENDING)
                .followerCount(42)
                .reputationScore(BigDecimal.valueOf(4.5))
                .totalReviews(10)
                .build();

        when(userService.updateProfile(eq(userId), any())).thenReturn(updated);

        mockMvc.perform(put("/api/users/{id}/profile", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Updated"))
                .andExpect(jsonPath("$.bio").value("Updated bio"))
                .andExpect(jsonPath("$.profilePicUrl").value("https://example.com/new-pic.jpg"))
                .andExpect(jsonPath("$.verificationStatus").value("PENDING"));
    }

    @Test
    void updateProfile_shouldReturn401WhenAuthenticationIsNull() throws Exception {
        SecurityContextHolder.clearContext();

        UpdateProfileRequest request = UpdateProfileRequest.builder().name("Alice").build();

        mockMvc.perform(put("/api/users/{id}/profile", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));

        verifyNoInteractions(userService);
    }

    @Test
    void updateProfile_shouldReturn401WhenNotAuthenticated() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(null, null));

        UpdateProfileRequest request = UpdateProfileRequest.builder().name("Alice").build();

        mockMvc.perform(put("/api/users/{id}/profile", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));

        verifyNoInteractions(userService);
    }

    @Test
    void updateProfile_shouldReturn403WhenWrongUser() throws Exception {
        UUID otherId = UUID.randomUUID();
        UpdateProfileRequest request = UpdateProfileRequest.builder().name("Hacker").build();

        mockMvc.perform(put("/api/users/{id}/profile", otherId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only update your own profile"));

        verifyNoInteractions(userService);
    }

    @Test
    void updateProfile_shouldReturn404WhenUserNotFound() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder().name("Alice").build();

        when(userService.updateProfile(eq(userId), any()))
                .thenThrow(new PnPnException("User not found", HttpStatus.NOT_FOUND));

        mockMvc.perform(put("/api/users/{id}/profile", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    // ═══════════════════════════════════════════════════════════════════
    // GET /api/users/{id}/reputation
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void getReputation_shouldReturn200() throws Exception {
        when(userService.getReputation(userId)).thenReturn(userDto);

        mockMvc.perform(get("/api/users/{id}/reputation", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.reputationScore").value(4.5))
                .andExpect(jsonPath("$.totalReviews").value(10));
    }

    @Test
    void getReputation_shouldReturn404WhenNotFound() throws Exception {
        when(userService.getReputation(userId))
                .thenThrow(new PnPnException("User not found", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/users/{id}/reputation", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    // ═══════════════════════════════════════════════════════════════════
    // GET /api/users/{id}/followers/count
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void getFollowerCount_shouldReturn200() throws Exception {
        when(userService.getFollowerCount(userId)).thenReturn(42);

        mockMvc.perform(get("/api/users/{id}/followers/count", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.followerCount").value(42));
    }

    @Test
    void getFollowerCount_shouldReturn404WhenNotFound() throws Exception {
        when(userService.getFollowerCount(userId))
                .thenThrow(new PnPnException("User not found", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/users/{id}/followers/count", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }
}
