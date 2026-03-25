package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.core.util.LlmCallLimiter;
import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentLlmSupportService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentPromptSupportService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentRecommendationWorkflowService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Recommendation workflow extracted from the main medical agent service.
 */
@Slf4j
@Service
public class MedicalAgentRecommendationWorkflowServiceImpl implements MedicalAgentRecommendationWorkflowService {

    private final ChatClient chatClient;
    private final String functionGemmaModelName;
    private final MedicalCaseRepository medicalCaseRepository;
    private final MedicalAgentLlmSupportService medicalAgentLlmSupportService;
    private final MedicalAgentPromptSupportService medicalAgentPromptSupportService;
    private final LogStreamService logStreamService;
    private final LlmCallLimiter llmCallLimiter;

    public MedicalAgentRecommendationWorkflowServiceImpl(
            @Qualifier("medicalAgentChatClient") ChatClient chatClient,
            @Value("${spring.ai.custom.tool-calling.model:functiongemma}") String functionGemmaModelName,
            MedicalCaseRepository medicalCaseRepository,
            MedicalAgentLlmSupportService medicalAgentLlmSupportService,
            MedicalAgentPromptSupportService medicalAgentPromptSupportService,
            LogStreamService logStreamService,
            LlmCallLimiter llmCallLimiter) {
        this.chatClient = chatClient;
        this.functionGemmaModelName = functionGemmaModelName;
        this.medicalCaseRepository = medicalCaseRepository;
        this.medicalAgentLlmSupportService = medicalAgentLlmSupportService;
        this.medicalAgentPromptSupportService = medicalAgentPromptSupportService;
        this.logStreamService = logStreamService;
        this.llmCallLimiter = llmCallLimiter;
    }

    @Override
    public MedicalAgentService.AgentResponse generateRecommendations(String matchId, Map<String, Object> request) {
        log.info("generateRecommendations() called - matchId: {}", matchId);
        String sessionId = (String) request.getOrDefault("sessionId", "default");
        logStreamService.setCurrentSessionId(sessionId);

        try {
            String caseId = (String) request.get("caseId");
            if (caseId == null && matchId != null) {
                caseId = matchId;
            }

            if (caseId != null) {
                String doctorMatcherSkill = medicalAgentPromptSupportService.loadSkill("doctor-matcher");
                String prompt = medicalAgentPromptSupportService.buildPrompt(
                        List.of(doctorMatcherSkill),
                        String.format("Generate expert recommendations for match %s. Use tools to get doctor and match details.", matchId),
                        request
                );

                log.info("Sending prompt to LLM for recommendation generation (model: {}, matchId: {}):\n{}",
                        functionGemmaModelName, matchId, prompt);
                log.info("Calling LLM model: {} for recommendation generation (matchId: {})", functionGemmaModelName, matchId);
                logStreamService.sendLog(sessionId, "INFO", "LLM Call",
                        String.format("Calling model: %s for recommendation generation", functionGemmaModelName));
                String toolResults = llmCallLimiter.execute(LlmClientType.TOOL_CALLING, () -> chatClient.prompt()
                        .user(prompt)
                        .call()
                        .content());
                log.info("LLM model: {} completed recommendation generation (matchId: {}), response length: {}",
                        functionGemmaModelName, matchId, toolResults != null ? toolResults.length() : 0);

                String caseAnalysis = medicalAgentLlmSupportService.analyzeCaseWithMedGemma(caseId);
                Integer patientAge = medicalCaseRepository.findById(caseId).map(MedicalCase::patientAge).orElse(null);
                String response = medicalAgentLlmSupportService.interpretResultsWithMedGemma(toolResults, caseAnalysis, patientAge);

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("matchId", matchId);
                metadata.put("skills", List.of("doctor-matcher"));
                metadata.put("hybridApproach", true);
                metadata.put("medgemmaUsed", true);
                return new MedicalAgentService.AgentResponse(response, metadata);
            }
        } catch (Exception e) {
            log.warn("Error in hybrid generateRecommendations, falling back to standard approach", e);
        } finally {
            logStreamService.clearCurrentSessionId();
        }

        String doctorMatcherSkill = medicalAgentPromptSupportService.loadSkill("doctor-matcher");
        String prompt = medicalAgentPromptSupportService.buildPrompt(
                List.of(doctorMatcherSkill),
                String.format("Generate expert recommendations for match %s.", matchId),
                request
        );

        log.info("Sending prompt to LLM for recommendation generation fallback (model: {}, matchId: {}):\n{}",
                functionGemmaModelName, matchId, prompt);
        log.info("Calling LLM model: {} for recommendation generation (fallback, matchId: {})", functionGemmaModelName, matchId);
        String response = llmCallLimiter.execute(LlmClientType.TOOL_CALLING, () -> chatClient.prompt()
                .user(prompt)
                .call()
                .content());
        log.info("LLM model: {} completed recommendation generation (fallback, matchId: {}), response length: {}",
                functionGemmaModelName, matchId, response != null ? response.length() : 0);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("matchId", matchId);
        metadata.put("skills", List.of("doctor-matcher"));
        return new MedicalAgentService.AgentResponse(response, metadata);
    }
}
