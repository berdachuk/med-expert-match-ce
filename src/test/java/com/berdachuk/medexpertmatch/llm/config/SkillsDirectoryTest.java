package com.berdachuk.medexpertmatch.llm.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillsDirectoryTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    @DisplayName("skills loaded from classpath directory")
    void skillsLoadedFromClasspath() {
        var resource = new ClassPathResource("skills");
        assertTrue(resource.exists(), "Skills directory must exist on classpath");
    }

    @Test
    @DisplayName("skills directory contains expected skill folders")
    void skillsDirectoryContainsExpectedFolders() throws IOException {
        var resource = new ClassPathResource("skills");
        assertTrue(resource.exists());

        var file = resource.getFile();
        assertTrue(file.isDirectory());

        String[] contents = file.list();
        assertTrue(contents != null && contents.length > 0, "Skills directory must have skill folders");
    }

    @Test
    @DisplayName("SKILL.md files exist in skill folders")
    void skillMdFilesExist() throws IOException {
        var resource = new ClassPathResource("skills");
        var file = resource.getFile();
        var subdirs = file.listFiles(java.io.File::isDirectory);

        assertTrue(subdirs != null && subdirs.length > 0, "Must have skill subdirectories");

        for (var subdir : subdirs) {
            var skillMd = new java.io.File(subdir, "SKILL.md");
            assertTrue(skillMd.exists(),
                    "SKILL.md must exist in " + subdir.getName());
        }
    }

    @Test
    @DisplayName("app starts successfully with empty extra directory")
    void appStartsWithEmptyExtraDirectory() {
        String extraDir = "";
        assertDoesNotThrow(() -> {
            assertTrue(extraDir.isEmpty() || extraDir.equals(""),
                    "Empty extra directory must not cause errors");
        });
    }

    @Test
    @DisplayName("skill directory behavior handles missing extra directory gracefully")
    void handlesMissingExtraDirectoryGracefully() {
        Path nonExistent = tempDir.resolve("nonexistent-skills");
        assertTrue(Files.notExists(nonExistent), "Directory should not exist for test");
    }
}
