package com.berdachuk.medexpertmatch.chat.repository;

import com.berdachuk.medexpertmatch.chat.domain.ChatMessage;

import java.util.List;

public interface ChatMessageRepository {

    ChatMessage saveMessage(String chatId, String role, String content);

    List<ChatMessage> getHistory(String chatId, int limit, int offset);
}
