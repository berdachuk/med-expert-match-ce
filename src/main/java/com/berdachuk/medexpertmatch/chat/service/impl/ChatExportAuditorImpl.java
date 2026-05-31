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
    public static final String BUNDLE_ACTION = "CHAT_EXPORT_BUNDLE";
    public static final String DATA_DELETE_ACTION = "CHAT_DATA_DELETE";

    private final AuditLogRepository auditLogRepository;
    private final ChatTurnMetrics chatTurnMetrics;

    public ChatExportAuditorImpl(AuditLogRepository auditLogRepository, ChatTurnMetrics chatTurnMetrics) {
        this.auditLogRepository = auditLogRepository;
        this.chatTurnMetrics = chatTurnMetrics;
    }

    @Override
    public void recordExport(String userId, String chatId, int messageCount) {
        insertAudit(IdGenerator.generateId(), ACTION, "chat",
                IdentifierHasher.sha256Hex(chatId), IdentifierHasher.sha256Hex(userId),
                Map.of("messageCount", messageCount));
    }

    @Override
    public String recordExportBundle(String userId, int chatCount, int messageCount) {
        String auditId = IdGenerator.generateId();
        String userHash = IdentifierHasher.sha256Hex(userId);
        insertAudit(auditId, BUNDLE_ACTION, "chat_bundle", userHash, userHash,
                Map.of("chatCount", chatCount, "messageCount", messageCount));
        return IdentifierHasher.sha256Hex(auditId);
    }

    @Override
    public String recordDataDeletion(String userId, int chatsRemoved, int messagesSoftDeleted) {
        String auditId = IdGenerator.generateId();
        String userHash = IdentifierHasher.sha256Hex(userId);
        insertAudit(auditId, DATA_DELETE_ACTION, "chat_data", userHash, userHash,
                Map.of("chatsRemoved", chatsRemoved, "messagesSoftDeleted", messagesSoftDeleted));
        return IdentifierHasher.sha256Hex(auditId);
    }

    private void insertAudit(String id, String action, String resourceType, String resourceId,
                             String actor, Map<String, Object> details) {
        AuditLog auditLog = new AuditLog(id, action, resourceType, resourceId, actor, details, Instant.now());
        auditLogRepository.insert(auditLog);
        chatTurnMetrics.recordExport();
    }
}
