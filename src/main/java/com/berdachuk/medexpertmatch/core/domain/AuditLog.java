package com.berdachuk.medexpertmatch.core.domain;

import java.time.Instant;
import java.util.Map;

/**
 * Structured audit event for security and compliance tracking.
 */
public record AuditLog(
        String id,
        String action,
        String resourceType,
        String resourceId,
        String actor,
        Map<String, Object> details,
        Instant createdAt
) {
}
