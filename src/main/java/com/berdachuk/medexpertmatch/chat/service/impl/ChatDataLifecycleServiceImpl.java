package com.berdachuk.medexpertmatch.chat.service.impl;

import com.berdachuk.medexpertmatch.chat.domain.Chat;
import com.berdachuk.medexpertmatch.chat.domain.ChatMessage;
import com.berdachuk.medexpertmatch.chat.repository.ChatMessageRepository;
import com.berdachuk.medexpertmatch.chat.repository.ChatRepository;
import com.berdachuk.medexpertmatch.chat.service.ChatDataLifecycleService;
import com.berdachuk.medexpertmatch.chat.service.ChatExportAuditor;
import com.berdachuk.medexpertmatch.chat.service.ChatService;
import com.berdachuk.medexpertmatch.core.compliance.PhiGuard;
import com.berdachuk.medexpertmatch.core.util.IdentifierHasher;
import com.berdachuk.medexpertmatch.core.service.HarnessPlanExportQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ChatDataLifecycleServiceImpl implements ChatDataLifecycleService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatService chatService;
    private final ChatExportAuditor chatExportAuditor;
    private final HarnessPlanExportQuery harnessPlanExportQuery;

    public ChatDataLifecycleServiceImpl(
            ChatRepository chatRepository,
            ChatMessageRepository chatMessageRepository,
            ChatService chatService,
            ChatExportAuditor chatExportAuditor,
            HarnessPlanExportQuery harnessPlanExportQuery) {
        this.chatRepository = chatRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chatService = chatService;
        this.chatExportAuditor = chatExportAuditor;
        this.harnessPlanExportQuery = harnessPlanExportQuery;
    }

    @Override
    @Transactional
    public Map<String, Object> deleteAllUserData(String userId) {
        chatService.getOrCreateDefaultChat(userId);
        List<Chat> chats = chatRepository.findAllByUserId(userId);
        int messagesSoftDeleted = 0;
        int chatsRemoved = 0;

        for (Chat chat : chats) {
            messagesSoftDeleted += chatMessageRepository.softDeleteByChatId(chat.id());
            if (!chat.isDefault() && chatRepository.deleteChat(chat.id())) {
                chatsRemoved++;
            }
        }

        String auditReferenceHash = chatExportAuditor.recordDataDeletion(userId, chatsRemoved, messagesSoftDeleted);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "deleted");
        result.put("chatsRemoved", chatsRemoved);
        result.put("messagesSoftDeleted", messagesSoftDeleted);
        result.put("auditReferenceHash", auditReferenceHash);
        return result;
    }

    @Override
    public Map<String, Object> exportUserBundle(String userId) {
        List<Chat> chats = chatService.listChats(userId);
        List<Map<String, Object>> chatExports = new ArrayList<>();
        int totalMessages = 0;

        for (Chat chat : chats) {
            List<ChatMessage> history = chatService.getHistory(chat.id(), userId, 10_000, 0);
            totalMessages += history.size();
            chatExports.add(toChatExport(userId, chat, history));
        }

        String auditReferenceHash = chatExportAuditor.recordExportBundle(userId, chatExports.size(), totalMessages);

        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("userIdHash", IdentifierHasher.sha256Hex(userId));
        bundle.put("exportedAt", java.time.Instant.now().toString());
        bundle.put("phiRedacted", true);
        bundle.put("chatCount", chatExports.size());
        bundle.put("messageCount", totalMessages);
        bundle.put("auditReferenceHash", auditReferenceHash);
        bundle.put("chats", chatExports);
        return bundle;
    }

    private Map<String, Object> toChatExport(String userId, Chat chat, List<ChatMessage> history) {
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("chatId", chat.id());
        export.put("name", chat.name());
        export.put("agentId", chat.agentId());
        export.put("isDefault", chat.isDefault());
        export.put("messages", history.stream().map(this::toExportMessage).toList());
        String sessionId = userId + "-" + chat.id();
        harnessPlanExportQuery.findPlanBySessionId(sessionId).ifPresent(plan -> export.put("plan", plan));
        return export;
    }

    private Map<String, Object> toExportMessage(ChatMessage message) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", message.id());
        row.put("role", message.role());
        row.put("content", PhiGuard.redact(message.content()));
        row.put("sequenceNumber", message.sequenceNumber());
        row.put("createdAt", message.createdAt() != null ? message.createdAt().toString() : null);
        return row;
    }
}
