package com.berdachuk.medexpertmatch.llm.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tracks session compaction activity for logs and health checks (M18).
 */
@Slf4j
@Component
public class SessionCompactionObservability {

    private final AtomicReference<Instant> lastCompactionAt = new AtomicReference<>();
    private final AtomicInteger compactionCount = new AtomicInteger();
    private final AtomicInteger failureCount = new AtomicInteger();

    public void recordCompaction(String sessionId, int eventsRemoved) {
        lastCompactionAt.set(Instant.now());
        compactionCount.incrementAndGet();
        log.info("Session compaction completed for sessionHash={} eventsRemoved={}",
                hashSessionId(sessionId), eventsRemoved);
    }

    public void recordFailure(String sessionId) {
        failureCount.incrementAndGet();
        log.warn("Session compaction failed for sessionHash={}", hashSessionId(sessionId));
    }

    public Instant lastCompactionAt() {
        return lastCompactionAt.get();
    }

    public int compactionCount() {
        return compactionCount.get();
    }

    public int failureCount() {
        return failureCount.get();
    }

    static String hashSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return "unknown";
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(sessionId.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            return "unavailable";
        }
    }
}
