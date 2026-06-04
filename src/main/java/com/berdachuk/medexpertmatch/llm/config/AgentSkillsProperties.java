package com.berdachuk.medexpertmatch.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
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
