package com.berdachuk.medexpertmatch.llm.automemory;

import com.berdachuk.medexpertmatch.llm.config.AgentMemoryProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves {@link AutoMemoryService} never persists PHI-shaped content to memory files, while still
 * persisting legitimate non-PHI preferences. Backs the critical HIPAA requirement: long-term
 * memory stores ONLY non-PHI.
 */
class AutoMemoryServicePhiTest {

    private static AutoMemoryService newService(Path dir) {
        AgentMemoryProperties props = new AgentMemoryProperties(
                dir.toString(),
                new AgentMemoryProperties.Consolidation(60L, null, null));
        return new AutoMemoryService(props);
    }

    private static String readDirContents(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (var stream = Files.walk(dir)) {
            for (Path p : stream.filter(Files::isRegularFile).toList()) {
                sb.append(Files.readString(p, StandardCharsets.UTF_8));
            }
        }
        return sb.toString();
    }

    @Test
    @DisplayName("PHI-shaped entry is NOT written to any memory file")
    void rejectsPhiEntry(@TempDir Path dir) throws IOException {
        AutoMemoryService service = newService(dir);

        service.appendEntry("user", "Patient SSN 123-45-6789 prefers morning appointments");

        String contents = readDirContents(dir);
        assertFalse(contents.contains("123-45-6789"), "SSN must never reach memory files");
        assertFalse(contents.contains("Patient SSN"), "PHI-shaped entry must not be persisted");
    }

    @Test
    @DisplayName("Non-PHI preference IS persisted")
    void persistsNonPhi(@TempDir Path dir) throws IOException {
        AutoMemoryService service = newService(dir);

        service.appendEntry("user", "Clinician prefers cardiology routing for chest-pain cases");

        String contents = readDirContents(dir);
        assertTrue(contents.contains("cardiology routing"), "non-PHI entry should be persisted");
    }

    @Test
    @DisplayName("readEntries never returns PHI that was attempted to be stored")
    void readBackHasNoPhi(@TempDir Path dir) {
        AutoMemoryService service = newService(dir);

        service.appendEntry("feedback", "Contact patient at john.doe@example.com about results");

        assertFalse(service.readEntries("feedback").contains("john.doe@example.com"),
                "email PHI must not be retrievable from memory");
    }
}
