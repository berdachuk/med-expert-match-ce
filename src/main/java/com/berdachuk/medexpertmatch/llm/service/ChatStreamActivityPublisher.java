package com.berdachuk.medexpertmatch.llm.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Bridges orchestration signals (tool calls, reasoning, todos) to the chat SSE stream.
 */
public interface ChatStreamActivityPublisher {

    void register(String sessionId, SseEmitter emitter);

    void unregister(String sessionId);

    void publishReasoning(String sessionId, String message);
}
