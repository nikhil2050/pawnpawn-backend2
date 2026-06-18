package com.pnpn.catalogservice.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * Configuration class for MinIO object storage integration.
 * <p>
 * This class serves as both a Spring {@link Configuration} and a
 * {@link ConfigurationProperties} binding for the <code>minio.*</code>
 * prefix in application properties. It performs the following responsibilities:
 * <ul>
 *   <li>Binds MinIO connection settings (URL, access key, secret key)
 *       and a map of logical bucket names to actual bucket names.</li>
 *   <li>Exposes a singleton {@link MinioClient} bean that the
 *       application uses to interact with the MinIO server.</li>
 *   <li>On startup, iterates over the configured buckets and auto-creates
 *      any that do not yet exist (see {@link #ensureBucketsExist()}).</li>
 * </ul>
 * <p>
 * <b>Usage in application.properties / application.yml:</b>
 * <pre>{@code
 *      minio.url=http://localhost:9000
 *      minio.access-key=minioadmin
 *      minio.secret-key=minioadmin
 *      minio.buckets.catalog-images=catalog-images
 *      minio.buckets.catalog-thumbnails=catalog-thumbnails
 * }</pre>
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "minio")
@Setter
@Getter
public class MinioConfig {

    /** MinIO server endpoint URL (e.g. http://localhost:9000). */
    private String url;

    /** MinIO access key for authentication. */
    private String accessKey;

    /** MinIO secret key for authentication. */
    private String secretKey;

    /**
     * Map of logical bucket names (keys) to actual MinIO bucket names (values).
     * <p>
     * Example entry: {@code "catalog-images" -> "catalog-images"}.
     * The keys let services refer to buckets by a logical name,
     * while the values are the real bucket names on the MinIO server.
     */
    private Map<String, String> buckets;

    /**
     * Injected {@link MinioClient} bean (created by {@link #minioClient()}).
     * Using the injected reference instead of calling the @Bean method directly
     * avoids the {@code AutoCloseable} resource-warning, as Spring manages the
     * bean lifecycle and closes it when the context shuts down.
     */
    private MinioClient minioClient;

    /**
     * Creates and exposes a {@link MinioClient} bean.
     * <p>
     * The client is configured using the bound {@code url},
     * {@code accessKey}, and {@code secretKey} properties.
     * Once created, any Spring component can inject this bean to
     * perform object-storage operations (put, get, delete, etc.).
     *
     * @return a ready-to-use {@link MinioClient} instance
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * Injects the singleton {@link MinioClient} bean back into this config
     * so that {@link #ensureBucketsExist()} can use it without calling
     * the {@code @Bean} method directly (which would trigger an
     * {@code AutoCloseable} resource-warning in static analysis).
     */
    @Autowired
    public void setMinioClient(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * Ensures all configured buckets exist on the MinIO server.
     * <p>
     * This method is invoked automatically after the bean's properties have been set
     * and the {@link MinioClient} bean can be constructed. For each entry in {@link #buckets}:
     * <ol>
     *   <li>Check whether the bucket already exists via {@link MinioClient#bucketExists}.</li>
     *   <li>If it does not exist, create it with {@link MinioClient#makeBucket}.</li>
     * </ol>
     * Failures for individual buckets are logged but do NOT prevent the application
     * from starting — the error is caught per bucket and processing continues.
     */
    @PostConstruct
    public void ensureBucketsExist() {
        for (Map.Entry<String, String> entry : buckets.entrySet()) {
            String bucketName = entry.getValue();
            try {
                boolean exists = minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket(bucketName).build());
                if (!exists) {
                    minioClient.makeBucket(
                            MakeBucketArgs.builder().bucket(bucketName).build());
                    log.info("Created MinIO bucket: {}", bucketName);
                } else {
                    log.debug("MinIO bucket already exists: {}", bucketName);
                }
            } catch (Exception e) {
                // Log and continue so one bad bucket doesn't block the others.
                log.error("Failed to verify/create MinIO bucket '{}': {}", bucketName, e.getMessage());
            }
        }
    }
}
