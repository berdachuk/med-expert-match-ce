package com.berdachuk.medexpertmatch.llm.tools;

import com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience;
import com.berdachuk.medexpertmatch.clinicalexperience.repository.ClinicalExperienceRepository;
import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.llm.tools.support.AgentToolCaseIdValidator;
import com.berdachuk.medexpertmatch.llm.tools.support.AgentToolSessionSupport;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Spring AI tools for clinical recommendations, differential diagnosis, and risk assessment.
 */
@Slf4j
@Component
public class ClinicalAdvisorAgentTools {

    private final MedicalCaseRepository medicalCaseRepository;
    private final ClinicalExperienceRepository clinicalExperienceRepository;
    private final ChatClient medGemmaChatClient;
    private final LogStreamService logStreamService;
    private final EvidenceAgentTools evidenceAgentTools;
    private final PromptTemplate clinicalRecommendationsPromptTemplate;
    private final PromptTemplate clinicalRecommendationsTaskDiagnosticPromptTemplate;
    private final PromptTemplate clinicalRecommendationsTaskTreatmentPromptTemplate;
    private final PromptTemplate clinicalRecommendationsTaskFollowUpPromptTemplate;
    private final PromptTemplate clinicalRecommendationsTaskDefaultPromptTemplate;
    private final PromptTemplate clinicalRecommendationsEvidenceInstructionPromptTemplate;
    private final PromptTemplate differentialDiagnosisPromptTemplate;
    private final PromptTemplate riskAssessmentPromptTemplate;
    private final PromptTemplate riskAssessmentTaskComplicationPromptTemplate;
    private final PromptTemplate riskAssessmentTaskMortalityPromptTemplate;
    private final PromptTemplate riskAssessmentTaskReadmissionPromptTemplate;
    private final PromptTemplate riskAssessmentTaskDefaultPromptTemplate;

    public ClinicalAdvisorAgentTools(
            MedicalCaseRepository medicalCaseRepository,
            ClinicalExperienceRepository clinicalExperienceRepository,
            @Qualifier("caseAnalysisChatClient") ChatClient medGemmaChatClient,
            LogStreamService logStreamService,
            @Lazy EvidenceAgentTools evidenceAgentTools,
            @Qualifier("clinicalRecommendationsPromptTemplate") PromptTemplate clinicalRecommendationsPromptTemplate,
            @Qualifier("clinicalRecommendationsTaskDiagnosticPromptTemplate") PromptTemplate clinicalRecommendationsTaskDiagnosticPromptTemplate,
            @Qualifier("clinicalRecommendationsTaskTreatmentPromptTemplate") PromptTemplate clinicalRecommendationsTaskTreatmentPromptTemplate,
            @Qualifier("clinicalRecommendationsTaskFollowUpPromptTemplate") PromptTemplate clinicalRecommendationsTaskFollowUpPromptTemplate,
            @Qualifier("clinicalRecommendationsTaskDefaultPromptTemplate") PromptTemplate clinicalRecommendationsTaskDefaultPromptTemplate,
            @Qualifier("clinicalRecommendationsEvidenceInstructionPromptTemplate") PromptTemplate clinicalRecommendationsEvidenceInstructionPromptTemplate,
            @Qualifier("differentialDiagnosisPromptTemplate") PromptTemplate differentialDiagnosisPromptTemplate,
            @Qualifier("riskAssessmentPromptTemplate") PromptTemplate riskAssessmentPromptTemplate,
            @Qualifier("riskAssessmentTaskComplicationPromptTemplate") PromptTemplate riskAssessmentTaskComplicationPromptTemplate,
            @Qualifier("riskAssessmentTaskMortalityPromptTemplate") PromptTemplate riskAssessmentTaskMortalityPromptTemplate,
            @Qualifier("riskAssessmentTaskReadmissionPromptTemplate") PromptTemplate riskAssessmentTaskReadmissionPromptTemplate,
            @Qualifier("riskAssessmentTaskDefaultPromptTemplate") PromptTemplate riskAssessmentTaskDefaultPromptTemplate) {
        this.medicalCaseRepository = medicalCaseRepository;
        this.clinicalExperienceRepository = clinicalExperienceRepository;
        this.medGemmaChatClient = medGemmaChatClient;
        this.logStreamService = logStreamService;
        this.evidenceAgentTools = evidenceAgentTools;
        this.clinicalRecommendationsPromptTemplate = clinicalRecommendationsPromptTemplate;
        this.clinicalRecommendationsTaskDiagnosticPromptTemplate = clinicalRecommendationsTaskDiagnosticPromptTemplate;
        this.clinicalRecommendationsTaskTreatmentPromptTemplate = clinicalRecommendationsTaskTreatmentPromptTemplate;
        this.clinicalRecommendationsTaskFollowUpPromptTemplate = clinicalRecommendationsTaskFollowUpPromptTemplate;
        this.clinicalRecommendationsTaskDefaultPromptTemplate = clinicalRecommendationsTaskDefaultPromptTemplate;
        this.clinicalRecommendationsEvidenceInstructionPromptTemplate = clinicalRecommendationsEvidenceInstructionPromptTemplate;
        this.differentialDiagnosisPromptTemplate = differentialDiagnosisPromptTemplate;
        this.riskAssessmentPromptTemplate = riskAssessmentPromptTemplate;
        this.riskAssessmentTaskComplicationPromptTemplate = riskAssessmentTaskComplicationPromptTemplate;
        this.riskAssessmentTaskMortalityPromptTemplate = riskAssessmentTaskMortalityPromptTemplate;
        this.riskAssessmentTaskReadmissionPromptTemplate = riskAssessmentTaskReadmissionPromptTemplate;
        this.riskAssessmentTaskDefaultPromptTemplate = riskAssessmentTaskDefaultPromptTemplate;
    }

