package com.pnpn.shared.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class PnPnExceptionTest {

    @Test
    void testDefaultConstructor() {
        PnPnException exception = new PnPnException("Default error");
        assertEquals("Default error", exception.getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
    }

    @Test
    void testStatusConstructor() {
        PnPnException exception = new PnPnException("Bad Request error", HttpStatus.BAD_REQUEST);
        assertEquals("Bad Request error", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void testCauseConstructor() {
        RuntimeException cause = new RuntimeException("Root cause");
        PnPnException exception = new PnPnException("Error with cause", cause);
        assertEquals("Error with cause", exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
    }

    @Test
    void testCauseAndStatusConstructor() {
        RuntimeException cause = new RuntimeException("Root cause");
        PnPnException exception = new PnPnException("Conflict error", cause, HttpStatus.CONFLICT);
        assertEquals("Conflict error", exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }
}
