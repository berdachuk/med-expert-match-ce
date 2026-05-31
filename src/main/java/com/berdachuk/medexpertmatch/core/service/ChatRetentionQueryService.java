package com.berdachuk.medexpertmatch.core.service;

import com.berdachuk.medexpertmatch.core.domain.ChatRetentionStats;

/**
 * Admin read access to chat retention metrics (M27).
 */
public interface ChatRetentionQueryService {

    ChatRetentionStats getStats();
}
