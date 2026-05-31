package com.berdachuk.medexpertmatch.llm.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springaicommunity.agent.utils.Skills;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the runtime Agent Skills registry under {@code src/main/resources/skills/} resolves all
 * expected skills (7 existing + 2 new = 9) through the spring-ai-agent-utils 0.8.0
 * {@link Skills} loader, that the two new skills ({@code clinical-guideline}, {@code triage}) are
 * present, and that every skill parses with valid frontmatter (name + description). Pure unit test
 * (no Spring context, no Testcontainers).
 */
class AgentSkillsRegistryTest {

    private static final String SKILLS_DIR = "src/main/resources/skills";

    private static final Set<String> EXPECTED_SKILLS = Set.of(
            "case-analyzer",
            "clinical-advisor",
            "doctor-matcher",
            "evidence-retriever",
            "network-analyzer",
            "recommendation-engine",
            "routing-planner",
            "clinical-guideline",
            "triage");

    private static List<SkillsTool.Skill> loadSkills() {
        return Skills.loadDirectory(SKILLS_DIR);
    }

    @Test
    @DisplayName("Registry resolves exactly 9 skills (7 existing + 2 new)")
    void resolvesAllNineSkills() {
        List<SkillsTool.Skill> skills = loadSkills();

        assertEquals(9, skills.size(),
                "expected 7 existing + 2 new skills, found: "
                        + skills.stream().map(SkillsTool.Skill::name).sorted().toList());
    }

    @Test
    @DisplayName("Registry includes all expected skill names, including the two new ones")
    void includesNewSkillNames() {
        Set<String> names = loadSkills().stream()
                .map(SkillsTool.Skill::name)
                .collect(Collectors.toSet());

        assertEquals(EXPECTED_SKILLS, names,
                "skill registry names must match the expected set");
        assertTrue(names.contains("clinical-guideline"), "clinical-guideline skill must be registered");
        assertTrue(names.contains("triage"), "triage skill must be registered");
    }

    @Test
    @DisplayName("Every skill parses with non-blank name and description frontmatter")
    void everySkillHasValidFrontmatter() {
        for (SkillsTool.Skill skill : loadSkills()) {
            assertNotNull(skill.name(), "skill name must not be null");
            assertFalse(skill.name().isBlank(), "skill name must not be blank");

            Object description = skill.frontMatter().get("description");
            assertNotNull(description, "skill '" + skill.name() + "' must declare a description");
            assertFalse(description.toString().isBlank(),
                    "skill '" + skill.name() + "' description must not be blank");
        }
    }

    @Test
    @DisplayName("SkillsTool builds from the runtime skills directory without error")
    void skillsToolBuildsFromDirectory() {
        var toolCallback = SkillsTool.builder()
                .addSkillsDirectory(SKILLS_DIR)
                .build();

        assertNotNull(toolCallback, "SkillsTool must build a ToolCallback from the skills directory");
    }
}
