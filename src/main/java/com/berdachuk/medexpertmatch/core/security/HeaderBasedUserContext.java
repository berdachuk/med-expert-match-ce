package com.berdachuk.medexpertmatch.core.security;

import com.berdachuk.medexpertmatch.core.domain.RateLimitTier;
import com.berdachuk.medexpertmatch.core.repository.ApiSessionTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Reads simulated user id from {@code X-User-Id} header (set by web UI)
 * and optional rate-limit tier from {@code X-API-Key} session tokens.
 */
@Component
public class HeaderBasedUserContext implements UserContext {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String API_KEY_HEADER = "X-API-Key";
    private static final String ANONYMOUS = "anonymous-user";

    private final ApiSessionTokenRepository apiSessionTokenRepository;

    public HeaderBasedUserContext(ApiSessionTokenRepository apiSessionTokenRepository) {
        this.apiSessionTokenRepository = apiSessionTokenRepository;
    }

    @Override
    public String currentUserId() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return ANONYMOUS;
        }
        HttpServletRequest request = attributes.getRequest();
        String userId = request.getHeader(USER_ID_HEADER);
        if (userId == null || userId.isBlank()) {
            if (request.getCookies() != null) {
                for (var cookie : request.getCookies()) {
                    if ("medexpertmatch-user-id".equals(cookie.getName())
                            && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                        return cookie.getValue().trim();
                    }
                }
            }
            return ANONYMOUS;
        }
        return userId.trim();
    }

    @Override
    public RateLimitTier currentRateLimitTier() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return RateLimitTier.DEFAULT;
        }
        String apiKey = attributes.getRequest().getHeader(API_KEY_HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            return RateLimitTier.DEFAULT;
        }
        return apiSessionTokenRepository.findByApiKey(apiKey.trim())
                .map(token -> token.rateLimitTier())
                .orElse(RateLimitTier.DEFAULT);
    }
}
