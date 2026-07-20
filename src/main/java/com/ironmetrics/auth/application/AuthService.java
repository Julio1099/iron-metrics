package com.ironmetrics.auth.application;

import com.ironmetrics.auth.api.AuthResponse;
import com.ironmetrics.auth.api.AuthenticatedUserResponse;
import com.ironmetrics.auth.api.LoginRequest;
import com.ironmetrics.auth.api.RegisterRequest;
import com.ironmetrics.users.domain.UserAccount;
import com.ironmetrics.users.infrastructure.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        userAccountRepository.findByEmailIgnoreCase(request.email())
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists.");
                });

        UserAccount userAccount = userAccountRepository.save(new UserAccount(
                request.email(),
                request.displayName(),
                passwordEncoder.encode(request.password())
        ));

        return toResponse(userAccount);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserAccount userAccount = userAccountRepository.findByEmailIgnoreCase(request.email())
                .filter(existing -> passwordEncoder.matches(request.password(), existing.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password."
                ));

        return toResponse(userAccount);
    }

    private AuthResponse toResponse(UserAccount userAccount) {
        JwtToken token = jwtTokenService.issueToken(userAccount);
        return new AuthResponse(
                token.value(),
                "Bearer",
                token.expiresInSeconds(),
                AuthenticatedUserResponse.from(userAccount)
        );
    }
}
