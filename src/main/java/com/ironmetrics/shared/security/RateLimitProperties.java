package com.ironmetrics.shared.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "iron-metrics.rate-limit")
public record RateLimitProperties(
        @Valid
        @NotNull
        Limit auth,

        @Valid
        @NotNull
        Limit authenticated
) {

    public record Limit(
            @Min(1)
            long capacity,

            @Min(1)
            long refillTokens,

            @NotNull
            Duration refillPeriod
    ) {
    }
}
