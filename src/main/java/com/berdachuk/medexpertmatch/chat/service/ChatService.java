package com.berdachuk.medexpertmatch.chat.service;

import com.berdachuk.medexpertmatch.chat.domain.Chat;
import com.berdachuk.medexpertmatch.chat.domain.ChatMessage;

import java.util.List;

public interface ChatService {

    Chat getOrCreateDefaultChat(String userId);

    Chat createChat(String userId, String name, String agentId);

    List<Chat> listChats(String userId);

    Chat requireOwnedChat(String chatId, String userId);

    boolean deleteChat(String chatId, String userId);

    boolean renameChat(String chatId, String userId, String name);

    List<ChatMessage> getHistory(String chatId, String userId, int limit, int offset);

    ChatMessage appendUserMessage(String chatId, String userId, String content);

    ChatMessage appendAssistantMessage(String chatId, String userId, String content);
}
