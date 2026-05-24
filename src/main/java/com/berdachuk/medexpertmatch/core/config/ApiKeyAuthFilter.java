package com.berdachuk.medexpertmatch.core.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "medexpertmatch.auth.enabled", havingValue = "true")
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);
    private static final String HEADER_NAME = "X-API-Key";

    private final Set<String> validKeys;

    public ApiKeyAuthFilter(@Value("${medexpertmatch.auth.api-keys:}") String apiKeys) {
        this.validKeys = apiKeys != null && !apiKeys.isBlank()
                ? Set.of(apiKeys.split(","))
                : Set.of();
        log.info("API key auth configured with {} valid key(s)", validKeys.size());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.contains("/actuator") || path.contains("/health") || path.startsWith("/api/v1/openapi")) {
            chain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(HEADER_NAME);
        if (apiKey == null || !validKeys.contains(apiKey)) {
            log.warn("Unauthorized request to {} from {}", path, request.getRemoteAddr());
            response.setStatus(401);
            response.setContentType("application/problem+json");
            response.getWriter().write("""
                    {"type":"about:blank","title":"Unauthorized","status":401,\
                    "detail":"Valid X-API-Key header required","instance":"%s"}\
                    """.formatted(path));
            return;
        }

        chain.doFilter(request, response);
    }
}
