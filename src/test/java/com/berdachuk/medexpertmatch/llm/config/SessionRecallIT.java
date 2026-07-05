package com.berdachuk.medexpertmatch.llm.config;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.berdachuk.medexpertmatch.llm.agent.AgentSessionBranches;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.session.CreateSessionRequest;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.session.compaction.CompactionResult;
import org.springframework.ai.session.compaction.CompactionStrategy;
import org.springframework.ai.session.compaction.CompactionTrigger;
import org.springframework.ai.session.tool.SessionEventTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.DefaultResourceLoader;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * REQ-134: Spring AI Session 0.5 recall and branch isolation integration coverage (M141).
 */
@SpringBootTest
class SessionRecallIT extends BaseIntegrationTest {

    private static final String RECALL_KEYWORD = "rare-symptom-alpha";

    @Autowired(required = false)
    private SessionService sessionService;

    @Test
    void sessionServiceIsAvailableWithJdbcRepository() {
        assertNotNull(sessionService, "SessionService must be wired when ai_session tables exist");
    }

    @Test
    @DisplayName("conversation_search finds compacted turn text in JDBC session log")
    void recallFindsKeywordAfterCompaction() {
        assertNotNull(sessionService, "SessionService required for recall IT");

        AgentSessionProperties props = new AgentSessionProperties(20, 4000, 10, 0);
        MedicalAgentConfiguration config = new MedicalAgentConfiguration(new DefaultResourceLoader());
        CompactionTrigger trigger = config.sessionCompactionTrigger(props, config.sessionTokenCountEstimator());
        CompactionStrategy strategy = config.sessionCompactionStrategy(
                props, config.sessionTokenCountEstimator(), new SessionCompactionObservability(new SimpleMeterRegistry()));

        String sessionId = "recall-it-" + UUID.randomUUID();
        sessionService.create(CreateSessionRequest.builder()
                .id(sessionId)
                .userId("recall-it-user")
                .build());

        sessionService.appendMessage(sessionId, new UserMessage("first turn mentions " + RECALL_KEYWORD));
        sessionService.appendMessage(sessionId, new AssistantMessage("acknowledged"));
        for (int i = 1; i < 25; i++) {
            sessionService.appendMessage(sessionId, new UserMessage("user turn " + i));
            sessionService.appendMessage(sessionId, new AssistantMessage("assistant turn " + i));
        }

        CompactionResult compaction = sessionService.compact(sessionId, trigger, strategy);
        assertTrue(compaction.eventsRemoved() > 0, "compaction must remove older turns");

        SessionEventTools tools = SessionEventTools.builder(sessionService).build();
        ToolContext toolContext = new ToolContext(
                Map.of(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, sessionId));
        String result = tools.conversationSearch("recall earlier symptom", RECALL_KEYWORD, 0, toolContext);

        assertNotNull(result);
        assertTrue(result.toLowerCase().contains(RECALL_KEYWORD),
                "recall must find keyword from compacted-away turn in full JDBC history");
    }

    @Test
    @DisplayName("Orchestrator branch filter excludes orch.sub.* events from JDBC reads")
    void orchestratorBranchFilterExcludesSubagentEvents() {
        assertNotNull(sessionService, "SessionService required for branch isolation IT");

        String sessionId = "branch-it-" + UUID.randomUUID();
        sessionService.create(CreateSessionRequest.builder()
                .id(sessionId)
                .userId("branch-it-user")
                .build());

        sessionService.appendEvent(SessionEvent.builder()
                .sessionId(sessionId)
                .timestamp(Instant.now())
                .message(new UserMessage("shared root turn"))
                .build());
        sessionService.appendEvent(SessionEvent.builder()
                .sessionId(sessionId)
                .timestamp(Instant.now())
                .message(new AssistantMessage("orchestrator reply"))
                .branch(AgentSessionBranches.ORCHESTRATOR)
                .build());
        sessionService.appendEvent(SessionEvent.builder()
                .sessionId(sessionId)
                .timestamp(Instant.now())
                .message(new AssistantMessage("subagent tool noise"))
                .branch("orch.sub.test-agent")
                .build());

        List<SessionEvent> orchestratorEvents = sessionService.getEvents(
                sessionId, AgentSessionBranches.orchestratorEventFilter());

        assertFalse(orchestratorEvents.stream().anyMatch(e -> "orch.sub.test-agent".equals(e.getBranch())),
                "orchestrator reads must exclude subagent branch events");
        assertTrue(orchestratorEvents.stream().anyMatch(e -> e.getBranch() == null
                        || AgentSessionBranches.ORCHESTRATOR.equals(e.getBranch())),
                "orchestrator reads must retain root and orch branch events");
    }
}
