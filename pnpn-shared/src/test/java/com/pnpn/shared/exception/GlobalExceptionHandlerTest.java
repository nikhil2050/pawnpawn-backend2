package com.pnpn.shared.exception;

import com.pnpn.shared.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private GlobalExceptionHandler handler;

    private MethodParameter validMethodParameter;

    @BeforeEach
    void setUp() throws Exception {
        Method method = GlobalExceptionHandlerTest.class.getMethod("dummyHandlerMethod", List.class);
        validMethodParameter = new MethodParameter(method, 0);
    }

    @SuppressWarnings("unused")
    public void dummyHandlerMethod(List<String> items) {
    }

    @Test
    void testHandlePnPnException() {
        when(request.getRequestURI()).thenReturn("/api/test");

        PnPnException ex = new PnPnException("Test error", HttpStatus.BAD_REQUEST);
        ResponseEntity<ErrorResponse> response = handler.handlePnPnException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.getStatus());
        assertEquals("Bad Request", body.getError());
        assertEquals("Test error", body.getMessage());
        assertEquals("/api/test", body.getPath());
        assertNull(body.getDetails());
    }

    @Test
    void testHandlePnPnExceptionWithDefaultStatus() {
        when(request.getRequestURI()).thenReturn("/api/test");

        PnPnException ex = new PnPnException("Default error");
        ResponseEntity<ErrorResponse> response = handler.handlePnPnException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Default error", response.getBody().getMessage());
    }

    @Test
    void testHandlePnPnExceptionWithCause() {
        when(request.getRequestURI()).thenReturn("/api/test");

        RuntimeException cause = new RuntimeException("cause");
        PnPnException ex = new PnPnException("Error with cause", cause);
        ResponseEntity<ErrorResponse> response = handler.handlePnPnException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Error with cause", response.getBody().getMessage());
    }

    @Test
    void testHandleValidationException() {
        when(request.getRequestURI()).thenReturn("/api/validate");

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
            new FieldError("obj", "email", "must be a valid email"),
            new FieldError("obj", "password", "must not be blank")
        ));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(validMethodParameter, bindingResult);
        ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.getStatus());
        assertEquals("Bad Request", body.getError());
        assertEquals("Validation failed", body.getMessage());
        assertEquals("/api/validate", body.getPath());
        assertNotNull(body.getDetails());
        assertEquals(2, body.getDetails().size());
        assertTrue(body.getDetails().get(0).contains("email"));
        assertTrue(body.getDetails().get(1).contains("password"));
    }

    @Test
    void testHandleValidationExceptionWithNoErrors() {
        when(request.getRequestURI()).thenReturn("/api/validate");

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(validMethodParameter, bindingResult);
        ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getDetails().isEmpty());
    }

    @Test
    void testHandleGenericException() {
        when(request.getRequestURI()).thenReturn("/api/unknown");

        Exception ex = new RuntimeException("Unexpected failure");
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(500, body.getStatus());
        assertEquals("Internal Server Error", body.getError());
        assertEquals("An unexpected error occurred", body.getMessage());
        assertEquals("/api/unknown", body.getPath());
        assertNull(body.getDetails());
    }

    @Test
    void testHandleGenericExceptionWithNestedCause() {
        when(request.getRequestURI()).thenReturn("/api/unknown");

        Exception cause = new IllegalArgumentException("Root cause");
        Exception ex = new RuntimeException("Wrapper", cause);
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
    }

    @Test
    void testResponseBodyTimestampIsNotNull() {
        when(request.getRequestURI()).thenReturn("/api/test");

        PnPnException ex = new PnPnException("Error");
        ResponseEntity<ErrorResponse> response = handler.handlePnPnException(ex, request);

        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void testAllHandlersReturnErrorResponseType() {
        when(request.getRequestURI()).thenReturn("/api/test");

        ResponseEntity<ErrorResponse> pnpnResp = handler.handlePnPnException(
            new PnPnException("msg"), request);
        assertInstanceOf(ErrorResponse.class, pnpnResp.getBody());

        BindingResult br = mock(BindingResult.class);
        when(br.getFieldErrors()).thenReturn(List.of());
        ResponseEntity<ErrorResponse> validResp = handler.handleValidationException(
            new MethodArgumentNotValidException(validMethodParameter, br), request);
        assertInstanceOf(ErrorResponse.class, validResp.getBody());

        ResponseEntity<ErrorResponse> genericResp = handler.handleGenericException(
            new Exception("msg"), request);
        assertInstanceOf(ErrorResponse.class, genericResp.getBody());
    }
}
