package com.pnpn.shared.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Test
    void testBuilderAndGetters() {
        Instant now = Instant.now();
        ErrorResponse response = ErrorResponse.builder()
            .timestamp(now)
            .status(400)
            .error("Bad Request")
            .message("Invalid input")
            .path("/api/test")
            .details(List.of("field1: must not be blank", "field2: invalid format"))
            .build();

        assertEquals(now, response.getTimestamp());
        assertEquals(400, response.getStatus());
        assertEquals("Bad Request", response.getError());
        assertEquals("Invalid input", response.getMessage());
        assertEquals("/api/test", response.getPath());
        assertEquals(2, response.getDetails().size());
    }

    @Test
    void testNoArgsConstructor() {
        ErrorResponse response = new ErrorResponse();
        assertNull(response.getTimestamp());
        assertEquals(0, response.getStatus());
        assertNull(response.getError());
        assertNull(response.getMessage());
        assertNull(response.getPath());
        assertNull(response.getDetails());
    }

    @Test
    void testAllArgsConstructor() {
        Instant now = Instant.now();
        ErrorResponse response = new ErrorResponse(
            now, 500, "Server Error", "Something went wrong",
            "/api/error", List.of("detail1")
        );

        assertEquals(now, response.getTimestamp());
        assertEquals(500, response.getStatus());
        assertEquals("Server Error", response.getError());
        assertEquals("Something went wrong", response.getMessage());
        assertEquals("/api/error", response.getPath());
        assertEquals(1, response.getDetails().size());
    }

    @Test
    void testJsonSerialization() throws Exception {
        ErrorResponse response = ErrorResponse.builder()
            .timestamp(Instant.parse("2025-01-01T00:00:00Z"))
            .status(400)
            .error("Bad Request")
            .message("Validation failed")
            .path("/api/test")
            .details(List.of("field: error"))
            .build();

        String json = objectMapper.writeValueAsString(response);
        assertTrue(json.contains("\"timestamp\""));
        assertTrue(json.contains("\"status\":400"));
        assertTrue(json.contains("\"error\":\"Bad Request\""));
        assertTrue(json.contains("\"message\":\"Validation failed\""));
        assertTrue(json.contains("\"path\":\"/api/test\""));
        assertTrue(json.contains("\"details\""));
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"timestamp\":\"2025-01-01T00:00:00Z\",\"status\":400,\"error\":\"Bad Request\","
            + "\"message\":\"Invalid\",\"path\":\"/api/test\",\"details\":[\"err1\"]}";

        ErrorResponse response = objectMapper.readValue(json, ErrorResponse.class);
        assertEquals(400, response.getStatus());
        assertEquals("Bad Request", response.getError());
        assertEquals("Invalid", response.getMessage());
        assertEquals("/api/test", response.getPath());
        assertEquals(1, response.getDetails().size());
        assertEquals("err1", response.getDetails().get(0));
    }

    @Test
    void testJsonIncludeNonNull() throws Exception {
        ErrorResponse response = ErrorResponse.builder()
            .timestamp(Instant.parse("2025-01-01T00:00:00Z"))
            .status(400)
            .error("Bad Request")
            .message("Error")
            .path("/api/test")
            .build();

        String json = objectMapper.writeValueAsString(response);
        assertFalse(json.contains("\"details\""), "details should be omitted when null");
    }

    @Test
    void testSettersAndToString() {
        ErrorResponse response = new ErrorResponse();
        Instant now = Instant.now();

        response.setTimestamp(now);
        response.setStatus(404);
        response.setError("Not Found");
        response.setMessage("Resource not found");
        response.setPath("/api/resource");
        response.setDetails(List.of("detail"));

        assertEquals(now, response.getTimestamp());
        assertEquals(404, response.getStatus());
        assertEquals("Not Found", response.getError());
        assertEquals("Resource not found", response.getMessage());
        assertEquals("/api/resource", response.getPath());
        assertEquals(1, response.getDetails().size());

        String str = response.toString();
        assertTrue(str.contains("404"));
        assertTrue(str.contains("Not Found"));
    }

    @Test
    void testEqualsAndHashCode() {
        Instant now = Instant.parse("2025-01-01T00:00:00Z");
        ErrorResponse r1 = ErrorResponse.builder()
            .timestamp(now).status(400).error("Err").message("Msg").path("/p").build();
        ErrorResponse r2 = ErrorResponse.builder()
            .timestamp(now).status(400).error("Err").message("Msg").path("/p").build();

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void testCanEqual() {
        Instant now = Instant.parse("2025-01-01T00:00:00Z");
        ErrorResponse r1 = ErrorResponse.builder()
            .timestamp(now).status(400).error("Err").message("Msg").path("/p").build();
        ErrorResponse r2 = ErrorResponse.builder()
            .timestamp(now).status(500).error("Err").message("Msg").path("/p").build();

        assertNotEquals(r1, r2);
    }
}
