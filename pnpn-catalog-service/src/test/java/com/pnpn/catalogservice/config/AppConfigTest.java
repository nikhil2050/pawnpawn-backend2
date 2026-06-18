package com.pnpn.catalogservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

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
}
