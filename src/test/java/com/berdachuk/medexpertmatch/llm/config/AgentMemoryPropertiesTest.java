package com.berdachuk.medexpertmatch.llm.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link AgentMemoryProperties} binds {@code agent.memory.*} correctly, applies the
 * documented defaults when properties are omitted, and that the long-term memory directory is
 * auto-created at startup. Pure unit test (no Spring context, no Testcontainers).
 */
class AgentMemoryPropertiesTest {

    private static AgentMemoryProperties bind(MockEnvironment env) {
        return new Binder(ConfigurationPropertySources.from(env.getPropertySources()))
                .bind("agent.memory", Bindable.of(AgentMemoryProperties.class))
                .orElseGet(() -> new AgentMemoryProperties(null, null));
    }

    @Test
    @DisplayName("Binds agent.memory.dir and consolidation gap from properties")
    void bindsExplicitValues(@TempDir Path tmp) {
        Path dir = tmp.resolve("custom-memory");
        MockEnvironment env = new MockEnvironment()
                .withProperty("agent.memory.dir", dir.toString())
                .withProperty("agent.memory.consolidation.gap-seconds", "120");

        AgentMemoryProperties props = bind(env);

        assertEquals(dir.toString(), props.dir());
        assertEquals(120L, props.consolidation().gapSeconds());
    }

    @Test
    @DisplayName("Applies defaults when properties are omitted")
    void appliesDefaults() {
        AgentMemoryProperties props = bind(new MockEnvironment());

        assertNotNull(props.dir());
        assertTrue(props.dir().endsWith("/.spring-ai-agent/medexpertmatch/memory"),
                "default dir should fall under the spring-ai-agent memory tree, was: " + props.dir());
        assertNotNull(props.consolidation());
        assertEquals(60L, props.consolidation().gapSeconds(),
                "default consolidation gap should be 60 seconds");
    }

    @Test
    @DisplayName("ensureDirectoryExists() creates the configured memory directory if missing")
    void createsMemoryDirectory(@TempDir Path tmp) {
        Path dir = tmp.resolve("nested").resolve("memory");
        AgentMemoryProperties props = new AgentMemoryProperties(
                dir.toString(),
                new AgentMemoryProperties.Consolidation(60L, null, null));

        assertTrue(!Files.exists(dir), "precondition: dir must not exist yet");

        props.ensureDirectoryExists();

        assertTrue(Files.isDirectory(dir), "memory directory should be created");
    }
}
