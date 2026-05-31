package com.berdachuk.medexpertmatch.llm.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecialistAgentScopeTest {

    private static final List<String> AGENT_FILES = List.of(
            "auto.md",
            "triage-intake.md",
            "case-analyzer.md",
            "evidence-scout.md",
            "specialist-matcher.md",
            "routing-planner.md",
            "network-analyst.md");

    private final DefaultResourceLoader resourceLoader = new DefaultResourceLoader();

    @Test
    @DisplayName("Specialist agents disallow nested Task delegation")
    void specialistsDisallowTask() throws Exception {
        for (String file : AGENT_FILES) {
            if ("auto.md".equals(file)) {
                continue;
            }
            String content = loadAgent(file);
            assertTrue(content.contains("disallowedTools"), file + " must restrict Task");
            assertTrue(content.toLowerCase(Locale.ROOT).contains("task"), file);
        }
    }

    @Test
    @DisplayName("Auto orchestrator enables Task tool")
    void autoAllowsTask() throws Exception {
        String content = loadAgent("auto.md");
        assertTrue(content.contains("name: auto"));
        assertTrue(content.contains("Task"));
    }

    @Test
    @DisplayName("Each specialist declares skills frontmatter")
    void specialistsDeclareSkills() throws Exception {
        for (String file : AGENT_FILES) {
            if ("auto.md".equals(file)) {
                continue;
            }
            String content = loadAgent(file);
            assertTrue(content.contains("skills:"), file);
            assertFalse(content.isBlank());
        }
    }

    private String loadAgent(String fileName) throws Exception {
        Resource resource = resourceLoader.getResource("classpath:agents/" + fileName);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }
}
