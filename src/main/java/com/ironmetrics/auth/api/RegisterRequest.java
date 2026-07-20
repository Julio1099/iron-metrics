package com.ironmetrics.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @NotBlank
        @Size(max = 120)
        String displayName,

        @NotBlank
        @Size(min = 8, max = 120)
        String password
) {
}
