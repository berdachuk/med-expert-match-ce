package com.berdachuk.medexpertmatch.llm.tools;

import com.berdachuk.medexpertmatch.caseanalysis.domain.CaseAnalysisResult;
import com.berdachuk.medexpertmatch.caseanalysis.service.CaseAnalysisService;
import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.llm.tools.support.AgentToolCaseIdValidator;
import com.berdachuk.medexpertmatch.llm.tools.support.AgentToolSessionSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Spring AI tools for medical case analysis.
 */
@Slf4j
@Component
public class CaseAnalysisAgentTools {

    private final CaseAnalysisService caseAnalysisService;
    private final MedicalCaseRepository medicalCaseRepository;
    private final LogStreamService logStreamService;

    public CaseAnalysisAgentTools(
            @Lazy CaseAnalysisService caseAnalysisService,
            MedicalCaseRepository medicalCaseRepository,
            LogStreamService logStreamService) {
        this.caseAnalysisService = caseAnalysisService;
        this.medicalCaseRepository = medicalCaseRepository;
        this.logStreamService = logStreamService;
    }

    @Tool(description = "Analyze a medical case by ID and extract key clinical information including chief complaint, symptoms, diagnosis, urgency level, and required specialty.")
    public CaseAnalysisResult analyze_case(
            @ToolParam(description = "Medical case ID - MUST be the exact case ID provided in the prompt (24-character hex string), not a placeholder or invented ID") String caseId
    ) {
        log.info("analyze_case() tool called - caseId: {}", caseId);
        String sessionId = AgentToolSessionSupport.getSessionId();
        String normalizedCaseId = AgentToolCaseIdValidator.requireValid(
                caseId, "analyze_case", logStreamService, sessionId);

        logStreamService.logToolCall(sessionId, "analyze_case",
                "caseId: " + caseId + " (normalized: " + normalizedCaseId + ")");

        try {
            MedicalCase medicalCase = medicalCaseRepository.findById(normalizedCaseId)
                    .orElseThrow(() -> new IllegalArgumentException("Medical case not found: " + caseId));

            CaseAnalysisResult result = caseAnalysisService.analyzeCase(medicalCase);
            logStreamService.logToolResult(sessionId, "analyze_case",
                    "Analysis completed: " + (result != null ? result.toString() : "null"));
            return result;
        } catch (Exception e) {
            logStreamService.logError(sessionId, "analyze_case failed", e.getMessage());
            throw e;
        }
    }

    @Tool(description = "Analyze unstructured medical case text and extract key clinical information including chief complaint, symptoms, diagnosis, urgency level, and required specialty.")
    public CaseAnalysisResult analyze_case_text(
            @ToolParam(description = "Unstructured case description text") String caseText
    ) {
        log.info("analyze_case_text() tool called");
        String sessionId = AgentToolSessionSupport.getSessionId();
        logStreamService.logToolCall(sessionId, "analyze_case_text",
                "caseText: " + (caseText != null && caseText.length() > 100
                        ? caseText.substring(0, 100) + "..." : caseText));

        try {
            CaseAnalysisResult result = caseAnalysisService.analyzeCase(medicalCaseFromText(caseText));
            logStreamService.logToolResult(sessionId, "analyze_case_text",
                    "Analysis completed: " + (result != null ? result.toString() : "null"));
            return result;
        } catch (Exception e) {
            logStreamService.logError(sessionId, "analyze_case_text failed", e.getMessage());
            throw e;
        }
    }

    @Tool(description = "Extract ICD-10 diagnosis codes from medical case text.")
    public List<String> extract_icd10_codes(
            @ToolParam(description = "Case description text") String caseText
    ) {
        log.info("extract_icd10_codes() tool called");
        return caseAnalysisService.extractICD10Codes(medicalCaseFromText(caseText));
    }

    @Tool(description = "Classify urgency level (CRITICAL, HIGH, MEDIUM, LOW) based on case text.")
    public UrgencyLevel classify_urgency(
            @ToolParam(description = "Case description text") String caseText
    ) {
        log.info("classify_urgency() tool called");
        return caseAnalysisService.classifyUrgency(medicalCaseFromText(caseText));
    }

    @Tool(description = "Determine required medical specialty based on case text.")
    public String determine_required_specialty(
            @ToolParam(description = "Case description text") String caseText
    ) {
        log.info("determine_required_specialty() tool called");
        List<String> specialties = caseAnalysisService.determineRequiredSpecialty(medicalCaseFromText(caseText));
        return specialties.isEmpty() ? null : specialties.get(0);
    }

    private static MedicalCase medicalCaseFromText(String caseText) {
        return new MedicalCase(
                null,
                null,
                caseText,
                null,
                null,
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                null);
    }
}
