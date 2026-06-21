package com.pnpn.catalogservice.config;

import io.minio.MinioClient;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MinioConfigTest {

    @Test
    void shouldCreateMinioClientBean() {
        MinioProperties props = new MinioProperties();
        props.setUrl("http://localhost:9000");
        props.setAccessKey("test-key");
        props.setSecretKey("test-secret");

        MinioConfig config = new MinioConfig();
        MinioClient client = config.minioClient(props);

        assertNotNull(client);
    }

    @Test
    void shouldBindProperties() {
        MinioProperties props = new MinioProperties();
        props.setUrl("http://localhost:9000");
        props.setAccessKey("test-key");
        props.setSecretKey("test-secret");

        Map<String, String> buckets = new LinkedHashMap<>();
        buckets.put("listings-images", "pnpn-listings-images");
        props.setBuckets(buckets);

        assertEquals("http://localhost:9000", props.getUrl());
        assertEquals("test-key", props.getAccessKey());
        assertEquals("test-secret", props.getSecretKey());
        assertEquals(1, props.getBuckets().size());
    }
}
