package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.llm.service.MedicalAgentPromptSupportService;
import lombok.extern.slf4j.Slf4j;
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

    public MedicalAgentPromptSupportServiceImpl(
            ResourceLoader resourceLoader,
            @Value("${medexpertmatch.skills.directory:skills}") String skillsDirectory) {
        this.resourceLoader = resourceLoader;
        this.skillsDirectory = skillsDirectory;
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
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("You are an expert matching assistant. Your role is to match healthcare specialists to medical cases.\n\n");
        promptBuilder.append("IMPORTANT: This is NOT a diagnostic system. Medical analysis is handled by MedGemma.\n");
        promptBuilder.append("Your task is to orchestrate tool calls to find matching doctors, not to provide medical diagnosis.\n\n");
        promptBuilder.append("Use the following guidance for expert matching:\n\n");

        for (String skill : skills) {
            promptBuilder.append("---\n");
            promptBuilder.append(skill);
            promptBuilder.append("\n---\n\n");
        }

        promptBuilder.append("Task: ").append(userRequest).append("\n");
        promptBuilder.append("Focus on matching specialists to cases, not on medical diagnosis.\n\n");

        if (requestParams != null && !requestParams.isEmpty()) {
            promptBuilder.append("Request Parameters:\n");
            requestParams.forEach((key, value) -> {
                if (!"sessionId".equals(key)) {
                    promptBuilder.append("- ").append(key).append(": ").append(value).append("\n");
                }
            });
            promptBuilder.append("\n");
        }

        promptBuilder.append("Use the available tools to find and match doctors. Provide a clear summary of matched specialists.\n\n");
        promptBuilder.append("CRITICAL OUTPUT LIMITS:\n");
        promptBuilder.append("- Provide EXACTLY ONE response and STOP after completing the task\n");
        promptBuilder.append("- Do NOT repeat the same content multiple times\n");
        promptBuilder.append("- Maximum response length: 2000 words (approximately 10000 characters)\n");
        promptBuilder.append("- Stop immediately after providing the response\n");
        promptBuilder.append("- Do NOT continue generating after the response is complete");

        return promptBuilder.toString();
    }
}
