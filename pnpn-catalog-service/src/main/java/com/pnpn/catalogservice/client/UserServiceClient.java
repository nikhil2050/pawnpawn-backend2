package com.pnpn.catalogservice.client;

import com.pnpn.shared.dto.UserDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/**
 * HTTP client that calls pnpn-user-service to fetch user/influencer details.
 * <p>
 * The target URL is configurable via {@code services.user-service.url}
 * (defaults to {@code http://pnpn-user-service} — the Docker Compose service name).
 * <p>
 * The {@link #getUserById(UUID)} method is protected by a Resilience4j circuit breaker
 * ({@code name = "user-service-cb"}) configured in {@code application.yml}:
 * <ul>
 *   <li>Sliding window: 10 calls</li>
 *   <li>Failure threshold: 50%</li>
 *   <li>Wait duration in OPEN state: 10s</li>
 * </ul>
 * When the circuit is OPEN or the call throws, {@link #getUserFallback} returns a
 * minimal {@link UserDto} with name "Unavailable" so the caller can continue gracefully.
 */
@Slf4j
@Service
public class UserServiceClient {

    private final RestTemplate restTemplate;
    private final String userServiceUrl;

    public UserServiceClient(RestTemplate restTemplate,
                             @Value("${services.user-service.url:http://pnpn-user-service}") String userServiceUrl) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
    }

    @CircuitBreaker(name = "user-service-cb", fallbackMethod = "getUserFallback")
    public UserDto getUserById(UUID userId) {
        return restTemplate.getForObject(
                userServiceUrl + "/api/users/{id}",
                UserDto.class,
                userId);
    }

    public UserDto getUserFallback(UUID userId, Throwable t) {
        log.warn("Fallback: failed to fetch user {} from user-service: {}", userId, t.getMessage());
        return UserDto.builder()
                .id(userId)
                .name("Unavailable")
                .build();
    }
}
