package com.berdachuk.medexpertmatch.chat.service.impl;

import com.berdachuk.medexpertmatch.chat.domain.Chat;
import com.berdachuk.medexpertmatch.chat.domain.ChatMessage;
import com.berdachuk.medexpertmatch.chat.repository.ChatMessageRepository;
import com.berdachuk.medexpertmatch.chat.repository.ChatRepository;
import com.berdachuk.medexpertmatch.chat.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatServiceImpl(ChatRepository chatRepository, ChatMessageRepository chatMessageRepository) {
        this.chatRepository = chatRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Override
    public Chat getOrCreateDefaultChat(String userId) {
        return chatRepository.findDefaultChat(userId)
                .orElseGet(() -> chatRepository.createChat(userId, "Default Chat", "auto", true));
    }

    @Override
    @Transactional
    public Chat createChat(String userId, String name, String agentId) {
        return chatRepository.createChat(userId, name, agentId, false);
    }

    @Override
    public List<Chat> listChats(String userId) {
        return chatRepository.findAllByUserId(userId);
    }

    @Override
    public Chat requireOwnedChat(String chatId, String userId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found"));
        if (!chat.userId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chat access denied");
        }
        return chat;
    }

    @Override
    @Transactional
    public boolean deleteChat(String chatId, String userId) {
        Chat chat = requireOwnedChat(chatId, userId);
        if (chat.isDefault()) {
            return false;
        }
        return chatRepository.deleteChat(chatId);
    }

    @Override
    @Transactional
    public boolean renameChat(String chatId, String userId, String name) {
        requireOwnedChat(chatId, userId);
        return chatRepository.updateChatName(chatId, name);
    }

    @Override
    public List<ChatMessage> getHistory(String chatId, String userId, int limit, int offset) {
        requireOwnedChat(chatId, userId);
        return chatMessageRepository.getHistory(chatId, limit, offset);
    }

    @Override
    @Transactional
    public ChatMessage appendUserMessage(String chatId, String userId, String content) {
        requireOwnedChat(chatId, userId);
        ChatMessage message = chatMessageRepository.saveMessage(chatId, ChatMessage.ROLE_USER, content);
        chatRepository.updateLastActivity(chatId);
        if (message.sequenceNumber() == 1) {
            String title = content.length() > 60 ? content.substring(0, 60) + "…" : content;
            chatRepository.updateChatName(chatId, title);
        }
        return message;
    }

    @Override
    @Transactional
    public ChatMessage appendAssistantMessage(String chatId, String userId, String content) {
        requireOwnedChat(chatId, userId);
        ChatMessage message = chatMessageRepository.saveMessage(chatId, ChatMessage.ROLE_ASSISTANT, content);
        chatRepository.updateLastActivity(chatId);
        return message;
    }
}
