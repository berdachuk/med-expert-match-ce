package com.berdachuk.medexpertmatch.core.service;

import com.berdachuk.medexpertmatch.core.domain.AuditLog;

import java.util.List;

/**
 * Admin read access to chat export audit events (M21).
 */
public interface ChatExportAuditQueryService {

    List<AuditLog> listChatExports(int limit, int offset);

    List<AuditLog> listChatExports(int limit, int offset, String action);
}
