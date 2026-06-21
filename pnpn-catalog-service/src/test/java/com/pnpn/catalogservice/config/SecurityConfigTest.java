package com.pnpn.catalogservice.config;

import com.pnpn.shared.config.JwtAuthenticationFilter;
import com.pnpn.shared.config.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTest.TestController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, SecurityConfigTest.TestController.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtil;

    @RestController
    static class TestController {
        @GetMapping("/api/listings")
        String publicMatch() { return "ok"; }

        @GetMapping("/api/listings/any-path")
        String publicMatchNested() { return "ok"; }

        @PostMapping("/api/listings")
        @PreAuthorize("hasRole('INFLUENCER')")
        String protectedByMethodSecurity() { return "ok"; }

        @GetMapping("/api/private")
        String requiresAuth() { return "ok"; }
    }

    @Test
    void publicGetEndpoints_shouldBeAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/listings")).andExpect(status().isOk());
    }

    @Test
    void publicGetNestedEndpoints_shouldBeAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/listings/any-path")).andExpect(status().isOk());
    }

    @Test
    void methodSecurity_shouldDenyUnauthenticatedPost() throws Exception {
        mockMvc.perform(post("/api/listings")).andExpect(status().isForbidden());
    }

    @Test
    void methodSecurity_shouldAllowAuthenticatedPost() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null,
                List.of(new SimpleGrantedAuthority("ROLE_INFLUENCER")));
        mockMvc.perform(post("/api/listings").with(authentication(auth)))
                .andExpect(status().isOk());
    }

    @Test
    void privateEndpoints_shouldReturn401WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/private")).andExpect(status().isUnauthorized());
    }
}
