package com.berdachuk.medexpertmatch.web.domain;

import com.berdachuk.medexpertmatch.chat.domain.ChatMessage;
import com.berdachuk.medexpertmatch.web.service.ChatMarkdownRenderer;

/**
 * View model for chat history rows with optional server-rendered assistant HTML.
 */
public record ChatMessageDisplay(String role, String content, String renderedHtml) {

    public static ChatMessageDisplay from(ChatMessage message, ChatMarkdownRenderer renderer) {
        if ("assistant".equals(message.role())) {
            return new ChatMessageDisplay(message.role(), message.content(),
                    renderer.renderSafe(message.content()));
        }
        return new ChatMessageDisplay(message.role(), message.content(), null);
    }
}
