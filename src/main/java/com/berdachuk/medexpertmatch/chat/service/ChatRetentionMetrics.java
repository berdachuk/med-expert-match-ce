package com.berdachuk.medexpertmatch.chat.service;

import com.berdachuk.medexpertmatch.chat.config.ChatRetentionProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Micrometer metrics and last-run snapshot for chat retention (M26).
 */
@Component
public class ChatRetentionMetrics {

    private final Counter runs;
    private final Counter chatsPurged;
    private final Counter messagesPurged;
    private final Counter failures;
    private final ChatRetentionProperties properties;
    private final AtomicReference<RetentionRunSnapshot> lastRun = new AtomicReference<>(RetentionRunSnapshot.empty());

    public ChatRetentionMetrics(MeterRegistry meterRegistry, ChatRetentionProperties properties) {
        this.properties = properties;
        this.runs = Counter.builder("chat.retention.runs")
                .description("Scheduled chat retention purge runs")
                .register(meterRegistry);
        this.chatsPurged = Counter.builder("chat.retention.chats_purged")
                .description("Idle non-default chats removed by retention")
                .register(meterRegistry);
        this.messagesPurged = Counter.builder("chat.retention.messages_purged")
                .description("Messages removed with purged chats")
                .register(meterRegistry);
        this.failures = Counter.builder("chat.retention.failures")
                .description("Failed chat retention purge runs")
                .register(meterRegistry);
    }

    public void recordFailure() {
        failures.increment();
    }

    public void recordPurgeRun(Instant runAt, int chatsRemoved, int messagesRemoved, boolean enabled, int idleDays) {
        runs.increment();
        if (chatsRemoved > 0) {
            chatsPurged.increment(chatsRemoved);
        }
        if (messagesRemoved > 0) {
            messagesPurged.increment(messagesRemoved);
        }
        lastRun.set(new RetentionRunSnapshot(runAt, chatsRemoved, messagesRemoved, enabled, idleDays));
    }

    public RetentionRunSnapshot lastRunSnapshot() {
        return lastRun.get();
    }

    public boolean retentionEnabled() {
        return properties.enabled();
    }

    public int retentionIdleDays() {
        return properties.idleDays();
    }

    public record RetentionRunSnapshot(
            Instant lastRunAt,
            int chatsPurged,
            int messagesPurged,
            boolean enabled,
            int idleDays) {

        static RetentionRunSnapshot empty() {
            return new RetentionRunSnapshot(null, 0, 0, false, 0);
        }
    }
}
