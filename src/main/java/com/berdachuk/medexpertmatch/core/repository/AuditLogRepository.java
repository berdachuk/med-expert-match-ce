package com.berdachuk.medexpertmatch.core.repository;

import com.berdachuk.medexpertmatch.core.domain.AuditLog;

import java.util.List;

/**
 * Repository for structured audit log events.
 */
public interface AuditLogRepository {

    String insert(AuditLog auditLog);

    List<AuditLog> findByAction(String action, int limit);

    List<AuditLog> findByAction(String action, int limit, int offset);

    List<AuditLog> findByActions(java.util.List<String> actions, int limit, int offset);
}
