package com.pnpn.userservice.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.pnpn.shared.config.JwtAuthenticationFilter;

/**
 * Spring Security configuration for the user service.
 * <p>
 * Security model (stateless, JWT-based):
 * <ul>
 *   <li><b>CSRF disabled</b> — clients call via a mobile app / SPA using JWT,
 *       not browser-based form submissions</li>
 *   <li><b>Stateless sessions</b> — no {@code HttpSession} is created;
 *       every request is authenticated independently via the JWT</li>
 *   <li><b>Public endpoints</b> — {@code /api/auth/**} (register, login, OTP)
 *       and {@code /actuator/**} (health, info) are accessible without auth</li>
 *   <li><b>Authenticated endpoints</b> — everything else requires a valid JWT
 *       in the {@code Authorization: Bearer &lt;token&gt;} header</li>
 *   <li><b>401 on missing/expired token</b> — explicit
 *       {@link org.springframework.security.web.AuthenticationEntryPoint}
 *       configured so unauthenticated requests return 401 instead of 403</li>
 * </ul>
 *
 * @see JwtAuthenticationFilter
 * @see JwtUtil
 */
@Configuration
@EnableWebSecurity
/**
 * Enables method-level security annotations (@PreAuthorize, @Secured, @RolesAllowed) across the service.
 * <p>
 * While this service currently authorises requests via HttpSecurity request matchers,
 * future endpoints may use @PreAuthorize for fine-grained role checks.
 * This annotation ensures consistency with pnpn-catalog-service where role enforcement on write operations
 * is handled at the controller level.
 */
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${services.user-service.api-key}")
    private String userServiceApiKey;

    /**
     * Defines the security filter chain for all HTTP requests.
     * <p>
     * The custom {@link JwtAuthenticationFilter} is inserted <em>before</em>
     * Spring's {@link UsernamePasswordAuthenticationFilter} so that JWT
     * validation occurs before form-login or HTTP-Basic checks.
     *
     * @param http the {@link HttpSecurity} builder
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if the configuration is invalid
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")))
            .addFilterBefore(new InternalApiKeyFilter(userServiceApiKey), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Password encoder for hashing user passwords during registration
     * and verifying passwords during login. Uses BCrypt (default strength 10).
     *
     * @return a {@link BCryptPasswordEncoder} instance
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
