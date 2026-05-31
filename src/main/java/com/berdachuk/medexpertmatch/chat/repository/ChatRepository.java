package com.berdachuk.medexpertmatch.chat.repository;

import com.berdachuk.medexpertmatch.chat.domain.Chat;

import java.util.List;
import java.util.Optional;

public interface ChatRepository {

    Chat createChat(String userId, String name, String agentId, boolean isDefault);

    Optional<Chat> findById(String chatId);

    Optional<Chat> findDefaultChat(String userId);

    List<Chat> findAllByUserId(String userId);

    boolean updateChatName(String chatId, String name);

    boolean updateAgentId(String chatId, String agentId);

    boolean deleteChat(String chatId);

    void updateLastActivity(String chatId);

    List<Chat> findIdleNonDefaultChatsBefore(java.time.Instant cutoff, int limit);
}
