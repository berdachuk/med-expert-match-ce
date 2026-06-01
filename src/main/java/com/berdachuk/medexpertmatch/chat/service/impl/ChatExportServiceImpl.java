package com.berdachuk.medexpertmatch.chat.service.impl;

import com.berdachuk.medexpertmatch.chat.domain.Chat;
import com.berdachuk.medexpertmatch.chat.domain.ChatMessage;
import com.berdachuk.medexpertmatch.chat.service.ChatExportAuditor;
import com.berdachuk.medexpertmatch.chat.service.ChatExportService;
import com.berdachuk.medexpertmatch.chat.service.ChatService;
import com.berdachuk.medexpertmatch.core.compliance.PhiGuard;
import com.berdachuk.medexpertmatch.core.service.HarnessPlanExportQuery;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatExportServiceImpl implements ChatExportService {

    private final ChatService chatService;
    private final ChatExportAuditor chatExportAuditor;
    private final HarnessPlanExportQuery harnessPlanExportQuery;

    public ChatExportServiceImpl(
            ChatService chatService,
            ChatExportAuditor chatExportAuditor,
            HarnessPlanExportQuery harnessPlanExportQuery) {
        this.chatService = chatService;
        this.chatExportAuditor = chatExportAuditor;
        this.harnessPlanExportQuery = harnessPlanExportQuery;
    }

    @Override
    public Map<String, Object> exportTranscript(String chatId, String userId) {
        Chat chat = chatService.requireOwnedChat(chatId, userId);
        List<ChatMessage> history = chatService.getHistory(chatId, userId, 10_000, 0);

        Map<String, Object> export = new LinkedHashMap<>();
        export.put("chatId", chat.id());
        export.put("name", chat.name());
        export.put("agentId", chat.agentId());
        export.put("exportedAt", java.time.Instant.now().toString());
        export.put("phiRedacted", true);
        export.put("messages", history.stream().map(this::toExportMessage).toList());
        String sessionId = userId + "-" + chatId;
        harnessPlanExportQuery.findPlanBySessionId(sessionId).ifPresent(plan -> export.put("plan", plan));

        chatExportAuditor.recordExport(userId, chatId, history.size());
        return export;
    }

    private Map<String, Object> toExportMessage(ChatMessage message) {
        String safeContent = PhiGuard.redact(message.content());
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", message.id());
        row.put("role", message.role());
        row.put("content", safeContent);
        row.put("sequenceNumber", message.sequenceNumber());
        row.put("createdAt", message.createdAt() != null ? message.createdAt().toString() : null);
        return row;
    }
}
