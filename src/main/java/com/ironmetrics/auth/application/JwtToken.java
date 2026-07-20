package com.ironmetrics.auth.application;

public record JwtToken(
        String value,
        long expiresInSeconds
) {
}
