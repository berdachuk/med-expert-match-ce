package com.berdachuk.medexpertmatch.llm.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
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
