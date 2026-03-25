package com.berdachuk.medexpertmatch.llm.service;

import java.util.List;
import java.util.Map;

/**
 * Shared prompt and skill-loading support for medical agent workflows.
 */
public interface MedicalAgentPromptSupportService {

    /**
     * Loads skill instructions by skill name.
     *
     * @param skillName The skill directory name
     * @return Skill content or an explanatory placeholder
     */
    String loadSkill(String skillName);

    /**
     * Builds a prompt from skills, task text, and request parameters.
     *
     * @param skills Skill instructions to include
     * @param userRequest The workflow request
     * @param requestParams Additional request parameters
     * @return Prompt text for the tool-calling LLM
     */
    String buildPrompt(List<String> skills, String userRequest, Map<String, Object> requestParams);
}