    @Tool(description = "Generate clinical recommendations for a medical case (diagnostic, treatment, or follow-up).")
    public String generate_recommendations(
            @ToolParam(description = "Medical case ID - MUST be the exact case ID provided in the prompt (24-character hex string), not a placeholder or invented ID") String caseId,
            @ToolParam(description = "Type of recommendation: DIAGNOSTIC, TREATMENT, or FOLLOW_UP") String recommendationType,
            @ToolParam(description = "Include evidence citations (default: true)") Boolean includeEvidence
    ) {
        log.info("generate_recommendations() tool called - caseId: {}, recommendationType: {}, includeEvidence: {}",
                caseId, recommendationType, includeEvidence);
        String sessionId = AgentToolSessionSupport.getSessionId();
        logStreamService.logToolCall(sessionId, "generate_recommendations",
                String.format("caseId: %s, recommendationType: %s, includeEvidence: %s",
                        caseId, recommendationType, includeEvidence));

        try {
            if (AgentToolCaseIdValidator.normalizeIfValid(caseId).isEmpty()) {
                String errorMsg = String.format(
                        "Invalid case ID format: '%s' (expected 24-character hex string, got %d characters)",
                        caseId, caseId != null ? caseId.length() : "null");
                log.error(errorMsg);
                logStreamService.logError(sessionId, "generate_recommendations validation failed", errorMsg);
                return "Error: " + errorMsg;
            }

            Optional<MedicalCase> caseOpt = medicalCaseRepository.findById(caseId);
            if (caseOpt.isEmpty()) {
                log.warn("Medical case not found - caseId: {}", caseId);
                logStreamService.logError(sessionId, "generate_recommendations failed",
                        "Medical case not found: " + caseId);
                return "Error: Medical case not found: " + caseId;
            }

            MedicalCase medicalCase = caseOpt.get();
            String normalizedType = recommendationType != null ? recommendationType.toUpperCase() : "DIAGNOSTIC";
            boolean includeEvidenceFlag = includeEvidence != null ? includeEvidence : true;

            StringBuilder caseContext = new StringBuilder();
            caseContext.append("Chief Complaint: ")
                    .append(medicalCase.chiefComplaint() != null ? medicalCase.chiefComplaint() : "N/A")
                    .append("\n");
            caseContext.append("Symptoms: ")
                    .append(medicalCase.symptoms() != null ? medicalCase.symptoms() : "N/A")
                    .append("\n");
            if (medicalCase.icd10Codes() != null && !medicalCase.icd10Codes().isEmpty()) {
                caseContext.append("ICD-10 Codes: ").append(String.join(", ", medicalCase.icd10Codes())).append("\n");
            }
            if (medicalCase.requiredSpecialty() != null) {
                caseContext.append("Required Specialty: ").append(medicalCase.requiredSpecialty()).append("\n");
            }
            if (medicalCase.additionalNotes() != null && !medicalCase.additionalNotes().isEmpty()) {
                caseContext.append("Additional Notes: ").append(medicalCase.additionalNotes()).append("\n");
            }

            String evidenceSection = "";
            if (includeEvidenceFlag) {
                evidenceSection = "\n\nEvidence from Clinical Guidelines and Literature:\n";
                if (medicalCase.icd10Codes() != null && !medicalCase.icd10Codes().isEmpty()) {
                    for (String icd10Code : medicalCase.icd10Codes()) {
                        List<String> guidelines = evidenceAgentTools.search_clinical_guidelines(
                                icd10Code, medicalCase.requiredSpecialty(), 3);
                        if (!guidelines.isEmpty()) {
                            evidenceSection += "Guidelines for " + icd10Code + ":\n";
                            for (String guideline : guidelines) {
                                evidenceSection += "- " + guideline + "\n";
                            }
                        }
                    }
                }
                String pubmedQuery = medicalCase.chiefComplaint() != null ? medicalCase.chiefComplaint() :
                        (medicalCase.icd10Codes() != null && !medicalCase.icd10Codes().isEmpty() ?
                                medicalCase.icd10Codes().get(0) : "");
                if (!pubmedQuery.isEmpty()) {
                    List<String> articles = evidenceAgentTools.query_pubmed(pubmedQuery, 3);
                    if (!articles.isEmpty()) {
                        evidenceSection += "\nRelevant Literature:\n";
                        for (String article : articles) {
                            evidenceSection += article + "\n\n";
                        }
                    }
                }
            }

            String taskInstructions = renderRecommendationTaskInstructions(normalizedType);
            String evidenceInstruction = "";
            if (includeEvidenceFlag && !evidenceSection.isEmpty()) {
                evidenceInstruction = clinicalRecommendationsEvidenceInstructionPromptTemplate.render(Collections.emptyMap());
            }

            String prompt = clinicalRecommendationsPromptTemplate.render(Map.of(
                    "caseContext", caseContext.toString(),
                    "taskInstructions", taskInstructions,
                    "evidenceSection", evidenceSection,
                    "evidenceInstruction", evidenceInstruction));

            log.info("Sending prompt to LLM for recommendations (model: LLM, caseId: {}, type: {}):\n{}",
                    caseId, normalizedType, prompt);
            log.info("Calling LLM for recommendations - caseId: {}, type: {}", caseId, normalizedType);
            String responseText = medGemmaChatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (responseText == null || responseText.trim().isEmpty()) {
                log.warn("Empty response from LLM for recommendations");
                logStreamService.logToolResult(sessionId, "generate_recommendations",
                        "No recommendations generated");
                return "No recommendations could be generated for case: " + caseId;
            }

            logStreamService.logToolResult(sessionId, "generate_recommendations",
                    String.format("Generated %s recommendations", normalizedType));
            return responseText;
        } catch (Exception e) {
            log.error("Error generating recommendations", e);
            logStreamService.logError(sessionId, "generate_recommendations failed", e.getMessage());
            return "Error generating recommendations: " + e.getMessage();
        }
    }

