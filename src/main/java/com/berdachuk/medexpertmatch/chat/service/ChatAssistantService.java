package com.berdachuk.medexpertmatch.chat.service;

import com.berdachuk.medexpertmatch.chat.domain.ChatMessage;

import java.util.Map;

/**
 * Generates LLM assistant replies for chat turns (M14).
 */
public interface ChatAssistantService {

    /**
     * Appends the user message, invokes the configured agent, and persists the assistant reply.
     *
     * @param chatId chat session id
     * @param userId owning user id
     * @param content user message text
     * @param agentId optional agent override from picker; falls back to chat.agentId()
     * @return user and assistant messages
     */
    Map<String, ChatMessage> processMessage(String chatId, String userId, String content, String agentId);
}
