package com.berdachuk.medexpertmatch.core.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Reads simulated user id from {@code X-User-Id} header (set by web UI).
 */
@Component
public class HeaderBasedUserContext implements UserContext {

    public static final String USER_ID_HEADER = "X-User-Id";
    private static final String ANONYMOUS = "anonymous-user";

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
}