    @Tool(description = "Generate differential diagnosis list based on symptoms and clinical findings.")
    public String differential_diagnosis(
            @ToolParam(description = "Medical case ID - MUST be the exact case ID provided in the prompt (24-character hex string), not a placeholder or invented ID") String caseId,
            @ToolParam(description = "Maximum number of diagnoses (default: 10)") Integer maxResults
    ) {
        log.info("differential_diagnosis() tool called - caseId: {}, maxResults: {}", caseId, maxResults);
        String sessionId = AgentToolSessionSupport.getSessionId();
        logStreamService.logToolCall(sessionId, "differential_diagnosis",
                String.format("caseId: %s, maxResults: %s", caseId, maxResults));

        try {
            if (AgentToolCaseIdValidator.normalizeIfValid(caseId).isEmpty()) {
                String errorMsg = String.format(
                        "Invalid case ID format: '%s' (expected 24-character hex string, got %d characters)",
                        caseId, caseId != null ? caseId.length() : "null");
                log.error(errorMsg);
                logStreamService.logError(sessionId, "differential_diagnosis validation failed", errorMsg);
                return "Error: " + errorMsg;
            }

            Optional<MedicalCase> caseOpt = medicalCaseRepository.findById(caseId);
            if (caseOpt.isEmpty()) {
                log.warn("Medical case not found - caseId: {}", caseId);
                logStreamService.logError(sessionId, "differential_diagnosis failed",
                        "Medical case not found: " + caseId);
                return "Error: Medical case not found: " + caseId;
            }

            MedicalCase medicalCase = caseOpt.get();
            int limit = (maxResults != null && maxResults > 0) ? maxResults : 10;

            StringBuilder caseContext = new StringBuilder();
            caseContext.append("Chief Complaint: ")
                    .append(medicalCase.chiefComplaint() != null ? medicalCase.chiefComplaint() : "N/A")
                    .append("\n");
            caseContext.append("Symptoms: ")
                    .append(medicalCase.symptoms() != null ? medicalCase.symptoms() : "N/A")
                    .append("\n");
            if (medicalCase.currentDiagnosis() != null && !medicalCase.currentDiagnosis().isEmpty()) {
                caseContext.append("Current Diagnosis: ").append(medicalCase.currentDiagnosis()).append("\n");
            }
            if (medicalCase.icd10Codes() != null && !medicalCase.icd10Codes().isEmpty()) {
                caseContext.append("ICD-10 Codes: ").append(String.join(", ", medicalCase.icd10Codes())).append("\n");
            }
            if (medicalCase.additionalNotes() != null && !medicalCase.additionalNotes().isEmpty()) {
                caseContext.append("Additional Notes: ").append(medicalCase.additionalNotes()).append("\n");
            }

            String prompt = differentialDiagnosisPromptTemplate.render(Map.of(
                    "caseContext", caseContext.toString(),
                    "limit", String.valueOf(limit)));

            log.info("Sending prompt to LLM for differential diagnosis (model: LLM, caseId: {}):\n{}", caseId, prompt);
            log.info("Calling LLM for differential diagnosis - caseId: {}", caseId);
            String responseText = medGemmaChatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (responseText == null || responseText.trim().isEmpty()) {
                log.warn("Empty response from LLM for differential diagnosis");
                logStreamService.logToolResult(sessionId, "differential_diagnosis",
                        "No differential diagnosis generated");
                return "No differential diagnosis could be generated for case: " + caseId;
            }

            logStreamService.logToolResult(sessionId, "differential_diagnosis",
                    "Generated differential diagnosis");
            return responseText;
        } catch (Exception e) {
            log.error("Error generating differential diagnosis", e);
            logStreamService.logError(sessionId, "differential_diagnosis failed", e.getMessage());
            return "Error generating differential diagnosis: " + e.getMessage();
        }
    }

