package com.ironmetrics.shared.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private final RateLimitProperties rateLimitProperties;
    private final ProblemDetailsResponseWriter problemDetailsResponseWriter;
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(
            RateLimitProperties rateLimitProperties,
            ProblemDetailsResponseWriter problemDetailsResponseWriter
    ) {
        this.rateLimitProperties = rateLimitProperties;
        this.problemDetailsResponseWriter = problemDetailsResponseWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Optional<RateLimitTarget> target = resolveTarget(request);
        if (target.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        ConsumptionProbe probe = bucketFor(target.get()).tryConsumeAndReturnRemaining(1);
        response.setHeader("X-Rate-Limit-Remaining", Long.toString(probe.getRemainingTokens()));

        if (!probe.isConsumed()) {
            response.setHeader("Retry-After", Long.toString(secondsUntilRefill(probe)));
            problemDetailsResponseWriter.write(
                    request,
                    response,
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Rate limit exceeded."
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Optional<RateLimitTarget> resolveTarget(HttpServletRequest request) {
        String path = request.getServletPath();
        if (HttpMethod.OPTIONS.matches(request.getMethod()) || isOperationalPath(path)) {
            return Optional.empty();
        }

        if (path.startsWith("/auth/")) {
            return Optional.of(new RateLimitTarget(
                    "auth:" + clientIp(request),
                    rateLimitProperties.auth()
            ));
        }

        return authenticatedPrincipal()
                .map(principal -> new RateLimitTarget(
                        "user:" + principal,
                        rateLimitProperties.authenticated()
                ));
    }

    private boolean isOperationalPath(String path) {
        return path.startsWith("/actuator")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html");
    }

    private Optional<String> authenticatedPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        return Optional.of(authentication.getName());
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(X_FORWARDED_FOR);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private Bucket bucketFor(RateLimitTarget target) {
        return buckets.computeIfAbsent(target.key(), ignored -> newBucket(target.limit()));
    }

    private Bucket newBucket(RateLimitProperties.Limit limit) {
        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(limit.capacity())
                .refillGreedy(limit.refillTokens(), limit.refillPeriod())
                .build();
        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }

    private long secondsUntilRefill(ConsumptionProbe probe) {
        return Math.max(1, Math.ceilDiv(probe.getNanosToWaitForRefill(), NANOS_PER_SECOND));
    }

    private record RateLimitTarget(String key, RateLimitProperties.Limit limit) {
    }
}
