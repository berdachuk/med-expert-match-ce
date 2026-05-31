package com.berdachuk.medexpertmatch.llm.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link AgentSkillsProperties} binds {@code agent.skills.*} correctly and applies the
 * documented classpath default ({@code skills}) when properties are omitted. Pure unit test
 * (no Spring context, no Testcontainers).
 */
class AgentSkillsPropertiesTest {

    private static AgentSkillsProperties bind(MockEnvironment env) {
        return new Binder(ConfigurationPropertySources.from(env.getPropertySources()))
                .bind("agent.skills", Bindable.of(AgentSkillsProperties.class))
                .orElseGet(() -> new AgentSkillsProperties(null, null));
    }

    @Test
    @DisplayName("Binds agent.skills.dir and agent.skills.extra-directory from properties")
    void bindsExplicitValues() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("agent.skills.dir", "custom-skills")
                .withProperty("agent.skills.extra-directory", "/mnt/skills");

        AgentSkillsProperties props = bind(env);

        assertEquals("custom-skills", props.dir());
        assertEquals("/mnt/skills", props.extraDirectory());
    }

    @Test
    @DisplayName("Applies classpath default 'skills' and empty extra-directory when omitted")
    void appliesDefaults() {
        AgentSkillsProperties props = bind(new MockEnvironment());

        assertEquals("skills", props.dir(),
                "default skills dir should be the classpath 'skills' directory");
        assertTrue(props.extraDirectory().isEmpty(),
                "default extra-directory should be empty (no mounted volume)");
    }
}
