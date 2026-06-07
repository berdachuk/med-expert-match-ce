package com.berdachuk.medexpertmatch.llm.event;

import com.berdachuk.medexpertmatch.llm.monitoring.LlmCallSnapshot;

public record LlmCallCompletedEvent(LlmCallSnapshot snapshot) {

    public String sessionId() {
        return snapshot != null ? snapshot.sessionId() : null;
    }
}
