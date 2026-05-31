package com.berdachuk.medexpertmatch.core.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Simulated admin gate for REST admin APIs (M21). Aligns with {@code ?user=admin} web UI pattern.
 */
@Component
public class AdminAccessGuard {

    public static final String ADMIN_USER_ID = "admin";

    private final UserContext userContext;

    public AdminAccessGuard(UserContext userContext) {
        this.userContext = userContext;
    }

    public void requireAdmin() {
        if (!ADMIN_USER_ID.equals(userContext.currentUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}
