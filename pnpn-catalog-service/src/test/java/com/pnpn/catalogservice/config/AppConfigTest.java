package com.pnpn.catalogservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppConfigTest {

    @Test
    void shouldCreateRestTemplateBean() {
        AppConfig appConfig = new AppConfig();
        RestTemplate restTemplate = appConfig.restTemplate();
        assertNotNull(restTemplate);
    }

    @Test
    void shouldCreateNewInstanceEachCall() {
        AppConfig appConfig = new AppConfig();
        RestTemplate first = appConfig.restTemplate();
        RestTemplate second = appConfig.restTemplate();
        assertNotNull(first);
        assertNotNull(second);
        assertNotSame(first, second);
    }

    @Test
    void shouldRegisterApiKeyInterceptor() {
        AppConfig appConfig = new AppConfig();
        ReflectionTestUtils.setField(appConfig, "userServiceApiKey", "test-api-key");

        RestTemplate restTemplate = appConfig.restTemplate();
        List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
        assertEquals(1, interceptors.size());
    }

    @Test
    void interceptor_shouldSetXInternalApiKeyHeader() throws IOException {
        String apiKey = "test-service-api-key";
        AppConfig appConfig = new AppConfig();
        ReflectionTestUtils.setField(appConfig, "userServiceApiKey", apiKey);

        RestTemplate restTemplate = appConfig.restTemplate();
        ClientHttpRequestInterceptor interceptor = restTemplate.getInterceptors().getFirst();

        HttpRequest request = mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);

        byte[] body = new byte[0];
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(execution.execute(request, body)).thenReturn(response);

        ClientHttpResponse result = interceptor.intercept(request, body, execution);

        assertEquals(apiKey, headers.getFirst("X-Internal-Api-Key"));
        assertSame(response, result);
        verify(execution).execute(request, body);
    }

    @Test
    void interceptor_shouldPreserveOtherHeaders() throws IOException {
        AppConfig appConfig = new AppConfig();
        ReflectionTestUtils.setField(appConfig, "userServiceApiKey", "key-123");

        RestTemplate restTemplate = appConfig.restTemplate();
        ClientHttpRequestInterceptor interceptor = restTemplate.getInterceptors().getFirst();

        HttpRequest request = mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        when(request.getHeaders()).thenReturn(headers);

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        interceptor.intercept(request, new byte[0], execution);

        assertEquals("application/json", headers.getFirst("Content-Type"));
        assertEquals("key-123", headers.getFirst("X-Internal-Api-Key"));
    }
}
