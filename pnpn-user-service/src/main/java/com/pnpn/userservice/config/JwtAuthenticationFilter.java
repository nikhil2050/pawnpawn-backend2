package com.pnpn.userservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Filter that intercepts every request and authenticates the user via a JWT
 * Bearer token in the {@code Authorization} header.
 * <p>
 * Extends {@link OncePerRequestFilter} to guarantee a single execution per
 * request dispatch (even though the container already guarantees this for
 * {@code Filter} implementations, it's best practice for portability).
 * <p>
 * Flow:
 * <ol>
 *   <li>Extract the {@code Authorization: Bearer &lt;token&gt;} header</li>
 *   <li>If present, validate the token via {@link JwtUtil#validateToken}</li>
 *   <li>If valid, extract {@code userId} (UUID) and {@code userType} (role)</li>
 *   <li>Create a {@link UsernamePasswordAuthenticationToken} with the
 *       userId as principal and {@code ROLE_INFLUENCER / ROLE_FOLLOWER}
 *       as the granted authority</li>
 *   <li>Set the authentication on {@link SecurityContextHolder} so that
 *       downstream code (controllers, services, {@code @PreAuthorize})
 *       can access the current user</li>
 *   <li>If the token is missing or invalid, the request continues without
 *       authentication — {@link SecurityConfig} will reject unauthenticated
 *       requests to secured paths</li>
 * </ol>
 *
 * @see SecurityConfig
 * @see JwtUtil
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    /**
     * Filters every incoming HTTP request. Attempts to authenticate the caller
     * from the JWT; if authentication succeeds, sets the Spring Security
     * context so the rest of the application can access the current user.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            try {
                UUID userId = jwtUtil.getUserIdFromToken(token);
                String userType = jwtUtil.getUserTypeFromToken(token).name();

                // Map the userType to a Spring Security granted authority
                // e.g. "INFLUENCER" -> "ROLE_INFLUENCER"
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + userType));
                var authentication = new UsernamePasswordAuthenticationToken(
                        userId, null, authorities);
                authentication.setDetails(userType);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                log.error("Failed to set authentication: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the Bearer token from the Authorization header.
     * Expects the standard format: {@code Authorization: Bearer <token>}.
     *
     * @param request the incoming HTTP request
     * @return the token string, or {@code null} if the header is missing or
     *         does not start with "Bearer "
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
