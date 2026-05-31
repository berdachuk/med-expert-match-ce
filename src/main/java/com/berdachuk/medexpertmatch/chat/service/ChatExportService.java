package com.berdachuk.medexpertmatch.chat.service;

import java.util.Map;

/**
 * PHI-safe chat transcript export (M19).
 */
public interface ChatExportService {

    Map<String, Object> exportTranscript(String chatId, String userId);
}
