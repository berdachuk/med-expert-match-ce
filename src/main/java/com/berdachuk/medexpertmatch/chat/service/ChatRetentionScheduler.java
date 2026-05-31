package com.berdachuk.medexpertmatch.chat.service;

import com.berdachuk.medexpertmatch.chat.config.ChatRetentionProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ChatRetentionScheduler {

    private final ChatRetentionService chatRetentionService;
    private final ChatRetentionProperties properties;
    private final ChatRetentionMetrics chatRetentionMetrics;

    public ChatRetentionScheduler(
            ChatRetentionService chatRetentionService,
            ChatRetentionProperties properties,
            ChatRetentionMetrics chatRetentionMetrics) {
        this.chatRetentionService = chatRetentionService;
        this.properties = properties;
        this.chatRetentionMetrics = chatRetentionMetrics;
    }

    @Scheduled(cron = "${chat.retention.cron:0 0 3 * * *}")
    public void purgeIdleChats() {
        if (!properties.enabled()) {
            return;
        }
        try {
            chatRetentionService.purgeIdleChats();
        } catch (RuntimeException ex) {
            chatRetentionMetrics.recordFailure();
            log.error("Chat retention purge failed", ex);
            throw ex;
        }
    }
}
