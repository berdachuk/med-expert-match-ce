package com.berdachuk.medexpertmatch.chat.domain;

import java.time.Instant;

/**
 * Single message in a chat conversation.
 */
public record ChatMessage(
        String id,
        String chatId,
        String role,
        String content,
        int sequenceNumber,
        Integer tokensUsed,
        Instant createdAt
) {
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_SYSTEM = "system";
}
