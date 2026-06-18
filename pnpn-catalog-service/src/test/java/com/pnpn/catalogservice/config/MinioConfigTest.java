package com.pnpn.catalogservice.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinioConfigTest {

    @Mock
    private MinioClient minioClient;

    private MinioConfig minioConfig;

    @BeforeEach
    void setUp() {
        minioConfig = new MinioConfig();
        minioConfig.setUrl("http://localhost:9000");
        minioConfig.setAccessKey("test-key");
        minioConfig.setSecretKey("test-secret");

        Map<String, String> buckets = new LinkedHashMap<>();
        buckets.put("listings-images", "pnpn-listings-images");
        buckets.put("listings-videos", "pnpn-listings-videos");
        buckets.put("reviews-images", "pnpn-reviews-images");
        buckets.put("avatars", "pnpn-avatars");
        minioConfig.setBuckets(buckets);

        // Inject the mock MinioClient so ensureBucketsExist() uses it
        minioConfig.setMinioClient(minioClient);
    }

    @Test
    void shouldBindProperties() {
        assertEquals("http://localhost:9000", minioConfig.getUrl());
        assertEquals("test-key", minioConfig.getAccessKey());
        assertEquals("test-secret", minioConfig.getSecretKey());
        assertEquals(4, minioConfig.getBuckets().size());
    }

    @Test
    void shouldCreateMinioClientBean() {
        MinioClient client = minioConfig.minioClient();

        assertNotNull(client);
    }

    @Test
    void shouldCreateMissingBucketsOnStartup() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
                .thenReturn(false);

        minioConfig.ensureBucketsExist();

        verify(minioClient, times(4)).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient, times(4)).makeBucket(any(MakeBucketArgs.class));

        ArgumentCaptor<MakeBucketArgs> captor = ArgumentCaptor.forClass(MakeBucketArgs.class);
        verify(minioClient, times(4)).makeBucket(captor.capture());

        assertEquals("pnpn-listings-images", captor.getAllValues().get(0).bucket());
        assertEquals("pnpn-listings-videos", captor.getAllValues().get(1).bucket());
        assertEquals("pnpn-reviews-images", captor.getAllValues().get(2).bucket());
        assertEquals("pnpn-avatars", captor.getAllValues().get(3).bucket());
    }

    @Test
    void shouldNotCreateExistingBuckets() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
                .thenReturn(true);

        minioConfig.ensureBucketsExist();

        verify(minioClient, times(4)).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient, never()).makeBucket(any());
    }

    @Test
    void shouldHandleBucketExistsFailureGracefully() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
                .thenThrow(new RuntimeException("MinIO not reachable"));

        minioConfig.ensureBucketsExist();

        verify(minioClient, times(4)).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient, never()).makeBucket(any());
    }

    @Test
    void shouldHandleMakeBucketFailureGracefully() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
                .thenReturn(false);
        doThrow(new RuntimeException("Cannot create bucket"))
                .when(minioClient).makeBucket(any(MakeBucketArgs.class));

        minioConfig.ensureBucketsExist();

        verify(minioClient, times(4)).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient, times(4)).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void shouldHandlePartialBucketExistence() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
                .thenAnswer(invocation -> {
                    BucketExistsArgs args = invocation.getArgument(0);
                    return "pnpn-listings-images".equals(args.bucket());
                });

        minioConfig.ensureBucketsExist();

        verify(minioClient, times(4)).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient, times(3)).makeBucket(any(MakeBucketArgs.class));
    }
}
