package com.berdachuk.medexpertmatch.llm.automemory;

import com.berdachuk.medexpertmatch.core.compliance.PhiGuard;
import com.berdachuk.medexpertmatch.llm.config.AgentMemoryProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Set;

@Slf4j
@Service
@ConditionalOnProperty(name = "medexpertmatch.skills.enabled", havingValue = "true", matchIfMissing = true)
public class AutoMemoryService {

    private static final Set<String> VALID_TYPES = Set.of("user", "feedback", "project", "reference");
    private static final String INDEX_FILE = "MEMORY.md";

    private final Path rootDir;

    public AutoMemoryService(AgentMemoryProperties properties) {
        this.rootDir = Path.of(properties.dir());
        properties.ensureDirectoryExists();
    }

    public void appendEntry(String type, String markdownLine) {
        if (!VALID_TYPES.contains(type)) {
            log.warn("Invalid AutoMemory type: {}, skipping", type);
            return;
        }
        // HIPAA guard: durable memory holds ONLY non-PHI. Reject PHI-shaped content outright so
        // patient data never reaches disk. No PHI in logs — log only the type.
        if (PhiGuard.containsPhi(markdownLine)) {
            log.warn("Rejected AutoMemory entry of type {} - PHI-shaped content not persisted", type);
            return;
        }
        try {
            Path filePath = rootDir.resolve(type + ".md");
            String entry = String.format("- %s (session: %s)%n",
                    markdownLine, "recorded");
            Files.writeString(filePath, entry, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            refreshIndex(type, markdownLine);
            log.debug("Appended AutoMemory entry to type: {}", type);
        } catch (IOException e) {
            log.error("Failed to append AutoMemory entry for type: {}", type, e);
        }
    }

    public String readEntries(String type) {
        if ("all".equals(type)) {
            return readAll();
        }
        if (!VALID_TYPES.contains(type)) {
            log.warn("Invalid AutoMemory type: {}, returning empty", type);
            return "";
        }
        try {
            Path filePath = rootDir.resolve(type + ".md");
            if (!Files.exists(filePath)) {
                return "";
            }
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to read AutoMemory entries for type: {}", type, e);
            return "";
        }
    }

    public String readAll() {
        StringBuilder all = new StringBuilder();
        for (String type : VALID_TYPES) {
            String entries = readEntries(type);
            if (!entries.isEmpty()) {
                all.append("## ").append(capitalize(type)).append("\n\n");
                all.append(entries).append("\n");
            }
        }
        return all.toString();
    }

    public String readIndex() {
        try {
            Path indexPath = rootDir.resolve(INDEX_FILE);
            if (!Files.exists(indexPath)) {
                return "# AutoMemory Index\n\nNo memories recorded yet.\n";
            }
            return Files.readString(indexPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to read AutoMemory index", e);
            return "";
        }
    }

    private void refreshIndex(String type, String preview) {
        try {
            Path indexPath = rootDir.resolve(INDEX_FILE);
            String timestamp = Instant.now().toString();
            String entry;
            if (!Files.exists(indexPath)) {
                entry = String.format("# AutoMemory Index%n%n## %s%n- [%s] %s (session) %s%n",
                        capitalize(type), timestamp, preview, "");
                Files.writeString(indexPath, entry, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } else {
                entry = String.format("- [%s] [%s] %s%n", capitalize(type), timestamp, preview);
                Files.writeString(indexPath, entry, StandardCharsets.UTF_8,
                        StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            log.warn("Failed to refresh AutoMemory index", e);
        }
    }

    private static String capitalize(String type) {
        if (type == null || type.isEmpty()) {
            return type;
        }
        return type.substring(0, 1).toUpperCase() + type.substring(1);
    }
}
