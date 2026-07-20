package com.ironmetrics.auth.api;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        AuthenticatedUserResponse user
) {
}
