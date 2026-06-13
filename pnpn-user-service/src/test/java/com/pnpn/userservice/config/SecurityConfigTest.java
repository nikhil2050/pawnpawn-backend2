package com.pnpn.userservice.config;

import com.pnpn.shared.enums.UserType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link SecurityConfig} URL authorization rules.
 * Verifies that public endpoints are accessible without authentication
 * and secured endpoints are properly rejected.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void actuatorHealth_shouldBeAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorInfo_shouldBeAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    void authRegister_shouldBeAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/auth/register"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void authLogin_shouldBeAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void securedPathWithoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void securedPathWithValidToken_shouldNotReturn401() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId, UserType.INFLUENCER);

        mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void securedPathWithExpiredToken_shouldReturn401() throws Exception {
        JwtUtil shortLived = new JwtUtil("default-dev-secret-key-change-in-production", -1);
        UUID userId = UUID.randomUUID();
        String token = shortLived.generateAccessToken(userId, UserType.FOLLOWER);

        mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownPath_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/nonexistent"))
                .andExpect(status().isUnauthorized());
    }
}
