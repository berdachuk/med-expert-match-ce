package com.berdachuk.medexpertmatch.llm.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agent.tools.FileSystemTools;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.DefaultResourceLoader;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the Agent Skills wiring in {@link MedicalAgentConfiguration}: the {@code skillsTool}
 * factory builds a {@link ToolCallback} from the default classpath {@code skills} directory, the
 * {@code fileSystemTools} factory exposes the read-capable tool, and that {@code ShellTools} is
 * intentionally NOT wired (HIPAA: no unsandboxed script execution). Pure unit test (no Spring
 * context, no Testcontainers, no LLM call).
 */
class MedicalAgentSkillsWiringTest {

    private final MedicalAgentConfiguration config =
            new MedicalAgentConfiguration(new DefaultResourceLoader());

    @Test
    @DisplayName("skillsTool factory builds a ToolCallback from the default classpath skills dir")
    void skillsToolBuildsFromClasspathDefault() {
        AgentSkillsProperties props = new AgentSkillsProperties(null, null);

        ToolCallback skillsTool = config.skillsTool(props);

        assertNotNull(skillsTool, "skillsTool must be built from the classpath skills directory");
    }

    @Test
    @DisplayName("fileSystemTools factory exposes the read tool for skill reference files")
    void fileSystemToolsExposesRead() throws Exception {
        FileSystemTools tools = config.fileSystemTools();

        assertNotNull(tools, "FileSystemTools bean must be created");
        Method read = tools.getClass().getMethod("read", String.class, Integer.class, Integer.class);
        assertNotNull(read, "FileSystemTools must expose a read(...) tool method");
    }

    @Test
    @DisplayName("ShellTools is intentionally NOT a factory method on the agent configuration (HIPAA)")
    void shellToolsIsNotWired() {
        boolean hasShellToolsBean = Arrays.stream(MedicalAgentConfiguration.class.getDeclaredMethods())
                .anyMatch(m -> m.getReturnType().getName().contains("ShellTools"));

        assertFalse(hasShellToolsBean,
                "ShellTools must not be registered as a bean — no unsandboxed script execution under HIPAA");
    }

    @Test
    @DisplayName("Memory system prompt remains PHI-safe alongside skills wiring")
    void memoryPromptStillPhiSafe() {
        assertTrue(MedicalAgentConfiguration.MEMORY_SYSTEM_PROMPT.toLowerCase().contains("phi"),
                "memory system prompt must still warn about PHI after skills wiring");
    }
}
