package com.berdachuk.medexpertmatch.chat.service.impl;

import com.berdachuk.medexpertmatch.chat.config.ChatRetentionProperties;
import com.berdachuk.medexpertmatch.chat.domain.Chat;
import com.berdachuk.medexpertmatch.chat.repository.ChatGoalContextRepositoryImpl;
import com.berdachuk.medexpertmatch.chat.repository.ChatMessageRepository;
import com.berdachuk.medexpertmatch.chat.repository.ChatRepository;
import com.berdachuk.medexpertmatch.chat.service.ChatRetentionMetrics;
import com.berdachuk.medexpertmatch.chat.service.ChatRetentionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
public class ChatRetentionServiceImpl implements ChatRetentionService {

    private final ChatRetentionProperties properties;
    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatGoalContextRepositoryImpl goalContextRepository;
    private final ChatRetentionMetrics chatRetentionMetrics;

    public ChatRetentionServiceImpl(
            ChatRetentionProperties properties,
            ChatRepository chatRepository,
            ChatMessageRepository chatMessageRepository,
            ChatGoalContextRepositoryImpl goalContextRepository,
            ChatRetentionMetrics chatRetentionMetrics) {
        this.properties = properties;
        this.chatRepository = chatRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.goalContextRepository = goalContextRepository;
        this.chatRetentionMetrics = chatRetentionMetrics;
    }

    @Override
    @Transactional
    public int purgeIdleChats() {
        if (!properties.enabled()) {
            chatRetentionMetrics.recordPurgeRun(Instant.now(), 0, 0, false, properties.idleDays());
            return 0;
        }
        Instant cutoff = Instant.now().minus(properties.idleDays(), ChronoUnit.DAYS);
        List<Chat> idle = chatRepository.findIdleNonDefaultChatsBefore(cutoff, properties.batchSize());
        int purged = 0;
        int messagesRemoved = 0;
        for (Chat chat : idle) {
            messagesRemoved += chatMessageRepository.getHistory(chat.id(), 10_000, 0).size();
            if (chatRepository.deleteChat(chat.id())) {
                purged++;
                try {
                    goalContextRepository.deleteByChatPattern("%-" + chat.id());
                } catch (Exception ignored) {
                    // best effort
                }
            }
        }
        if (purged > 0) {
            log.info("Purged {} idle non-default chat(s) older than {} days", purged, properties.idleDays());
        }
        chatRetentionMetrics.recordPurgeRun(Instant.now(), purged, messagesRemoved, true, properties.idleDays());
        return purged;
    }
}
