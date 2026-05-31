package com.berdachuk.medexpertmatch.core.config;

import com.berdachuk.medexpertmatch.core.domain.ApiSessionToken;
import com.berdachuk.medexpertmatch.core.repository.ApiSessionTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * Validates {@code X-API-Key} against persisted session tokens (M21).
 */
@Component
@ConditionalOnProperty(name = "medexpertmatch.auth.session-tokens.enabled", havingValue = "true")
public class SessionTokenApiKeyAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SessionTokenApiKeyAuthFilter.class);
    static final String HEADER_NAME = "X-API-Key";

    private final ApiSessionTokenRepository apiSessionTokenRepository;

    public SessionTokenApiKeyAuthFilter(ApiSessionTokenRepository apiSessionTokenRepository) {
        this.apiSessionTokenRepository = apiSessionTokenRepository;
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
        if (apiKey == null || apiKey.isBlank()) {
            reject(response, path, "Valid X-API-Key header required");
            return;
        }

        var tokenOpt = apiSessionTokenRepository.findByApiKey(apiKey.trim());
        if (tokenOpt.isEmpty()) {
            reject(response, path, "Invalid or revoked API key");
            return;
        }

        ApiSessionToken token = tokenOpt.get();
        if (token.expiresAt() != null && token.expiresAt().isBefore(Instant.now())) {
            reject(response, path, "API key expired");
            return;
        }

        apiSessionTokenRepository.updateLastUsedAt(token.id(), Instant.now());
        chain.doFilter(request, response);
    }

    private static void reject(HttpServletResponse response, String path, String detail) throws IOException {
        log.warn("Unauthorized request to {}: {}", path, detail);
        response.setStatus(401);
        response.setContentType("application/problem+json");
        response.getWriter().write("""
                {"type":"about:blank","title":"Unauthorized","status":401,\
                "detail":"%s","instance":"%s"}\
                """.formatted(detail, path));
    }
}
