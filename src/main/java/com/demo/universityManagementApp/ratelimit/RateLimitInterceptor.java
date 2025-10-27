package com.demo.universityManagementApp.ratelimit;

import com.demo.universityManagementApp.rest.model.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Spring MVC interceptor implementation for enforcing rate limiting on HTTP requests with comprehensive
 * key resolution and graceful rate limit exceeded responses. Provides configurable rate limiting
 * based on user identity or client IP address with detailed error reporting.
 * Key Features:
 * - User-based rate limiting for authenticated requests
 * - IP-based rate limiting for anonymous requests
 * - X-Forwarded-For header support for proxy environments
 * - Structured JSON error responses with retry timing information
 * - Non-blocking token consumption with precise wait time calculation
 * This interceptor integrates with the Bucket4j library for efficient token bucket algorithm
 * implementation and consistent rate limiting behavior across the application.
 *
 * @see org.springframework.stereotype.Component
 * @see org.springframework.web.servlet.HandlerInterceptor
 * @see BucketService
 * @see com.fasterxml.jackson.databind.ObjectMapper
 */
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final BucketService bucketService;
    private final ObjectMapper objectMapper;

    /**
     * Intercepts HTTP requests before handler execution to enforce rate limiting policies.
     * Consumes a token from the appropriate rate limiting bucket and blocks requests that exceed
     * the configured rate limits with detailed error responses including retry timing information.
     * Implementation Details:
     * - Resolves rate limiting key based on user or client IP address
     * - Attempts to consume a token from the corresponding rate limiting bucket
     * - Returns HTTP 429 Too Many Requests with JSON error body when rate limit exceeded
     * - Allows request progression when tokens are available and consumed successfully
     *
     * @param request the current HTTP request being intercepted
     * @param response the HTTP response that will be returned to the client
     * @param handler the target handler for the request, typically a controller method
     * @return true if the request should proceed to the handler, false if rate limited and blocked
     * @throws Exception if any error occurs during rate limiting evaluation or response writing
     * @see #resolveKey(HttpServletRequest)
     * @see BucketService#resolveBucket(String)
     */
    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) throws Exception {

        String key = resolveKey(request);
        Bucket bucket = bucketService.resolveBucket(key);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long waitSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());

            ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.TOO_MANY_REQUESTS.value())
                    .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                    .message(String.format("Rate limit exceeded. Try again in %d seconds", waitSeconds))
                    .path(request.getRequestURI())
                    .build();

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return false;
        }

        return true;
    }

    private String resolveKey(final HttpServletRequest request) {
        String username = (request.getUserPrincipal() != null) ? request.getUserPrincipal().getName() : null;
        if (username != null) return String.format("user: %s", username);

        String xfHeader = request.getHeader("X-Forwarded-For");
        return (xfHeader != null) ? String.format("ip: %s", xfHeader.split(",")[0]) : String.format("ip: %s", request.getRemoteAddr());
    }
}
