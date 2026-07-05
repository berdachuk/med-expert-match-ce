package com.berdachuk.medexpertmatch.llm.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.session.tool.SessionEventTools;
import org.springframework.core.io.DefaultResourceLoader;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies post-compaction recall wiring: {@link SessionEventTools} bean and session-scoped
 * {@code conversation_search} using the same context key as {@link SessionMemoryAdvisor}.
 */
class SessionEventToolsWiringTest {

    @Test
    @DisplayName("sessionEventTools bean builds from SessionService")
    void buildsSessionEventToolsBean() {
        MedicalAgentConfiguration config = new MedicalAgentConfiguration(new DefaultResourceLoader());

        SessionEventTools tools = config.sessionEventTools(mock(SessionService.class));

        assertNotNull(tools);
    }

    @Test
    @DisplayName("conversationSearch resolves session ID from ToolContext and keyword-filters events")
    void conversationSearchUsesSessionIdFromToolContext() {
        SessionService sessionService = mock(SessionService.class);
        String sessionId = "chat-session-42";
        SessionEvent event = SessionEvent.builder()
                .sessionId(sessionId)
                .timestamp(Instant.now())
                .message(new UserMessage("earlier symptom: fatigue"))
                .build();
        when(sessionService.getEvents(eq(sessionId), any(EventFilter.class)))
                .thenReturn(List.of(event));

        SessionEventTools tools = SessionEventTools.builder(sessionService).build();
        ToolContext toolContext = new ToolContext(
                Map.of(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, sessionId));

        String result = tools.conversationSearch(
                "recall prior symptom",
                "fatigue",
                0,
                toolContext);

        assertNotNull(result);
        assertTrue(result.contains("fatigue"),
                "search result should include matched event content");
        verify(sessionService).getEvents(eq(sessionId), argThat(filter ->
                "fatigue".equals(filter.keyword())
                        && filter.page() != null
                        && filter.page() == 0));
    }
}
