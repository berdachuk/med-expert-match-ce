package com.berdachuk.medexpertmatch.llm.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tracks session compaction activity for logs, health checks (M18), and Micrometer metrics (M141).
 */
@Slf4j
@Component
public class SessionCompactionObservability {

    private static final int MAX_SAMPLED_SESSION_GAUGES = 64;

    private final MeterRegistry meterRegistry;
    private final Counter compactionTotal;
    private final Counter eventsRemovedTotal;
    private final Counter failureTotal;
    private final Map<String, AtomicInteger> sampledSessionEventCounts = new ConcurrentHashMap<>();

    private final AtomicReference<Instant> lastCompactionAt = new AtomicReference<>();
    private final AtomicInteger compactionCount = new AtomicInteger();
    private final AtomicInteger failureCount = new AtomicInteger();

    public SessionCompactionObservability(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.compactionTotal = Counter.builder("session.compaction.total")
                .description("Session memory compactions completed")
                .register(meterRegistry);
        this.eventsRemovedTotal = Counter.builder("session.compaction.events_removed")
                .description("Session events removed by compaction")
                .register(meterRegistry);
        this.failureTotal = Counter.builder("session.compaction.failure.total")
                .description("Session compaction failures")
                .register(meterRegistry);
    }

    public void recordCompaction(String sessionId, int eventsRemoved) {
        recordCompaction(sessionId, eventsRemoved, 0);
    }

    public void recordCompaction(String sessionId, int eventsRemoved, int remainingEvents) {
        lastCompactionAt.set(Instant.now());
        compactionCount.incrementAndGet();
        compactionTotal.increment();
        if (eventsRemoved > 0) {
            eventsRemovedTotal.increment(eventsRemoved);
        }
        recordSampledSessionEventGauge(sessionId, remainingEvents);
        log.info("Session compaction completed for sessionHash={} eventsRemoved={}",
                hashSessionId(sessionId), eventsRemoved);
    }

    public void recordFailure(String sessionId) {
        failureCount.incrementAndGet();
        failureTotal.increment();
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

    static boolean isSampledSessionHash(String sessionHash) {
        if (sessionHash == null || sessionHash.isBlank() || sessionHash.length() < 1) {
            return false;
        }
        return (sessionHash.charAt(0) & 0x3) == 0;
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

    private void recordSampledSessionEventGauge(String sessionId, int remainingEvents) {
        String sessionHash = hashSessionId(sessionId);
        if (!isSampledSessionHash(sessionHash)) {
            return;
        }
        AtomicInteger existing = sampledSessionEventCounts.get(sessionHash);
        if (existing != null) {
            existing.set(remainingEvents);
            return;
        }
        if (sampledSessionEventCounts.size() >= MAX_SAMPLED_SESSION_GAUGES) {
            return;
        }
        AtomicInteger value = new AtomicInteger(remainingEvents);
        Gauge.builder("session.events.count", value, AtomicInteger::get)
                .description("Remaining session events after compaction (sampled by session_hash)")
                .tag("session_hash", sessionHash)
                .register(meterRegistry);
        sampledSessionEventCounts.put(sessionHash, value);
    }
}
