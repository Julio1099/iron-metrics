package com.ironmetrics.auth.api;

import com.ironmetrics.users.domain.UserAccount;
import java.util.UUID;

public record AuthenticatedUserResponse(
        UUID id,
        String email,
        String displayName
) {

    public static AuthenticatedUserResponse from(UserAccount userAccount) {
        return new AuthenticatedUserResponse(
                userAccount.getId(),
                userAccount.getEmail(),
                userAccount.getDisplayName()
        );
    }
}
