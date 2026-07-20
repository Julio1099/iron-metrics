package com.ironmetrics.shared.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class ProblemDetailsAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ProblemDetailsResponseWriter problemDetailsResponseWriter;

    public ProblemDetailsAuthenticationEntryPoint(ProblemDetailsResponseWriter problemDetailsResponseWriter) {
        this.problemDetailsResponseWriter = problemDetailsResponseWriter;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        problemDetailsResponseWriter.write(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                "Authentication is required."
        );
    }
}
