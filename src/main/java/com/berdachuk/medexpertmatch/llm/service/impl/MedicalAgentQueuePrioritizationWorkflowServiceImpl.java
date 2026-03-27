package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.core.util.LlmCallLimiter;
import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentLlmSupportService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentPromptSupportService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentQueuePrioritizationWorkflowService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Queue prioritization workflow extracted from the main medical agent service.
 */
@Slf4j
@Service
public class MedicalAgentQueuePrioritizationWorkflowServiceImpl implements MedicalAgentQueuePrioritizationWorkflowService {

    private static final int MAX_CASES_FOR_QUEUE = 20;
    private static final Pattern URGENCY_PATTERN = Pattern.compile("\"urgencyLevel\"\\s*:\\s*\"(CRITICAL|HIGH|MEDIUM|LOW)\"");
    private static final Pattern SPECIALTY_PATTERN = Pattern.compile("\"requiredSpecialty\"\\s*:\\s*\"([^\"]+)\"");

    private final ChatClient chatClient;
    private final String functionGemmaModelName;
    private final MedicalCaseRepository medicalCaseRepository;
    private final MedicalAgentLlmSupportService medicalAgentLlmSupportService;
    private final MedicalAgentPromptSupportService medicalAgentPromptSupportService;
    private final LogStreamService logStreamService;
    private final LlmCallLimiter llmCallLimiter;
    private final ObjectMapper objectMapper;

