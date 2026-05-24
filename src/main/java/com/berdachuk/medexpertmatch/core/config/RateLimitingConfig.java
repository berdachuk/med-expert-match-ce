package com.berdachuk.medexpertmatch.core.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitingConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingConfig.class);
    private static final int DEFAULT_RATE_LIMIT = 30;
    private static final int DEFAULT_WINDOW_SECONDS = 60;

    @Bean
    public FilterRegistrationBean<Filter> rateLimitingFilter(MeterRegistry meterRegistry) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TokenBucketFilter(new ConcurrentHashMap<>(), DEFAULT_RATE_LIMIT, DEFAULT_WINDOW_SECONDS, meterRegistry));
        registration.setOrder(-50);
        registration.addUrlPatterns("/api/*");
        return registration;
    }

    static class TokenBucketFilter implements Filter {

        private final Map<String, TokenBucket> buckets;
        private final int maxRequests;
        private final int windowSeconds;
        private final Counter allowedCounter;
        private final Counter deniedCounter;

        TokenBucketFilter(Map<String, TokenBucket> buckets, int maxRequests, int windowSeconds, MeterRegistry meterRegistry) {
            this.buckets = buckets;
            this.maxRequests = maxRequests;
            this.windowSeconds = windowSeconds;
            this.allowedCounter = Counter.builder("medexpertmatch.rate.limit.allowed")
                    .description("Number of requests allowed through rate limiter")
                    .register(meterRegistry);
            this.deniedCounter = Counter.builder("medexpertmatch.rate.limit.denied")
                    .description("Number of requests denied by rate limiter")
                    .register(meterRegistry);
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            String path = httpRequest.getRequestURI();
            if (path.contains("/health") || path.contains("/actuator")) {
                chain.doFilter(request, response);
                return;
            }

            String clientIp = getClientIp(httpRequest);
            TokenBucket bucket = buckets.computeIfAbsent(clientIp,
                    k -> new TokenBucket(maxRequests, windowSeconds));

            if (bucket.tryConsume()) {
                allowedCounter.increment();
                chain.doFilter(request, response);
            } else {
                deniedCounter.increment();
                log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
                httpResponse.setStatus(429);
                httpResponse.setHeader("Retry-After", String.valueOf(windowSeconds));
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"error\":\"Too Many Requests\",\"retryAfterSeconds\":" + windowSeconds + "}");
            }
        }

        private String getClientIp(HttpServletRequest request) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isEmpty()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
    }

    static class TokenBucket {
        private final double maxTokens;
        private final double refillRate;
        private double tokens;
        private long lastRefill;

        TokenBucket(int maxRequests, int windowSeconds) {
            this.maxTokens = maxRequests;
            this.refillRate = (double) maxRequests / windowSeconds;
            this.tokens = maxRequests;
            this.lastRefill = System.nanoTime();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            double elapsed = (now - lastRefill) / 1_000_000_000.0;
            tokens = Math.min(maxTokens, tokens + elapsed * refillRate);
            lastRefill = now;
        }
    }
}
