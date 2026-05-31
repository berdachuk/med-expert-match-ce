package com.berdachuk.medexpertmatch.chat.service.impl;

import com.berdachuk.medexpertmatch.chat.service.ChatRetentionMetrics;
import com.berdachuk.medexpertmatch.core.domain.ChatRetentionStats;
import com.berdachuk.medexpertmatch.core.service.ChatRetentionQueryService;
import org.springframework.stereotype.Service;

@Service
public class ChatRetentionQueryServiceImpl implements ChatRetentionQueryService {

    private final ChatRetentionMetrics chatRetentionMetrics;

    public ChatRetentionQueryServiceImpl(ChatRetentionMetrics chatRetentionMetrics) {
        this.chatRetentionMetrics = chatRetentionMetrics;
    }

    @Override
    public ChatRetentionStats getStats() {
        ChatRetentionMetrics.RetentionRunSnapshot snapshot = chatRetentionMetrics.lastRunSnapshot();
        return new ChatRetentionStats(
                chatRetentionMetrics.retentionEnabled(),
                chatRetentionMetrics.retentionIdleDays(),
                snapshot.lastRunAt(),
                snapshot.chatsPurged(),
                snapshot.messagesPurged());
    }
}
