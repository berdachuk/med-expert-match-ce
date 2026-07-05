package com.berdachuk.medexpertmatch.llm.config;

import com.berdachuk.medexpertmatch.core.service.ChatRetentionPurgeListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.session.SessionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Deletes Spring AI JDBC session rows when chat retention purges the parent chat (M141).
 */
@Slf4j
@Component
@ConditionalOnBean(SessionService.class)
public class SessionRetentionPurgeListener implements ChatRetentionPurgeListener {

    private final SessionService sessionService;

    public SessionRetentionPurgeListener(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void onChatPurged(String userId, String chatId) {
        if (userId == null || userId.isBlank() || chatId == null || chatId.isBlank()) {
            return;
        }
        String sessionId = userId + "-" + chatId;
        try {
            sessionService.delete(sessionId);
            log.debug("Purged Spring AI session for retained chat sessionIdHash={}",
                    SessionCompactionObservability.hashSessionId(sessionId));
        } catch (Exception ex) {
            log.warn("Failed to purge Spring AI session for chat {}: {}", chatId, ex.getMessage());
        }
    }
}
