package com.berdachuk.medexpertmatch.chat.service;

/**
 * Records PHI-safe audit events for chat transcript exports (M20).
 */
public interface ChatExportAuditor {

    void recordExport(String userId, String chatId, int messageCount);

    /**
     * @return SHA-256 hash of audit log id (safe reference for UI feedback)
     */
    String recordExportBundle(String userId, int chatCount, int messageCount);

    /**
     * @return SHA-256 hash of audit log id (safe reference for UI feedback)
     */
    String recordDataDeletion(String userId, int chatsRemoved, int messagesSoftDeleted);
}
