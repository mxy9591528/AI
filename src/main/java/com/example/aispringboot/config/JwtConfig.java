package com.example.aispringboot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix="jwt")
public class JwtConfig {
    private String secret;
    private long expiration;
}

