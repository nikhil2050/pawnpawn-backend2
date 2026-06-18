package com.pnpn.catalogservice.client;

import com.pnpn.shared.dto.UserDto;
import com.pnpn.shared.enums.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    private UserServiceClient userServiceClient;

    private final UUID userId = UUID.randomUUID();
    private final String userServiceUrl = "http://pnpn-user-service";

    @BeforeEach
    void setUp() {
        userServiceClient = new UserServiceClient(restTemplate, userServiceUrl);
    }

    // ═══════════════════════════════════════════════════════════════════
    // getUserById
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void getUserById_shouldReturnUserDto() {
        UserDto expected = UserDto.builder()
                .id(userId)
                .name("Alice")
                .email("alice@example.com")
                .type(UserType.INFLUENCER)
                .build();

        when(restTemplate.getForObject(
                userServiceUrl + "/api/users/{id}",
                UserDto.class,
                userId))
                .thenReturn(expected);

        UserDto actual = userServiceClient.getUserById(userId);

        assertNotNull(actual);
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getEmail(), actual.getEmail());
        verify(restTemplate).getForObject(
                userServiceUrl + "/api/users/{id}",
                UserDto.class,
                userId);
    }

    @Test
    void getUserById_shouldReturnNullWhenServiceReturnsNull() {
        when(restTemplate.getForObject(
                userServiceUrl + "/api/users/{id}",
                UserDto.class,
                userId))
                .thenReturn(null);

        UserDto actual = userServiceClient.getUserById(userId);

        assertNull(actual);
    }

    // ═══════════════════════════════════════════════════════════════════
    // getUserFallback (tested directly — @CircuitBreaker is an AOP aspect
    // applied only when the bean is managed by Spring)
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void getUserFallback_shouldReturnFallbackUserDto() {
        UUID id = UUID.randomUUID();
        Throwable cause = new RuntimeException("Service unavailable");

        UserDto fallback = userServiceClient.getUserFallback(id, cause);

        assertNotNull(fallback);
        assertEquals(id, fallback.getId());
        assertEquals("Unavailable", fallback.getName());
    }

    @Test
    void getUserFallback_shouldReturnFallbackForAnyUserId() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        UserDto fb1 = userServiceClient.getUserFallback(id1, new Exception());
        UserDto fb2 = userServiceClient.getUserFallback(id2, new Exception());

        assertEquals(id1, fb1.getId());
        assertEquals(id2, fb2.getId());
        assertEquals("Unavailable", fb1.getName());
        assertEquals("Unavailable", fb2.getName());
    }
}
