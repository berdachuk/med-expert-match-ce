package com.berdachuk.medexpertmatch.chat.service;

import com.berdachuk.medexpertmatch.core.domain.RateLimitTier;
import com.berdachuk.medexpertmatch.chat.domain.ChatMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * Generates LLM assistant replies for chat turns (M14).
 */
public interface ChatAssistantService {

    /**
     * Appends the user message, invokes the configured agent, and persists the assistant reply.
     */
    Map<String, ChatMessage> processMessage(
            String chatId, String userId, String content, String agentId, String chatMode);

    /**
     * Streams assistant tokens over SSE, then persists the full assistant reply (M15).
     */
    SseEmitter streamMessage(
            String chatId, String userId, String content, String agentId, String chatMode, RateLimitTier tier);
}
