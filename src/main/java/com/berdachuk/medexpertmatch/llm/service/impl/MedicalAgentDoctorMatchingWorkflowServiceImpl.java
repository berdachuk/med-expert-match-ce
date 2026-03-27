package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentDoctorMatchingWorkflowService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentLlmSupportService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.llm.tools.MedicalAgentTools;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Doctor matching workflow extracted from the main medical agent service.
 */
@Slf4j
@Service
public class MedicalAgentDoctorMatchingWorkflowServiceImpl implements MedicalAgentDoctorMatchingWorkflowService {

    private final MedicalAgentLlmSupportService medicalAgentLlmSupportService;
    private final MedicalCaseRepository medicalCaseRepository;
    private final LogStreamService logStreamService;
    private final MedicalAgentTools medicalAgentTools;
    private final ObjectMapper objectMapper;

    public MedicalAgentDoctorMatchingWorkflowServiceImpl(
            MedicalAgentLlmSupportService medicalAgentLlmSupportService,
            MedicalCaseRepository medicalCaseRepository,
            LogStreamService logStreamService,
            MedicalAgentTools medicalAgentTools,
            ObjectMapper objectMapper) {
        this.medicalAgentLlmSupportService = medicalAgentLlmSupportService;
        this.medicalCaseRepository = medicalCaseRepository;
        this.logStreamService = logStreamService;
        this.medicalAgentTools = medicalAgentTools;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MedicalAgentService.AgentResponse matchDoctors(String caseId, Map<String, Object> request) {
        log.info("matchDoctors() called - caseId: {}", caseId);
        String sessionId = (String) request.getOrDefault("sessionId", "default");
        log.info("Using sessionId: {} for log streaming", sessionId);
        logStreamService.setCurrentSessionId(sessionId);

        String response;
        try {
            logStreamService.logMatchDoctorsStep(sessionId, "Starting match doctors operation", "Case ID: " + caseId);
            logStreamService.sendProgress(sessionId, 5);

            logStreamService.sendLog(sessionId, "INFO", "Step 1: LLM case analysis", "Analyzing case with LLM");
            logStreamService.sendProgress(sessionId, 15);
            String caseAnalysisJson = medicalAgentLlmSupportService.analyzeCaseWithMedGemma(caseId);
            logStreamService.sendLog(sessionId, "INFO", "LLM analysis complete", "Case analysis received from LLM");
            logStreamService.sendProgress(sessionId, 35);

            logStreamService.sendProgress(sessionId, 45);
            Integer maxResults = (Integer) request.getOrDefault("maxResults", 10);
            List<DoctorMatch> matches = medicalAgentTools.match_doctors_to_case(caseId, maxResults, null, null, null);
            String jsonResponse = objectMapper.writeValueAsString(matches);
            logStreamService.sendProgress(sessionId, 55);

            Integer patientAge = medicalCaseRepository.findById(caseId).map(MedicalCase::patientAge).orElse(null);
            logStreamService.sendLog(sessionId, "INFO", "Step 3: LLM result interpretation", "Interpreting tool results with LLM");
            logStreamService.sendProgress(sessionId, 65);
            response = medicalAgentLlmSupportService.interpretResultsWithMedGemma(jsonResponse, caseAnalysisJson, patientAge);
            logStreamService.sendLog(sessionId, "INFO", "LLM interpretation complete", "Final response generated");
            logStreamService.sendProgress(sessionId, 85);
        } catch (Exception e) {
            log.error("Error in matchDoctors", e);
            logStreamService.logError(sessionId, "Unexpected error", e.getMessage() + "\n" + ExceptionUtils.getStackTrace(e));
            try {
                String caseAnalysis = medicalAgentLlmSupportService.analyzeCaseWithMedGemma(caseId);
                response = "Error during tool execution, but case analysis completed:\n\n" + caseAnalysis;
            } catch (Exception analysisError) {
                log.error("Could not get case analysis on error", analysisError);
                response = "Error during match operation. Please check logs for details.";
            }
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("caseId", caseId);
        metadata.put("skills", List.of("case-analyzer", "doctor-matcher"));
        metadata.put("hybridApproach", true);
        metadata.put("llmUsed", true);
        metadata.put("toolLlmUsed", false);

        logStreamService.sendProgress(sessionId, 100);
        logStreamService.logCompletion(sessionId, "Match doctors operation", "Successfully matched doctors for case: " + caseId);
        logStreamService.clearCurrentSessionId();

        return new MedicalAgentService.AgentResponse(response, metadata);
    }
}
