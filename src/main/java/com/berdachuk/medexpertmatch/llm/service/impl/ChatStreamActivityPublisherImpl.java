package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.chat.service.ChatTurnMetrics;
import com.berdachuk.medexpertmatch.core.event.ToolCallLoggedEvent;
import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import com.berdachuk.medexpertmatch.llm.event.LlmCallCompletedEvent;
import com.berdachuk.medexpertmatch.llm.monitoring.LlmCallSnapshot;
import com.berdachuk.medexpertmatch.llm.monitoring.LlmUsageSessionRollup;
import com.berdachuk.medexpertmatch.llm.service.AgentTodoUpdateEvent;
import com.berdachuk.medexpertmatch.llm.service.ChatStreamActivityPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ChatStreamActivityPublisherImpl implements ChatStreamActivityPublisher {

    private final ChatTurnMetrics chatTurnMetrics;
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public ChatStreamActivityPublisherImpl(ChatTurnMetrics chatTurnMetrics) {
        this.chatTurnMetrics = chatTurnMetrics;
    }

    @Override
    public void register(String sessionId, SseEmitter emitter) {
        if (sessionId == null || sessionId.isBlank() || emitter == null) {
            return;
        }
        emitters.put(sessionId, emitter);
    }

    @Override
    public void unregister(String sessionId) {
        if (sessionId != null) {
            emitters.remove(sessionId);
        }
    }

    @Override
    public void publishReasoning(String sessionId, String message) {
        publish(sessionId, "reasoning", Map.of("message", message));
    }

    @Override
    public void publishTurnSummary(String sessionId, LlmUsageSessionRollup rollup) {
        if (rollup == null || rollup.llmCallCount() == 0) {
            return;
        }
        publish(sessionId, "llm_turn_summary", Map.of(
                "message", rollup.compactSummary(),
                "llmCallCount", rollup.llmCallCount(),
                "totalPromptTokens", rollup.totalPromptTokens(),
                "totalCompletionTokens", rollup.totalCompletionTokens()));
    }

    @EventListener
    void onLlmCallCompleted(LlmCallCompletedEvent event) {
        LlmCallSnapshot snapshot = event.snapshot();
        String sessionId = event.sessionId();
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", snapshot.compactMessage());
        payload.put("operation", snapshot.operation().uiLabel());
        payload.put("clientType", snapshot.clientType().name());
        if (snapshot.model() != null) {
            payload.put("model", snapshot.model());
        }
        if (snapshot.promptTokens() != null) {
            payload.put("promptTokens", snapshot.promptTokens());
        }
        if (snapshot.completionTokens() != null) {
            payload.put("completionTokens", snapshot.completionTokens());
        }
        if (snapshot.cacheReadTokens() != null) {
            payload.put("cacheReadTokens", snapshot.cacheReadTokens());
        }
        payload.put("latencyMs", snapshot.latencyMs());
        payload.put("cacheHit", snapshot.cacheHit());
        publish(sessionId, "llm_call", payload);
    }

    @EventListener
    void onToolCallLogged(ToolCallLoggedEvent event) {
        if (event.sessionId() == null || event.toolName() == null) {
            return;
        }
        publish(event.sessionId(), "tool_call", Map.of(
                "toolName", event.toolName(),
                "message", "Tool: " + event.toolName()));
        chatTurnMetrics.recordToolCall();
    }

    @EventListener
    void onTodoUpdate(AgentTodoUpdateEvent event) {
        String sessionId = OrchestrationContextHolder.sessionIdOrNull();
        if (sessionId == null || event.todos() == null) {
            return;
        }
        List<Map<String, String>> todos = event.todos().todos().stream()
                .map(item -> Map.of(
                        "content", item.activeForm() != null && !item.activeForm().isBlank()
                                ? item.activeForm() : item.content(),
                        "status", item.status().name()))
                .toList();
        publish(sessionId, "todo_update", Map.of("todos", todos));
    }

    private void publish(String sessionId, String type, Map<String, Object> fields) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>(fields);
        payload.put("type", type);
        try {
            emitter.send(SseEmitter.event().name("activity").data(payload));
        } catch (IOException e) {
            log.debug("Failed to send chat activity event for session {}: {}", sessionId, e.getMessage());
            emitters.remove(sessionId);
        }
    }
}
