package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.llm.service.MedicalAgentPromptSupportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Shared skill loading and prompt construction support.
 */
@Slf4j
@Service
public class MedicalAgentPromptSupportServiceImpl implements MedicalAgentPromptSupportService {

    private final ResourceLoader resourceLoader;
    private final String skillsDirectory;
    private final PromptTemplate agentMatchingOrchestrationPromptTemplate;

    public MedicalAgentPromptSupportServiceImpl(
            ResourceLoader resourceLoader,
            @Value("${medexpertmatch.skills.directory:skills}") String skillsDirectory,
            @Qualifier("agentMatchingOrchestrationPromptTemplate") PromptTemplate agentMatchingOrchestrationPromptTemplate) {
        this.resourceLoader = resourceLoader;
        this.skillsDirectory = skillsDirectory;
        this.agentMatchingOrchestrationPromptTemplate = agentMatchingOrchestrationPromptTemplate;
    }

    @Override
    public String loadSkill(String skillName) {
        try {
            String skillPath = skillsDirectory + "/" + skillName + "/SKILL.md";
            Resource resource = resourceLoader.getResource("classpath:" + skillPath);
            if (!resource.exists()) {
                resource = resourceLoader.getResource("file:" + skillPath);
            }
            if (resource.exists()) {
                return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            }
            log.warn("Skill file not found: {}", skillPath);
            return "Skill instructions not available for: " + skillName;
        } catch (IOException e) {
            log.error("Failed to load skill: {}", skillName, e);
            return "Error loading skill: " + skillName;
        }
    }

    @Override
    public String buildPrompt(List<String> skills, String userRequest, Map<String, Object> requestParams) {
        StringBuilder skillsSection = new StringBuilder();
        for (String skill : skills) {
            skillsSection.append("---\n");
            skillsSection.append(skill);
            skillsSection.append("\n---\n\n");
        }

        String requestParametersSection = buildRequestParametersSection(requestParams);
        return agentMatchingOrchestrationPromptTemplate.render(Map.of(
                "skillsSection", skillsSection.toString(),
                "userRequest", userRequest,
                "requestParametersSection", requestParametersSection));
    }

    private String buildRequestParametersSection(Map<String, Object> requestParams) {
        if (requestParams == null || requestParams.isEmpty()) {
            return "";
        }
        StringBuilder section = new StringBuilder("Request Parameters:\n");
        requestParams.forEach((key, value) -> {
            if (!"sessionId".equals(key)) {
                section.append("- ").append(key).append(": ").append(value).append("\n");
            }
        });
        return section.append("\n").toString();
    }
}