    public MedicalAgentQueuePrioritizationWorkflowServiceImpl(
            @Qualifier("medicalAgentChatClient") ChatClient chatClient,
            @Value("${spring.ai.custom.tool-calling.model:functiongemma}") String functionGemmaModelName,
            MedicalCaseRepository medicalCaseRepository,
            MedicalAgentLlmSupportService medicalAgentLlmSupportService,
            MedicalAgentPromptSupportService medicalAgentPromptSupportService,
            LogStreamService logStreamService,
            LlmCallLimiter llmCallLimiter,
            ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.functionGemmaModelName = functionGemmaModelName;
        this.medicalCaseRepository = medicalCaseRepository;
        this.medicalAgentLlmSupportService = medicalAgentLlmSupportService;
        this.medicalAgentPromptSupportService = medicalAgentPromptSupportService;
        this.logStreamService = logStreamService;
        this.llmCallLimiter = llmCallLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    public MedicalAgentService.AgentResponse prioritizeConsults(Map<String, Object> request) {
        log.info("prioritizeConsults() called");
        String sessionId = (String) request.getOrDefault("sessionId", "default");
        logStreamService.setCurrentSessionId(sessionId);

        @SuppressWarnings("unchecked")
        List<String> caseIds = (List<String>) request.get("caseIds");
        if (caseIds == null || caseIds.isEmpty()) {
            caseIds = medicalCaseRepository.findAllIds(MAX_CASES_FOR_QUEUE);
            log.info("No caseIds in request; loaded {} case IDs from database for queue prioritization", caseIds.size());
        }

        try {
            if (caseIds != null && !caseIds.isEmpty()) {
                logStreamService.sendLog(sessionId, "INFO", "LLM urgency analysis", "Analyzing urgency for " + caseIds.size() + " cases");

                List<CaseUrgencyEntry> entries = new ArrayList<>();
                for (String caseId : caseIds) {
                    try {
                        MedicalCase medicalCase = medicalCaseRepository.findById(caseId).orElse(null);
                        String caseAnalysis = medicalAgentLlmSupportService.analyzeCaseWithMedGemma(caseId);
                        entries.add(parseUrgencyFromAnalysis(caseId, caseAnalysis, medicalCase));
                    } catch (Exception e) {
                        log.warn("Could not analyze case {} for prioritization", caseId, e);
                        entries.add(parseUrgencyFromAnalysis(caseId, null, null));
                    }
                }

                entries.sort(Comparator
                        .comparing(CaseUrgencyEntry::urgencyLevel, Comparator.comparing(UrgencyLevel::valueOf, Comparator.comparingInt(Enum::ordinal)))
                        .thenComparing(CaseUrgencyEntry::caseId));

                String response = buildPrioritizationResponse(entries);
                log.info("Deterministic prioritization built for {} cases (no LLM for list)", entries.size());

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("skills", List.of("case-analyzer"));
                metadata.put("hybridApproach", true);
                metadata.put("llmUsed", true);
                metadata.put("deterministicOrder", true);
                return new MedicalAgentService.AgentResponse(response, metadata);
            }
        } catch (Exception e) {
            log.warn("Error in hybrid prioritizeConsults, falling back to standard approach", e);
        } finally {
            logStreamService.clearCurrentSessionId();
        }

        String caseAnalyzerSkill = medicalAgentPromptSupportService.loadSkill("case-analyzer");
        String userRequest = (caseIds != null && !caseIds.isEmpty())
                ? "Prioritize consultation queue based on case urgency and complexity."
                : "There are no cases in the consultation queue. Briefly describe how consultation queue prioritization would work when cases are present (by urgency and complexity). Do not invent specific case details or doctor names.";
        String prompt = medicalAgentPromptSupportService.buildPrompt(List.of(caseAnalyzerSkill), userRequest, request);

        log.info("Sending prompt to LLM for consult prioritization fallback (model: {}):\n{}", functionGemmaModelName, prompt);
        log.info("Calling LLM model: {} for consult prioritization (fallback)", functionGemmaModelName);
        String response = llmCallLimiter.execute(LlmClientType.TOOL_CALLING, () -> chatClient.prompt()
                .user(prompt)
                .call()
                .content());
        log.info("LLM model: {} completed consult prioritization (fallback), response length: {}",
                functionGemmaModelName, response != null ? response.length() : 0);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("skills", List.of("case-analyzer"));
        return new MedicalAgentService.AgentResponse(response, metadata);
    }

    private CaseUrgencyEntry parseUrgencyFromAnalysis(String caseId, String caseAnalysis, MedicalCase medicalCase) {
        String urgency = UrgencyLevel.MEDIUM.name();
        String specialty = "Unknown";
        String chiefComplaint = medicalCase != null ? medicalCase.chiefComplaint() : null;
        String symptoms = medicalCase != null ? medicalCase.symptoms() : null;
        Integer patientAge = medicalCase != null ? medicalCase.patientAge() : null;
        String caseSummary = null;

        if (caseAnalysis != null && !caseAnalysis.isBlank()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = objectMapper.readValue(caseAnalysis.trim(), Map.class);
                Object urgencyValue = map.get("urgencyLevel");
                if (urgencyValue != null) {
                    String urgencyText = urgencyValue.toString().toUpperCase();
                    if ("CRITICAL".equals(urgencyText) || "HIGH".equals(urgencyText)
                            || "MEDIUM".equals(urgencyText) || "LOW".equals(urgencyText)) {
                        urgency = urgencyText;
                    }
                }
                Object specialtyValue = map.get("requiredSpecialty");
                if (specialtyValue != null && !specialtyValue.toString().isEmpty()) {
                    specialty = specialtyValue.toString().trim();
                }
                Object caseSummaryValue = map.get("caseSummary");
                if (caseSummaryValue != null && !caseSummaryValue.toString().isEmpty()) {
                    caseSummary = caseSummaryValue.toString().trim();
                }
            } catch (Exception e) {
                Matcher urgencyMatcher = URGENCY_PATTERN.matcher(caseAnalysis);
                if (urgencyMatcher.find()) {
                    urgency = urgencyMatcher.group(1);
                }
                Matcher specialtyMatcher = SPECIALTY_PATTERN.matcher(caseAnalysis);
                if (specialtyMatcher.find()) {
                    specialty = specialtyMatcher.group(1).trim();
                }
            }
        }
        return new CaseUrgencyEntry(caseId, urgency, specialty, chiefComplaint, symptoms, patientAge, caseSummary);
    }

    private String buildPrioritizationResponse(List<CaseUrgencyEntry> entries) {
        StringBuilder response = new StringBuilder();
        response.append("Prioritized consultation queue (by urgency):\n\n");

        for (CaseUrgencyEntry entry : entries) {
            response.append("### Case ").append(entry.caseId()).append("\n");
            response.append("**Specialty:** ").append(entry.specialty()).append(" | **Urgency:** ").append(entry.urgencyLevel()).append("\n");
            if (entry.patientAge() != null) {
                response.append("**Patient Age:** ").append(entry.patientAge()).append("\n");
            }
            if (entry.chiefComplaint() != null && !entry.chiefComplaint().isBlank()) {
                response.append("**Chief Complaint:** ").append(entry.chiefComplaint()).append("\n");
            }
            if (entry.symptoms() != null && !entry.symptoms().isBlank()) {
                response.append("**Symptoms:** ").append(entry.symptoms()).append("\n");
            }
            if (entry.caseSummary() != null && !entry.caseSummary().isBlank()) {
                response.append("**Summary:** ").append(entry.caseSummary()).append("\n");
            }
            response.append("\n");
        }

        response.append("---\n\n");
        response.append("**Note:** This analysis is for research and educational purposes only. ");
        response.append("Do not use this system for diagnostic decisions without human-in-the-loop validation.");
        return response.toString();
    }
}
