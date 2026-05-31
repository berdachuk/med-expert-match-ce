package com.berdachuk.medexpertmatch.llm.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.compaction.CompactionRequest;
import org.springframework.ai.session.compaction.CompactionResult;
import org.springframework.ai.session.compaction.CompactionStrategy;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Verifies turn-window compaction retains a USER-led window after more than 20 turns.
 */
class SessionTurnWindowSafetyTest {

    @Test
    @DisplayName("Retained window starts on USER after >20 turns")
    void retainedWindowStartsOnUser() {
        AgentSessionProperties props = new AgentSessionProperties(20, 4000, 30);
        MedicalAgentConfiguration config = new MedicalAgentConfiguration(mock(org.springframework.core.io.ResourceLoader.class));
        CompactionStrategy strategy = config.sessionCompactionStrategy(
                props, new JTokkitTokenCountEstimator(), new SessionCompactionObservability());

        Session session = Session.builder()
                .id("turn-safety-test")
                .userId("test-user")
                .createdAt(Instant.now())
                .build();

        List<SessionEvent> events = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            events.add(sessionEvent(session.id(), new UserMessage("user turn " + i)));
            events.add(sessionEvent(session.id(), new AssistantMessage("assistant turn " + i)));
        }

        CompactionRequest request = CompactionRequest.of(session, events);
        CompactionResult result = strategy.compact(request);
        assertEquals(MessageType.USER, result.compactedEvents().getFirst().getMessageType(),
                "compacted window must start on a USER message");
    }

    private static SessionEvent sessionEvent(String sessionId, org.springframework.ai.chat.messages.Message message) {
        return SessionEvent.builder()
                .sessionId(sessionId)
                .timestamp(Instant.now())
                .message(message)
                .build();
    }
}
