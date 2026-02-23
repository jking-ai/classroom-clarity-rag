package com.jkingai.classroomclarity.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jkingai.classroomclarity.dto.ErrorResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, Bucket> generalBuckets = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Bucket> queryBuckets = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!properties.enabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        String path = request.getRequestURI();

        // Check query-specific rate limit first
        if (path.equals("/api/v1/query")) {
            Bucket queryBucket = queryBuckets.computeIfAbsent(clientIp, k -> createQueryBucket());
            if (!queryBucket.tryConsume(1)) {
                writeRateLimitResponse(response, request);
                return;
            }
        }

        // Check general rate limit
        Bucket generalBucket = generalBuckets.computeIfAbsent(clientIp, k -> createGeneralBucket());
        if (!generalBucket.tryConsume(1)) {
            writeRateLimitResponse(response, request);
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/v1/health") || path.equals("/api/v1/limits") || path.startsWith("/actuator/");
    }

    private Bucket createGeneralBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(properties.burstCapacity())
                        .refillGreedy(properties.requestsPerMinute(), Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private Bucket createQueryBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(properties.queryBurstCapacity())
                        .refillGreedy(properties.queryRequestsPerMinute(), Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeRateLimitResponse(HttpServletResponse response, HttpServletRequest request)
            throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", "60");

        ErrorResponse error = ErrorResponse.of(
                "RATE_LIMIT_EXCEEDED",
                "Too many requests. Please try again later.",
                request.getRequestURI()
        );
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
