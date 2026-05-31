package com.berdachuk.medexpertmatch.llm.config;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.CreateSessionRequest;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.compaction.CompactionResult;
import org.springframework.ai.session.compaction.CompactionStrategy;
import org.springframework.ai.session.compaction.CompactionStrategy;
import org.springframework.ai.session.compaction.CompactionTrigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class SessionTurnSafetyIT extends BaseIntegrationTest {

    @Autowired(required = false)
    private SessionService sessionService;

    @Test
    void sessionServiceIsAvailableWithJdbcRepository() {
        assertNotNull(sessionService, "SessionService must be wired when ai_session tables exist");
    }

    @Test
    @DisplayName("JDBC session compacts >20 turns with USER-led retained window")
    void compactsAfterMoreThanTwentyTurns() {
        assertNotNull(sessionService, "SessionService required for JDBC compaction IT");

        AgentSessionProperties props = new AgentSessionProperties(20, 4000, 10);
        MedicalAgentConfiguration config = new MedicalAgentConfiguration(new DefaultResourceLoader());
        CompactionTrigger trigger = config.sessionCompactionTrigger(props, config.sessionTokenCountEstimator());
        CompactionStrategy strategy = config.sessionCompactionStrategy(
                props, config.sessionTokenCountEstimator(), new SessionCompactionObservability());

        String sessionId = "turn-safety-jdbc-" + UUID.randomUUID();
        sessionService.create(CreateSessionRequest.builder()
                .id(sessionId)
                .userId("turn-safety-user")
                .build());

        for (int i = 0; i < 25; i++) {
            sessionService.appendMessage(sessionId, new UserMessage("user turn " + i));
            sessionService.appendMessage(sessionId, new AssistantMessage("assistant turn " + i));
        }

        List<SessionEvent> before = sessionService.getEvents(sessionId);
        assertTrue(before.size() >= 50, "expected at least 50 session events before compaction");

        CompactionResult result = sessionService.compact(sessionId, trigger, strategy);
        assertTrue(result.eventsRemoved() > 0, "compaction must drop oldest turns");
        assertEquals(MessageType.USER, result.compactedEvents().getFirst().getMessageType(),
                "compacted window must start on a USER message");

        List<SessionEvent> after = sessionService.getEvents(sessionId);
        assertTrue(after.size() < before.size(), "persisted events must shrink after compaction");
        assertEquals(MessageType.USER, after.getFirst().getMessageType(),
                "retained JDBC window must start on a USER message");
    }
}
