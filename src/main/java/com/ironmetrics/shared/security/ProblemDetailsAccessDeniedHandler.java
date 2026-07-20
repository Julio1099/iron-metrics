package com.ironmetrics.shared.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class ProblemDetailsAccessDeniedHandler implements AccessDeniedHandler {

    private final ProblemDetailsResponseWriter problemDetailsResponseWriter;

    public ProblemDetailsAccessDeniedHandler(ProblemDetailsResponseWriter problemDetailsResponseWriter) {
        this.problemDetailsResponseWriter = problemDetailsResponseWriter;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        problemDetailsResponseWriter.write(
                request,
                response,
                HttpStatus.FORBIDDEN,
                "Access is denied."
        );
    }
}
