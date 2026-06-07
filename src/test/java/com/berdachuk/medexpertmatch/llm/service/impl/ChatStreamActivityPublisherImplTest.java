package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.chat.service.ChatTurnMetrics;
import com.berdachuk.medexpertmatch.core.util.LlmCacheSource;
import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import com.berdachuk.medexpertmatch.core.util.LlmOperation;
import com.berdachuk.medexpertmatch.llm.event.LlmCallCompletedEvent;
import com.berdachuk.medexpertmatch.llm.monitoring.LlmCallSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ChatStreamActivityPublisherImplTest {

    private ChatStreamActivityPublisherImpl publisher;
    private SseEmitter emitter;

    @BeforeEach
    void setUp() {
        publisher = new ChatStreamActivityPublisherImpl(mock(ChatTurnMetrics.class));
        emitter = mock(SseEmitter.class);
        publisher.register("chat-1", emitter);
    }

    @Test
    @DisplayName("LlmCallCompletedEvent maps to llm_call SSE payload")
    void publishesLlmCallActivity() throws IOException {
        LlmCallSnapshot snapshot = new LlmCallSnapshot(
                "chat-1",
                LlmClientType.TOOL_CALLING,
                LlmOperation.CHAT_STREAM,
                "STANDARD",
                "GENERAL_QUESTION",
                "functiongemma",
                890,
                204,
                null,
                null,
                "stop",
                900L,
                1200,
                3,
                4096,
                LlmCacheSource.NONE,
                false);

        publisher.onLlmCallCompleted(new LlmCallCompletedEvent(snapshot));

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
    }
}
