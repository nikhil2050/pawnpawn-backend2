# MinIO → AWS S3 Migration Guide

## Table of Contents

1. [Overview](#overview)
2. [Option A — Configuration-Only (MinIO SDK → AWS S3 endpoint)](#option-a--configuration-only-minio-sdk--aws-s3-endpoint)
3. [Option B — Full AWS SDK v1 Migration](#option-b--full-aws-sdk-v1-migration)
4. [Comparison & Recommendation](#comparison--recommendation)
5. [Appendix — Files Touched Per Option](#appendix--files-touched-per-option)

---

## Overview

The catalog service currently uses `MinioClient` (from `io.minio:minio`) to:

1. **Create buckets** on startup (`MinioInitializer.java`)
2. **Generate presigned upload URLs** (`ListingService.java:generatePresignedUrl`)

Two approaches exist for switching to AWS S3 as the object store. They are **not mutually exclusive** — try Option A first; if any MinIO‑specific API behaviour causes issues, fall back to Option B.

---

## Option A — Configuration-Only (MinIO SDK → AWS S3 endpoint)

### How it works

The `io.minio:minio` Java SDK implements the S3‑compatible API. You can point it at any S3‑compliant endpoint, including `s3.amazonaws.com`. **No Java code changes required** — only properties change.

### What changes

#### 1. `pnpn-catalog-service/src/main/resources/application.yml`

```yaml
minio:
  url: https://s3.${AWS_REGION:us-east-1}.amazonaws.com
  access-key: ${AWS_ACCESS_KEY_ID}
  secret-key: ${AWS_SECRET_ACCESS_KEY}
  buckets:
    listings-images: pnpn-prod-listings-images    # must be globally unique on S3
    listings-videos: pnpn-prod-listings-videos
    reviews-images: pnpn-prod-reviews-images
    avatars: pnpn-prod-avatars
```

> **⚠ WARNING:** S3 bucket names must be **globally unique** across all AWS accounts. Rename them from `pnpn-listings-images` to something like `pnpn-{env}-listings-images` (e.g. `pnpn-prod-listings-images`, `pnpn-staging-listings-images`).

#### 2. `pnpn-catalog-service/src/test/resources/application.yml`

```yaml
minio:
  url: https://s3.us-east-1.amazonaws.com
  access-key: test
  secret-key: test
  buckets:
    listings-images: pnpn-test-listings-images
    listings-videos: pnpn-test-listings-videos
    reviews-images: pnpn-test-reviews-images
    avatars: pnpn-test-avatars
```

#### 3. Environment variables

| Old variable       | New variable            | Example value                              |
|--------------------|-------------------------|--------------------------------------------|
| `MINIO_HOST`       | *(removed)*             | —                                          |
| `MINIO_ACCESS_KEY` | `AWS_ACCESS_KEY_ID`     | `AKIAIOSFODNN7EXAMPLE`                     |
| `MINIO_SECRET_KEY` | `AWS_SECRET_ACCESS_KEY` | `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY` |
| *(new)*            | `AWS_REGION`            | `us-east-1`                                |

### Risk assessment

| Operation                         | Risk      | Reason                                                                                                                                                       |
|-----------------------------------|-----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `bucketExists()` / `makeBucket()` | 🟢 Low    | Standard S3 API calls — MinIO and AWS behave identically                                                                                                     |
| `getPresignedObjectUrl()`         | 🟡 Medium | MinIO's presigned URL generation may produce slightly different expiry semantics or URL format than native AWS. Test with a real S3 bucket before deploying. |

### Rollback

Restore the original `application.yml` values. Zero code changes mean zero risk.

---

## Option B — Full AWS SDK v1 Migration

If Option A produces incompatible presigned URLs, replace the MinIO SDK with the AWS SDK v1 for S3 (`com.amazonaws:aws-java-sdk-s3`).

### Step-by-step code changes

---

### Step 1 — Update POMs

#### 1a. `pom.xml` (root — `<dependencyManagement>`)

Add the AWS S3 dependency version:

```xml
<properties>
    <aws-sdk-s3.version>1.12.765</aws-sdk-s3.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- remove the minio block entirely -->
        <!--
        <dependency>
            <groupId>io.minio</groupId>
            <artifactId>minio</artifactId>
            <version>${minio.version}</version>
        </dependency>
        -->

        <dependency>
            <groupId>com.amazonaws</groupId>
            <artifactId>aws-java-sdk-s3</artifactId>
            <version>${aws-sdk-s3.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

#### 1b. `pnpn-catalog-service/pom.xml`

```xml
<dependencies>
    <!-- remove -->
    <!--
    <dependency>
        <groupId>io.minio</groupId>
        <artifactId>minio</artifactId>
    </dependency>
    -->

    <!-- add -->
    <dependency>
        <groupId>com.amazonaws</groupId>
        <artifactId>aws-java-sdk-s3</artifactId>
    </dependency>
</dependencies>
```

---

### Step 2 — Replace `MinioProperties.java` → `S3Properties.java`

**New file:** `pnpn-catalog-service/src/main/java/com/pnpn/catalogservice/config/S3Properties.java`

```java
package com.pnpn.catalogservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "s3")
@Setter
@Getter
public class S3Properties {

    private String url;

    private String region;

    private String accessKey;

    private String secretKey;

    private Map<String, String> buckets;
}
```

---

### Step 3 — Replace `MinioConfig.java` → `S3Config.java`

**New file:** `pnpn-catalog-service/src/main/java/com/pnpn/catalogservice/config/S3Config.java`

```java
package com.pnpn.catalogservice.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

    @Bean
    public AmazonS3 amazonS3(S3Properties s3Properties) {
        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                s3Properties.getUrl(), s3Properties.getRegion()))
                .withCredentials(new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials(
                                s3Properties.getAccessKey(), s3Properties.getSecretKey())))
                .withPathStyleAccessEnabled(true)
                .build();
    }
}
```

> **Note:** `withPathStyleAccessEnabled(true)` ensures bucket names appear in the URL path (`/bucket/key`) rather than as a subdomain (`bucket.s3.amazonaws.com/key`). This is required for MinIO compatibility and also works with AWS S3.

---

### Step 4 — Replace `MinioInitializer.java` → `S3Initializer.java`

**New file:** `pnpn-catalog-service/src/main/java/com/pnpn/catalogservice/config/S3Initializer.java`

```java
package com.pnpn.catalogservice.config;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3Initializer {

    private final AmazonS3 amazonS3;
    private final S3Properties s3Properties;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureBucketsExist() {
        for (Map.Entry<String, String> entry : s3Properties.getBuckets().entrySet()) {
            String bucketName = entry.getValue();
            try {
                if (!amazonS3.doesBucketExistV2(bucketName)) {
                    amazonS3.createBucket(new CreateBucketRequest(bucketName));
                    log.info("Created S3 bucket: {}", bucketName);
                } else {
                    log.debug("S3 bucket already exists: {}", bucketName);
                }
            } catch (Exception e) {
                log.error("Failed to verify/create S3 bucket '{}': {}",
                        bucketName, e.getMessage());
            }
        }
    }
}
```

---

### Step 5 — Update `ListingService.java`

#### 5a. Change the field type and imports

```java
// remove these imports
// import io.minio.GetPresignedObjectUrlArgs;
// import io.minio.MinioClient;
// import io.minio.http.Method;

