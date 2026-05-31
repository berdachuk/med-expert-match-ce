package com.berdachuk.medexpertmatch.llm.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springaicommunity.agent.utils.MarkdownParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the two new runtime skills authored for this feature ({@code clinical-guideline} and
 * {@code triage}). Each SKILL.md must exist, parse, declare {@code name} + {@code description}
 * frontmatter, and the {@code name} must match its folder. Pure unit test (no Testcontainers).
 */
class NewSkillsFrontmatterTest {

    private static final Path SKILLS_ROOT = Path.of("src", "main", "resources", "skills");

    @ParameterizedTest(name = "skill ''{0}'' has valid frontmatter with name matching its folder")
    @ValueSource(strings = {"clinical-guideline", "triage"})
    @DisplayName("New skills declare name (matching folder) + description and are non-PHI advisory")
    void newSkillHasValidFrontmatter(String skillName) throws Exception {
        Path skillFile = SKILLS_ROOT.resolve(skillName).resolve("SKILL.md");
        assertTrue(Files.isRegularFile(skillFile),
                "SKILL.md must exist for new skill: " + skillFile);

        MarkdownParser parser = new MarkdownParser(Files.readString(skillFile));
        Map<String, Object> frontMatter = parser.getFrontMatter();

        assertNotNull(frontMatter.get("name"), "frontmatter must declare 'name'");
        assertEquals(skillName, frontMatter.get("name").toString(),
                "frontmatter name must match the skill folder name");

        Object description = frontMatter.get("description");
        assertNotNull(description, "frontmatter must declare 'description'");
        assertFalse(description.toString().isBlank(), "description must not be blank");

        String content = parser.getContent();
        assertNotNull(content, "SKILL.md body must not be null");
        assertFalse(content.isBlank(), "SKILL.md body (instructions) must not be blank");

        // Advisory, not diagnostic: a medical disclaimer must be present per repo HIPAA conventions.
        assertTrue(content.toLowerCase().contains("disclaimer"),
                "skill '" + skillName + "' must include a medical disclaimer (advisory, not diagnostic)");
    }
}
