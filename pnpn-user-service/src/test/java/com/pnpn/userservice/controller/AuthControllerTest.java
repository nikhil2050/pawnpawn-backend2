package com.pnpn.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pnpn.shared.dto.auth.LoginRequest;
import com.pnpn.shared.dto.auth.LoginResponse;
import com.pnpn.shared.dto.auth.OtpRequest;
import com.pnpn.shared.dto.auth.OtpVerifyRequest;
import com.pnpn.shared.dto.auth.RegisterRequest;
import com.pnpn.shared.enums.UserType;
import com.pnpn.shared.exception.GlobalExceptionHandler;
import com.pnpn.shared.exception.PnPnException;
import com.pnpn.userservice.config.JwtUtil;
import com.pnpn.userservice.dto.LogoutRequest;
import com.pnpn.userservice.dto.RefreshTokenRequest;
import com.pnpn.userservice.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtUtil jwtUtil;

    private final UUID userId = UUID.randomUUID();
    private final String accessToken = "jwt.token.here";

    // ═══════════════════════════════════════════════════════════════════
    // POST /api/auth/register
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void register_shouldReturn201() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alice").email("alice@example.com").password("Pass1234!").type(UserType.INFLUENCER).build();

        LoginResponse response = LoginResponse.builder()
                .userId(userId).type(UserType.INFLUENCER).accessToken(accessToken).expiresIn(900000L).build();

        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.accessToken").value(accessToken));
    }

    @Test
    void register_shouldReturn400WhenValidationFails() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("").email("bad-email").password("short").type(null).build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shouldReturn409WhenEmailExists() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alice").email("alice@example.com").password("Pass1234!").type(UserType.INFLUENCER).build();

        when(authService.register(any())).thenThrow(new PnPnException("Email already registered", HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    // ═══════════════════════════════════════════════════════════════════
    // POST /api/auth/login
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void login_shouldReturn200() throws Exception {
        LoginRequest request = LoginRequest.builder().email("alice@example.com").password("Pass1234!").build();

        LoginResponse response = LoginResponse.builder()
                .userId(userId).type(UserType.INFLUENCER).accessToken(accessToken).expiresIn(900000L).build();

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(accessToken));
    }

    @Test
    void login_shouldReturn401WhenInvalid() throws Exception {
        LoginRequest request = LoginRequest.builder().email("alice@example.com").password("WrongPass1!").build();

        when(authService.login(any())).thenThrow(new PnPnException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void login_shouldReturn400WhenValidationFails() throws Exception {
        LoginRequest request = LoginRequest.builder().email("").password("").build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ═══════════════════════════════════════════════════════════════════
    // POST /api/auth/send-otp
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void sendOtp_shouldReturn200() throws Exception {
        OtpRequest request = OtpRequest.builder().email("alice@example.com").build();

        when(authService.sendOtp(any())).thenReturn("1234");

        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP sent successfully"))
                .andExpect(jsonPath("$.code").value("1234"));
    }

    @Test
    void sendOtp_shouldReturn404WhenUserNotFound() throws Exception {
        OtpRequest request = OtpRequest.builder().email("unknown@example.com").build();

        when(authService.sendOtp(any())).thenThrow(new PnPnException("User not found", HttpStatus.NOT_FOUND));

        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void sendOtp_shouldReturn400WhenEmailMissing() throws Exception {
        OtpRequest request = OtpRequest.builder().email("").build();

        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ═══════════════════════════════════════════════════════════════════
    // POST /api/auth/verify-otp
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void verifyOtp_shouldReturn200() throws Exception {
        OtpVerifyRequest request = OtpVerifyRequest.builder().email("alice@example.com").code("1234").build();

        doNothing().when(authService).verifyOtp(any());

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP verified successfully"));
    }

    @Test
    void verifyOtp_shouldReturn401WhenExpired() throws Exception {
        OtpVerifyRequest request = OtpVerifyRequest.builder().email("alice@example.com").code("1234").build();

        doThrow(new PnPnException("OTP expired or not requested", HttpStatus.UNAUTHORIZED))
                .when(authService).verifyOtp(any());

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("OTP expired or not requested"));
    }

    @Test
    void verifyOtp_shouldReturn400WhenCodeNot4Digits() throws Exception {
        OtpVerifyRequest request = OtpVerifyRequest.builder().email("alice@example.com").code("12").build();

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ═══════════════════════════════════════════════════════════════════
    // POST /api/auth/refresh
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void refresh_shouldReturn200() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .userId(userId).refreshToken(UUID.randomUUID().toString()).build();

        LoginResponse response = LoginResponse.builder()
                .userId(userId).accessToken("new-token").expiresIn(900000L).build();

        when(authService.refreshToken(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-token"));
    }

    @Test
    void refresh_shouldReturn401WhenInvalid() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .userId(userId).refreshToken("bad-token").build();

        when(authService.refreshToken(any())).thenThrow(
                new PnPnException("Invalid or expired refresh token", HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));
    }

    @Test
    void refresh_shouldReturn400WhenValidationFails() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder().userId(null).refreshToken(null).build();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ═══════════════════════════════════════════════════════════════════
    // POST /api/auth/logout
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void logout_shouldReturn200() throws Exception {
        LogoutRequest request = LogoutRequest.builder()
                .userId(userId).refreshToken(UUID.randomUUID().toString()).build();

        doNothing().when(authService).logout(any());

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }
}
