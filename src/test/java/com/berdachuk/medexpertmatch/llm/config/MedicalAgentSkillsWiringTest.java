package com.berdachuk.medexpertmatch.llm.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the Agent Skills wiring in {@link MedicalAgentConfiguration}: the {@code skillsTool}
 * factory builds a {@link ToolCallback} from the default classpath {@code skills} directory,
 * and that {@code FileSystemTools} and {@code ShellTools} are intentionally NOT wired
 * (HIPAA: no unsandboxed script execution, and to prevent LLM from misreading case IDs as file paths).
 * Pure unit test (no Spring context, no Testcontainers, no LLM call).
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
    @DisplayName("fileSystemTools is NOT exposed — prevents LLM from misreading case IDs as file paths")
    void fileSystemToolsIsNotWired() {
        boolean hasFileSystemToolsBean = Arrays.stream(MedicalAgentConfiguration.class.getDeclaredMethods())
                .anyMatch(m -> m.getReturnType().getName().contains("FileSystemTools"));

        assertFalse(hasFileSystemToolsBean,
                "FileSystemTools must not be registered — prevents LLM from reading case IDs as files");
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
    void memoryPromptStillPhiSafe() throws Exception {
        String prompt = StreamUtils.copyToString(
                new ClassPathResource("prompts/auto-memory-system.st").getInputStream(),
                StandardCharsets.UTF_8);
        assertTrue(prompt.toLowerCase().contains("phi"),
                "memory system prompt must still warn about PHI after skills wiring");
    }
}
