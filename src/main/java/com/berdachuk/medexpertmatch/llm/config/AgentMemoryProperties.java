package com.berdachuk.medexpertmatch.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Long-term (cross-session) memory settings for the medical agent's AutoMemory layer.
 * <p>
 * Binds {@code agent.memory.*}. The memory directory holds ONLY non-PHI content (clinician
 * preferences, routing policies, model config) — patient data must never be persisted (see
 * {@code PhiGuard} and the HIPAA rules in AGENTS.md).
 *
 * @param dir           filesystem directory for durable memory markdown files; defaults under the
 *                      Spring AI agent memory tree ({@code ${user.home}/.spring-ai-agent/medexpertmatch/memory})
 * @param consolidation memory consolidation trigger settings
 */
@ConfigurationProperties(prefix = "agent.memory")
public record AgentMemoryProperties(String dir, Consolidation consolidation) {

    private static final String DEFAULT_DIR =
            System.getProperty("user.home") + "/.spring-ai-agent/medexpertmatch/memory";

    public AgentMemoryProperties {
        if (dir == null || dir.isBlank()) {
            dir = DEFAULT_DIR;
        }
        if (consolidation == null) {
            consolidation = new Consolidation(null, null, null);
        }
    }

    /**
     * Creates the configured memory directory if it does not yet exist. Invoked at startup so the
     * AutoMemory layer has a writable root.
     */
    public void ensureDirectoryExists() {
        try {
            Files.createDirectories(Path.of(dir));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create agent memory directory: " + dir, e);
        }
    }

    /**
     * Consolidation trigger configuration. A time-gap predicate is always supported; turn-count and
     * probabilistic options are optional (null = disabled).
     *
     * @param gapSeconds  idle gap (seconds) after which consolidation should fire (default 60)
     * @param maxTurns    optional turn-count threshold (nice-to-have; null disables)
     * @param probability optional probabilistic fire chance in [0,1] (nice-to-have; null disables)
     */
    public record Consolidation(Long gapSeconds, Integer maxTurns, Double probability) {

        private static final long DEFAULT_GAP_SECONDS = 60L;

        public Consolidation {
            if (gapSeconds == null) {
                gapSeconds = DEFAULT_GAP_SECONDS;
            }
        }
    }
}
