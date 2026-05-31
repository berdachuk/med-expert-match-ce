package com.berdachuk.medexpertmatch.chat.service.impl;

import com.berdachuk.medexpertmatch.chat.service.ChatExportAuditor;
import com.berdachuk.medexpertmatch.chat.service.ChatTurnMetrics;
import com.berdachuk.medexpertmatch.core.domain.AuditLog;
import com.berdachuk.medexpertmatch.core.repository.AuditLogRepository;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.core.util.IdentifierHasher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class ChatExportAuditorImpl implements ChatExportAuditor {

    public static final String ACTION = "CHAT_EXPORT";

    private final AuditLogRepository auditLogRepository;
    private final ChatTurnMetrics chatTurnMetrics;

    public ChatExportAuditorImpl(AuditLogRepository auditLogRepository, ChatTurnMetrics chatTurnMetrics) {
        this.auditLogRepository = auditLogRepository;
        this.chatTurnMetrics = chatTurnMetrics;
    }

    @Override
    public void recordExport(String userId, String chatId, int messageCount) {
        String userHash = IdentifierHasher.sha256Hex(userId);
        String chatHash = IdentifierHasher.sha256Hex(chatId);
        AuditLog auditLog = new AuditLog(
                IdGenerator.generateId(),
                ACTION,
                "chat",
                chatHash,
                userHash,
                Map.of("messageCount", messageCount),
                Instant.now());
        auditLogRepository.insert(auditLog);
        chatTurnMetrics.recordExport();
    }
}
