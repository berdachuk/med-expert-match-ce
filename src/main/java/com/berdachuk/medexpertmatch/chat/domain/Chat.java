package com.berdachuk.medexpertmatch.chat.domain;

import java.time.Instant;

/**
 * AI chat session owned by a single user.
 */
public record Chat(
        String id,
        String userId,
        String name,
        String agentId,
        boolean isDefault,
        Instant createdAt,
        Instant updatedAt,
        Instant lastActivityAt,
        int messageCount
) {
}