// add these imports
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;

// change field
// private final MinioClient minioClient;
private final AmazonS3 amazonS3;
```

#### 5b. Update `generatePresignedUrl()` method

```java
public MediaPresignedUrlResponse generatePresignedUrl(String bucketName, String fileName, UUID userId) {
    String objectName = userId + "/" + UUID.randomUUID() + "_" + fileName;

    try {
        Date expiration = new Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1));

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, objectName)
                .withMethod(HttpMethod.PUT)
                .withExpiration(expiration);

        String presignedUrl = amazonS3.generatePresignedUrl(request).toString();

        String mediaUrl = "/media/" + bucketName + "/" + objectName;

        return MediaPresignedUrlResponse.builder()
                .presignedUrl(presignedUrl)
                .mediaUrl(mediaUrl)
                .build();
    } catch (Exception e) {
        log.error("Failed to generate presigned URL for bucket: {}, object: {}",
                bucketName, objectName, e);
        throw new PnPnException("Failed to generate upload URL", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

#### 5c. Update the constructor

If using `@RequiredArgsConstructor`, rename the field from `minioClient` to `amazonS3` — the annotation generates a constructor parameter with the same name automatically.

---

### Step 6 — Update `application.yml` (config prefix change)

```yaml
# remove the minio block
# minio:
#   url: ...
#   access-key: ...

# add the s3 block
s3:
  url: https://s3.${AWS_REGION:us-east-1}.amazonaws.com
  region: ${AWS_REGION:us-east-1}
  access-key: ${AWS_ACCESS_KEY_ID}
  secret-key: ${AWS_SECRET_ACCESS_KEY}
  buckets:
    listings-images: pnpn-prod-listings-images
    listings-videos: pnpn-prod-listings-videos
    reviews-images: pnpn-prod-reviews-images
    avatars: pnpn-prod-avatars
```

Do the same in `pnpn-catalog-service/src/test/resources/application.yml`.

---

### Step 7 — Update tests

#### 7a. `MinioConfigTest.java` → `S3ConfigTest.java`

```java
package com.pnpn.catalogservice.config;

import com.amazonaws.services.s3.AmazonS3;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class S3ConfigTest {

    @Test
    void shouldCreateAmazonS3Bean() {
        S3Properties props = new S3Properties();
        props.setUrl("http://localhost:9000");
        props.setRegion("us-east-1");
        props.setAccessKey("test-key");
        props.setSecretKey("test-secret");

        S3Config config = new S3Config();
        AmazonS3 client = config.amazonS3(props);

        assertNotNull(client);
    }

    @Test
    void shouldBindProperties() {
        S3Properties props = new S3Properties();
        props.setUrl("http://localhost:9000");
        props.setRegion("us-east-1");
        props.setAccessKey("test-key");
        props.setSecretKey("test-secret");

        Map<String, String> buckets = new LinkedHashMap<>();
        buckets.put("listings-images", "s3-test-listings-images");
        props.setBuckets(buckets);

        assertEquals("http://localhost:9000", props.getUrl());
        assertEquals("us-east-1", props.getRegion());
        assertEquals("test-key", props.getAccessKey());
        assertEquals("test-secret", props.getSecretKey());
        assertEquals(1, props.getBuckets().size());
    }
}
```

#### 7b. `MinioInitializerTest.java` → `S3InitializerTest.java`

```java
package com.pnpn.catalogservice.config;

import com.amazonaws.services.s3.AmazonS3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.amazonaws.services.s3.model.CreateBucketRequest;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3InitializerTest {

    @Mock
    private AmazonS3 amazonS3;

    private S3Properties s3Properties;
    private S3Initializer s3Initializer;

    @BeforeEach
    void setUp() {
        s3Properties = new S3Properties();
        s3Properties.setUrl("http://localhost:9000");
        s3Properties.setRegion("us-east-1");
        s3Properties.setAccessKey("test-key");
        s3Properties.setSecretKey("test-secret");

        Map<String, String> buckets = new LinkedHashMap<>();
        buckets.put("listings-images", "s3-listings-images");
        buckets.put("listings-videos", "s3-listings-videos");
        buckets.put("reviews-images", "s3-reviews-images");
        buckets.put("avatars", "s3-avatars");
        s3Properties.setBuckets(buckets);

        s3Initializer = new S3Initializer(amazonS3, s3Properties);
    }

    @Test
    void shouldCreateMissingBucketsOnStartup() {
        when(amazonS3.doesBucketExistV2(anyString())).thenReturn(false);

        s3Initializer.ensureBucketsExist();

        verify(amazonS3, times(4)).doesBucketExistV2(anyString());
        verify(amazonS3, times(4)).createBucket(any(CreateBucketRequest.class));

        ArgumentCaptor<CreateBucketRequest> captor =
                ArgumentCaptor.forClass(CreateBucketRequest.class);
        verify(amazonS3, times(4)).createBucket(captor.capture());

        assertEquals("s3-listings-images", captor.getAllValues().get(0).getBucketName());
        assertEquals("s3-listings-videos", captor.getAllValues().get(1).getBucketName());
        assertEquals("s3-reviews-images", captor.getAllValues().get(2).getBucketName());
        assertEquals("s3-avatars", captor.getAllValues().get(3).getBucketName());
    }

    @Test
    void shouldNotCreateExistingBuckets() {
        when(amazonS3.doesBucketExistV2(anyString())).thenReturn(true);

        s3Initializer.ensureBucketsExist();

        verify(amazonS3, times(4)).doesBucketExistV2(anyString());
        verify(amazonS3, never()).createBucket(any());
    }

    @Test
    void shouldHandleBucketExistsFailureGracefully() {
        when(amazonS3.doesBucketExistV2(anyString()))
                .thenThrow(new RuntimeException("S3 not reachable"));

        s3Initializer.ensureBucketsExist();

        verify(amazonS3, times(4)).doesBucketExistV2(anyString());
        verify(amazonS3, never()).createBucket(any());
    }

    @Test
    void shouldHandleCreateBucketFailureGracefully() {
        when(amazonS3.doesBucketExistV2(anyString())).thenReturn(false);
        doThrow(new RuntimeException("Cannot create bucket"))
                .when(amazonS3).createBucket(any(CreateBucketRequest.class));

        s3Initializer.ensureBucketsExist();

        verify(amazonS3, times(4)).doesBucketExistV2(anyString());
        verify(amazonS3, times(4)).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void shouldHandlePartialBucketExistence() {
        when(amazonS3.doesBucketExistV2(anyString()))
                .thenAnswer(invocation -> {
                    String bucket = invocation.getArgument(0);
                    return "s3-listings-images".equals(bucket);
                });

        s3Initializer.ensureBucketsExist();

        verify(amazonS3, times(4)).doesBucketExistV2(anyString());
        verify(amazonS3, times(3)).createBucket(any(CreateBucketRequest.class));
    }
}
```

#### 7c. `ListingServiceTest.java`

Change three things:

| Before                                                                                                         | After                                                                                                       |
|----------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------|
| `import io.minio.MinioClient;`                                                                                 | `import com.amazonaws.services.s3.AmazonS3;`                                                                |
| `import io.minio.GetPresignedObjectUrlArgs;`                                                                   | `import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;`                                       |
| `@Mock private MinioClient minioClient;`                                                                       | `@Mock private AmazonS3 amazonS3;`                                                                          |
| `listingService = new ListingService(listingRepository, userServiceClient, minioClient, stringRedisTemplate);` | `listingService = new ListingService(listingRepository, userServiceClient, amazonS3, stringRedisTemplate);` |

Update the `generatePresignedUrl_shouldReturnUrl` test:

```java
@Test
void generatePresignedUrl_shouldReturnUrl() throws Exception {
    String presignedUrl = "https://s3.amazonaws.com/upload-url";
    String fileName = "photo.jpg";

    when(amazonS3.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
            .thenReturn(new java.net.URL(presignedUrl));

    MediaPresignedUrlResponse response = listingService.generatePresignedUrl(
            bucketName, fileName, influencerId);

    assertNotNull(response);
    assertEquals(presignedUrl, response.getPresignedUrl());
    assertTrue(response.getMediaUrl().startsWith("/media/" + bucketName + "/"));
    assertTrue(response.getMediaUrl().endsWith("_" + fileName));

    ArgumentCaptor<GeneratePresignedUrlRequest> captor =
            ArgumentCaptor.forClass(GeneratePresignedUrlRequest.class);
    verify(amazonS3).generatePresignedUrl(captor.capture());
    assertEquals(bucketName, captor.getValue().getBucketName());
    assertEquals(com.amazonaws.HttpMethod.PUT, captor.getValue().getMethod());
}

@Test
void generatePresignedUrl_shouldThrowWhenS3Fails() throws Exception {
    when(amazonS3.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
            .thenThrow(new RuntimeException("S3 connection refused"));

    PnPnException ex = assertThrows(PnPnException.class,
            () -> listingService.generatePresignedUrl(bucketName, "test.jpg", influencerId));
    assertEquals("Failed to generate upload URL", ex.getMessage());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
}
```

---

### Step 8 — Delete old files

After migrating, delete these files (no longer needed):

| File                                            | Reason                               |
|-------------------------------------------------|--------------------------------------|
| `.../config/MinioConfig.java`                   | Replaced by `S3Config.java`          |
| `.../config/MinioProperties.java`               | Replaced by `S3Properties.java`      |
| `.../config/MinioInitializer.java`              | Replaced by `S3Initializer.java`     |
| `.../test/.../config/MinioConfigTest.java`      | Replaced by `S3ConfigTest.java`      |
| `.../test/.../config/MinioInitializerTest.java` | Replaced by `S3InitializerTest.java` |

---

## Comparison & Recommendation

| Factor                      | Option A (Config-only)                       | Option B (AWS SDK)                    |
|-----------------------------|----------------------------------------------|---------------------------------------|
| **Code changes**            | None                                         | ~8 files touched                      |
| **Risk**                    | Low — MinIO speaks S3 API natively           | Low — AWS SDK is the canonical client |
| **Presigned URL fidelity**  | May differ slightly from native AWS          | Exactly matches AWS behaviour         |
| **MinIO-specific features** | Can't use (e.g. MinIO console, IAM policies) | Irrelevant                            |
| **Future flexibility**      | Still locked to MinIO SDK API                | Native AWS features accessible        |
| **Effort**                  | 2 files, 5 minutes                           | ~30 minutes + test updates            |

### Recommendation

**Start with Option A.** It requires zero code changes, takes 5 minutes, and works for the vast majority of cases. Deploy to a staging environment and verify presigned URL uploads work end-to-end.

**Only proceed to Option B** if Option A produces presigned URLs that the client cannot use (e.g. expiry mismatch, URL format not understood by the upload library).

---

## Appendix — Files Touched Per Option

### Option A

| File                                                      | Change                                                                   |
|-----------------------------------------------------------|--------------------------------------------------------------------------|
| `pnpn-catalog-service/src/main/resources/application.yml` | Update `minio.url`, `minio.access-key`, `minio.secret-key`, bucket names |
| `pnpn-catalog-service/src/test/resources/application.yml` | Same updates for test profile                                            |

### Option B

| #     | File                                           | Change type                                                         |
|-------|------------------------------------------------|---------------------------------------------------------------------|
| 1     | `pom.xml` (root)                               | Add `aws-java-sdk-s3` version + `<dependency>`                      |
| 2     | `pnpn-catalog-service/pom.xml`                 | Remove MinIO dep, add AWS S3 dep                                    |
| 3     | `.../config/S3Properties.java`                 | **New** file                                                        |
| 4     | `.../config/S3Config.java`                     | **New** file                                                        |
| 5     | `.../config/S3Initializer.java`                | **New** file                                                        |
| 6     | `.../service/ListingService.java`              | Replace `MinioClient` → `AmazonS3`, update `generatePresignedUrl()` |
| 7     | `.../resources/application.yml`                | Change prefix `minio` → `s3`                                        |
| 8     | `.../test/resources/application.yml`           | Same prefix change                                                  |
| 9     | `.../test/.../config/S3ConfigTest.java`        | **New** file                                                        |
| 10    | `.../test/.../config/S3InitializerTest.java`   | **New** file                                                        |
| 11    | `.../test/.../service/ListingServiceTest.java` | Update mock type + presigned URL tests                              |
| 12–16 | Old `Minio*` files (5 files)                   | **Delete**                                                          |
