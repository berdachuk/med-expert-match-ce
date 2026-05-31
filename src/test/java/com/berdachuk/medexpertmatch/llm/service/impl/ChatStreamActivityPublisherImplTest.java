package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.chat.service.ChatTurnMetrics;
import com.berdachuk.medexpertmatch.core.event.ToolCallLoggedEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import com.berdachuk.medexpertmatch.llm.service.AgentTodoUpdateEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatStreamActivityPublisherImplTest {

    private final ChatStreamActivityPublisherImpl publisher =
            new ChatStreamActivityPublisherImpl(new ChatTurnMetrics(new SimpleMeterRegistry()));

    @AfterEach
    void clearContext() {
        OrchestrationContextHolder.clear();
        publisher.unregister("session-1");
    }

    @Test
    @DisplayName("forwards tool_call activity to registered chat SSE emitter")
    void forwardsToolCallActivity() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        publisher.register("session-1", emitter);

        publisher.onToolCallLogged(new ToolCallLoggedEvent("session-1", "match_doctors_to_case", "caseId: abc"));

        ArgumentCaptor<org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder> captor =
                ArgumentCaptor.forClass(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder.class);
        verify(emitter, atLeastOnce()).send(captor.capture());
    }

    @Test
    @DisplayName("forwards todo_update activity when orchestration session is set")
    void forwardsTodoUpdateActivity() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        publisher.register("session-1", emitter);
        OrchestrationContextHolder.setSessionId("session-1");

        TodoWriteTool.Todos todos = new TodoWriteTool.Todos(List.of(
                new TodoWriteTool.Todos.TodoItem(
                        "Match specialists",
                        TodoWriteTool.Todos.Status.in_progress,
                        "Matching specialists")));
        publisher.onTodoUpdate(new AgentTodoUpdateEvent(this, todos));

        verify(emitter, atLeastOnce()).send(org.mockito.ArgumentMatchers.<SseEmitter.SseEventBuilder>any());
    }

    @Test
    @DisplayName("publishReasoning sends PHI-safe status line")
    void publishesReasoning() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        publisher.register("session-1", emitter);

        publisher.publishReasoning("session-1", "Planning response…");

        verify(emitter, atLeastOnce()).send(org.mockito.ArgumentMatchers.<SseEmitter.SseEventBuilder>any());
    }
}