    @Tool(description = "Assess patient risk factors and complications for a medical case.")
    public String risk_assessment(
            @ToolParam(description = "Medical case ID") String caseId,
            @ToolParam(description = "Type of risk assessment: COMPLICATION, MORTALITY, READMISSION, etc.") String riskType
    ) {
        log.info("risk_assessment() tool called - caseId: {}, riskType: {}", caseId, riskType);
        String sessionId = AgentToolSessionSupport.getSessionId();
        logStreamService.logToolCall(sessionId, "risk_assessment",
                String.format("caseId: %s, riskType: %s", caseId, riskType));

        try {
            Optional<MedicalCase> caseOpt = medicalCaseRepository.findById(caseId);
            if (caseOpt.isEmpty()) {
                log.warn("Medical case not found - caseId: {}", caseId);
                logStreamService.logError(sessionId, "risk_assessment failed", "Medical case not found: " + caseId);
                return "Error: Medical case not found: " + caseId;
            }

            MedicalCase medicalCase = caseOpt.get();
            String normalizedRiskType = riskType != null ? riskType.toUpperCase() : "COMPLICATION";

            StringBuilder caseContext = new StringBuilder();
            if (medicalCase.patientAge() != null) {
                caseContext.append("Patient Age: ").append(medicalCase.patientAge()).append("\n");
            }
            caseContext.append("Chief Complaint: ")
                    .append(medicalCase.chiefComplaint() != null ? medicalCase.chiefComplaint() : "N/A")
                    .append("\n");
            caseContext.append("Symptoms: ")
                    .append(medicalCase.symptoms() != null ? medicalCase.symptoms() : "N/A")
                    .append("\n");
            if (medicalCase.urgencyLevel() != null) {
                caseContext.append("Urgency Level: ").append(medicalCase.urgencyLevel()).append("\n");
            }
            if (medicalCase.icd10Codes() != null && !medicalCase.icd10Codes().isEmpty()) {
                caseContext.append("ICD-10 Codes: ").append(String.join(", ", medicalCase.icd10Codes())).append("\n");
            }
            if (medicalCase.additionalNotes() != null && !medicalCase.additionalNotes().isEmpty()) {
                caseContext.append("Additional Notes: ").append(medicalCase.additionalNotes()).append("\n");
            }

            String historicalData = "";
            if (medicalCase.icd10Codes() != null && !medicalCase.icd10Codes().isEmpty()) {
                List<MedicalCase> similarCases = medicalCaseRepository.findByIcd10Code(
                        medicalCase.icd10Codes().get(0), 10);
                if (!similarCases.isEmpty()) {
                    List<String> caseIds = similarCases.stream().map(MedicalCase::id).toList();
                    Map<String, List<ClinicalExperience>> experiencesByCase =
                            clinicalExperienceRepository.findByCaseIds(caseIds);

                    int totalCases = experiencesByCase.size();
                    int complicatedCases = 0;
                    for (List<ClinicalExperience> experiences : experiencesByCase.values()) {
                        for (ClinicalExperience exp : experiences) {
                            if ("COMPLICATED".equalsIgnoreCase(exp.outcome()) ||
                                    exp.complications() != null && !exp.complications().isEmpty()) {
                                complicatedCases++;
                                break;
                            }
                        }
                    }

                    if (totalCases > 0) {
                        double complicationRate = (double) complicatedCases / totalCases * 100;
                        historicalData = String.format(
                                "\nHistorical Data: Based on %d similar cases, complication rate: %.1f%%\n",
                                totalCases, complicationRate);
                    }
                }
            }

            String taskInstructions = renderRiskAssessmentTaskInstructions(normalizedRiskType);
            String prompt = riskAssessmentPromptTemplate.render(Map.of(
                    "caseContext", caseContext.toString(),
                    "historicalData", historicalData,
                    "taskInstructions", taskInstructions));

            log.info("Sending prompt to LLM for risk assessment (model: LLM, caseId: {}, type: {}):\n{}",
                    caseId, normalizedRiskType, prompt);
            log.info("Calling LLM for risk assessment - caseId: {}, type: {}", caseId, normalizedRiskType);
            String responseText = medGemmaChatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (responseText == null || responseText.trim().isEmpty()) {
                log.warn("Empty response from LLM for risk assessment");
                logStreamService.logToolResult(sessionId, "risk_assessment",
                        "No risk assessment generated");
                return "No risk assessment could be generated for case: " + caseId;
            }

            logStreamService.logToolResult(sessionId, "risk_assessment",
                    String.format("Generated %s risk assessment", normalizedRiskType));
            return responseText;
        } catch (Exception e) {
            log.error("Error generating risk assessment", e);
            logStreamService.logError(sessionId, "risk_assessment failed", e.getMessage());
            return "Error generating risk assessment: " + e.getMessage();
        }
    }

    private String renderRecommendationTaskInstructions(String recommendationType) {
        return switch (recommendationType) {
            case "DIAGNOSTIC" -> clinicalRecommendationsTaskDiagnosticPromptTemplate.render(Collections.emptyMap());
            case "TREATMENT" -> clinicalRecommendationsTaskTreatmentPromptTemplate.render(Collections.emptyMap());
            case "FOLLOW_UP" -> clinicalRecommendationsTaskFollowUpPromptTemplate.render(Collections.emptyMap());
            default -> clinicalRecommendationsTaskDefaultPromptTemplate.render(Collections.emptyMap());
        };
    }

    private String renderRiskAssessmentTaskInstructions(String riskType) {
        return switch (riskType) {
            case "COMPLICATION" -> riskAssessmentTaskComplicationPromptTemplate.render(Collections.emptyMap());
            case "MORTALITY" -> riskAssessmentTaskMortalityPromptTemplate.render(Collections.emptyMap());
            case "READMISSION" -> riskAssessmentTaskReadmissionPromptTemplate.render(Collections.emptyMap());
            default -> riskAssessmentTaskDefaultPromptTemplate.render(Collections.emptyMap());
        };
    }
}
