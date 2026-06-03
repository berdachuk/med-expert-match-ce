package com.berdachuk.medexpertmatch.llm.event;

import java.time.Instant;

public record DeadLetterEvent(
        String id,
        String sessionId,
        String agentName,
        String originalEventType,
        String error,
        Instant timestamp,
        String payload) {
}