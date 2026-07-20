package com.ironmetrics.shared.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iron-metrics.security.jwt")
public record JwtProperties(
        String issuer,
        String secret,
        Duration accessTokenTtl
) {
}
