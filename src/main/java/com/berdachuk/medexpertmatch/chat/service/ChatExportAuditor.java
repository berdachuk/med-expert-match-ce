package com.berdachuk.medexpertmatch.chat.service;

/**
 * Records PHI-safe audit events for chat transcript exports (M20).
 */
public interface ChatExportAuditor {

    void recordExport(String userId, String chatId, int messageCount);
}
