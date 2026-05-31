package com.berdachuk.medexpertmatch.core.domain;

import java.time.Instant;

/**
 * Admin-visible chat retention configuration and last purge snapshot (M27).
 */
public record ChatRetentionStats(
        boolean enabled,
        int idleDays,
        Instant lastRunAt,
        int lastChatsPurged,
        int lastMessagesPurged) {
}
