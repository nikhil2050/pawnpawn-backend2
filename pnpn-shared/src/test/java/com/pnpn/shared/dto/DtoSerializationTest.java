package com.pnpn.shared.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pnpn.shared.enums.UserType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DtoSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void testUserDtoSerialization() throws IOException {
        UserDto userDto = UserDto.builder()
                .id(UUID.randomUUID())
                .name("Jane Doe")
                .email("jane@example.com")
                .type(UserType.INFLUENCER)
                .reputationScore(BigDecimal.valueOf(4.85))
                .createdAt(Instant.now())
                .build();

        String json = objectMapper.writeValueAsString(userDto);
        assertNotNull(json);

        UserDto deserialized = objectMapper.readValue(json, UserDto.class);
        assertEquals(userDto.getId(), deserialized.getId());
        assertEquals(userDto.getName(), deserialized.getName());
        assertEquals(userDto.getEmail(), deserialized.getEmail());
        assertEquals(userDto.getType(), deserialized.getType());
        assertEquals(userDto.getReputationScore(), deserialized.getReputationScore());
        assertEquals(userDto.getCreatedAt(), deserialized.getCreatedAt());
    }
}
