package com.berdachuk.medexpertmatch.core.service.impl;

import com.berdachuk.medexpertmatch.core.domain.AuditLog;
import com.berdachuk.medexpertmatch.core.repository.AuditLogRepository;
import com.berdachuk.medexpertmatch.core.service.ChatExportAuditQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ChatExportAuditQueryServiceImpl implements ChatExportAuditQueryService {

    static final String CHAT_EXPORT_ACTION = "CHAT_EXPORT";
    static final String CHAT_EXPORT_BUNDLE_ACTION = "CHAT_EXPORT_BUNDLE";
    static final String CHAT_DATA_DELETE_ACTION = "CHAT_DATA_DELETE";

    private static final List<String> DEFAULT_EXPORT_ACTIONS = List.of(
            CHAT_EXPORT_ACTION,
            CHAT_EXPORT_BUNDLE_ACTION,
            CHAT_DATA_DELETE_ACTION);

    private final AuditLogRepository auditLogRepository;

    public ChatExportAuditQueryServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> listChatExports(int limit, int offset) {
        return listChatExports(limit, offset, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> listChatExports(int limit, int offset, String action) {
        int safeLimit = Math.min(Math.max(limit, 1), 500);
        int safeOffset = Math.max(offset, 0);
        if (action == null || action.isBlank()) {
            return auditLogRepository.findByActions(DEFAULT_EXPORT_ACTIONS, safeLimit, safeOffset);
        }
        if (!DEFAULT_EXPORT_ACTIONS.contains(action)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported audit action: " + action);
        }
        return auditLogRepository.findByAction(action, safeLimit, safeOffset);
    }
}
