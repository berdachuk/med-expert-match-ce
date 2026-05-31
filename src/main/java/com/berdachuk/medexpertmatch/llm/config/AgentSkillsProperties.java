package com.berdachuk.medexpertmatch.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the medical agent's runtime Agent Skills registry.
 * <p>
 * Binds {@code agent.skills.*}. By default skills are loaded from the {@code skills} directory on the
 * classpath (JAR-safe via {@code SkillsTool.addSkillsResource}). An optional {@code extraDirectory}
 * points at a filesystem location (e.g. a mounted volume) for additional skills layered on top.
 *
 * @param dir            classpath skills directory (default {@code skills})
 * @param extraDirectory optional filesystem directory for extra skills (default empty = none)
 */
@ConfigurationProperties(prefix = "agent.skills")
public record AgentSkillsProperties(String dir, String extraDirectory) {

    private static final String DEFAULT_DIR = "skills";

    public AgentSkillsProperties {
        if (dir == null || dir.isBlank()) {
            dir = DEFAULT_DIR;
        }
        if (extraDirectory == null) {
            extraDirectory = "";
        }
    }
}
