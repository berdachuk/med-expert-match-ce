package com.berdachuk.medexpertmatch.core.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Simulated reviewer gate for harness workflow checkpoints (admin or clinician user id).
 */
@Component
public class CheckpointAccessGuard {

    public static final String CLINICIAN_USER_ID = "clinician";

    private final UserContext userContext;

    public CheckpointAccessGuard(UserContext userContext) {
        this.userContext = userContext;
    }

    public void requireCheckpointReviewer() {
        String userId = userContext.currentUserId();
        if (!AdminAccessGuard.ADMIN_USER_ID.equals(userId) && !CLINICIAN_USER_ID.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Checkpoint reviewer access required");
        }
    }
}
