package com.pnpn.userservice.config;

import com.pnpn.shared.enums.UserType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JwtAuthenticationFilter}.
 * Uses Mockito to simulate HTTP requests and verify SecurityContext behavior.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtUtil jwtUtil;
    private JwtAuthenticationFilter filter;

    private static final String SECRET = "test-secret-key-for-unit-testing-only-32-char!!";
    private static final long EXPIRATION = 900000;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION);
        filter = new JwtAuthenticationFilter(jwtUtil);
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSetAuthenticationForValidToken() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId, UserType.INFLUENCER);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(userId, auth.getPrincipal());
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_INFLUENCER")));

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSetFollowerRoleForFollowerToken() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId, UserType.FOLLOWER);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_FOLLOWER")));
    }

    @Test
    void shouldNotSetAuthenticationWhenNoToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotSetAuthenticationWhenNotBearerToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic somebase64==");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotSetAuthenticationForExpiredToken() throws Exception {
        JwtUtil shortLived = new JwtUtil(SECRET, -1);
        JwtAuthenticationFilter expiredFilter = new JwtAuthenticationFilter(shortLived);

        UUID userId = UUID.randomUUID();
        String token = shortLived.generateAccessToken(userId, UserType.FOLLOWER);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        expiredFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotSetAuthenticationForMalformedToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer not-a-valid-jwt");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldClearContextOnException() throws Exception {
        JwtUtil brokenJwt = mock(JwtUtil.class);
        JwtAuthenticationFilter brokenFilter = new JwtAuthenticationFilter(brokenJwt);

        when(request.getHeader("Authorization")).thenReturn("Bearer some.token.here");
        when(brokenJwt.validateToken(anyString())).thenReturn(true);
        when(brokenJwt.getUserIdFromToken(anyString())).thenThrow(new RuntimeException("parse error"));

        SecurityContextHolder.getContext().setAuthentication(mock(Authentication.class));
        brokenFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldContinueChainRegardlessOfAuth() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid.token.here");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }
}
