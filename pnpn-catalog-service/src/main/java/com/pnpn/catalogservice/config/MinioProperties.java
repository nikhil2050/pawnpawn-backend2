package com.pnpn.catalogservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "minio")
@Setter
@Getter
public class MinioProperties {

    private String url;

    private String accessKey;

    private String secretKey;

    private Map<String, String> buckets;

}
