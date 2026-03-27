package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentCaseAnalysisWorkflowService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentLlmSupportService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.llm.tools.MedicalAgentTools;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Case analysis workflow extracted from the main medical agent service.
 */
@Slf4j
@Service
public class MedicalAgentCaseAnalysisWorkflowServiceImpl implements MedicalAgentCaseAnalysisWorkflowService {

    private final MedicalAgentLlmSupportService medicalAgentLlmSupportService;
    private final MedicalCaseRepository medicalCaseRepository;
    private final LogStreamService logStreamService;
    private final MedicalAgentTools medicalAgentTools;

    public MedicalAgentCaseAnalysisWorkflowServiceImpl(
            MedicalAgentLlmSupportService medicalAgentLlmSupportService,
            MedicalCaseRepository medicalCaseRepository,
            LogStreamService logStreamService,
            MedicalAgentTools medicalAgentTools) {
        this.medicalAgentLlmSupportService = medicalAgentLlmSupportService;
        this.medicalCaseRepository = medicalCaseRepository;
        this.logStreamService = logStreamService;
        this.medicalAgentTools = medicalAgentTools;
    }

    @Override
    public MedicalAgentService.AgentResponse analyzeCase(String caseId, Map<String, Object> request) {
        log.info("analyzeCase() called - caseId: {}", caseId);
        String sessionId = (String) request.getOrDefault("sessionId", "default");
        logStreamService.setCurrentSessionId(sessionId);

        try {
            logStreamService.sendLog(sessionId, "INFO", "MedGemma case analysis", "Starting comprehensive case analysis");
            String caseAnalysis = medicalAgentLlmSupportService.analyzeCaseWithMedGemma(caseId);

            String condition = null;
            String specialty = null;
            String pubmedQuery = null;
            Optional<MedicalCase> caseOpt = medicalCaseRepository.findById(caseId);
            if (caseOpt.isPresent()) {
                MedicalCase medicalCase = caseOpt.get();
                if (medicalCase.icd10Codes() != null && !medicalCase.icd10Codes().isEmpty()) {
                    condition = medicalCase.icd10Codes().get(0);
                }
                if (condition == null || condition.isBlank()) {
                    condition = medicalCase.currentDiagnosis() != null && !medicalCase.currentDiagnosis().isBlank()
                            ? medicalCase.currentDiagnosis() : medicalCase.chiefComplaint();
                }
                specialty = medicalCase.requiredSpecialty();
                pubmedQuery = medicalCase.chiefComplaint() != null && !medicalCase.chiefComplaint().isBlank()
                        ? medicalCase.chiefComplaint()
                        : (medicalCase.currentDiagnosis() != null ? medicalCase.currentDiagnosis() : condition);
            }
            if (condition == null || condition.isBlank()) {
                condition = "clinical case presentation";
            }
            if (pubmedQuery == null || pubmedQuery.isBlank()) {
                pubmedQuery = condition;
            }
            if (specialty == null || specialty.isBlank()) {
                specialty = "general";
            }

            final int evidenceMaxResults = 3;
            log.info("Case analysis evidence: condition={}, specialty={}, pubmedQuery={}, maxResults={}",
                    condition, specialty, pubmedQuery, evidenceMaxResults);
            logStreamService.sendLog(sessionId, "INFO", "Evidence retrieval", "Calling search_clinical_guidelines and query_pubmed");
            List<String> guidelines = medicalAgentTools.search_clinical_guidelines(condition, specialty, evidenceMaxResults);
            List<String> pubmedResults = medicalAgentTools.query_pubmed(pubmedQuery, evidenceMaxResults);
            int pubmedArticleCount = pubmedResults.size();
            if (pubmedResults.size() == 1 && pubmedResults.get(0) != null && pubmedResults.get(0).startsWith("No articles found")) {
                pubmedArticleCount = 0;
            }
            logStreamService.sendLog(sessionId, "INFO", "Evidence retrieval",
                    String.format("Evidence retrieved: search_clinical_guidelines (%d), query_pubmed (%d articles)",
                            guidelines.size(), pubmedArticleCount));

            StringBuilder evidenceBuilder = new StringBuilder();
            evidenceBuilder.append("=== Clinical guidelines (search_clinical_guidelines) ===\n");
            for (int i = 0; i < guidelines.size(); i++) {
                evidenceBuilder.append(i + 1).append(". ").append(guidelines.get(i)).append("\n");
            }
            evidenceBuilder.append("\n=== PubMed (query_pubmed) ===\n");
            for (int i = 0; i < pubmedResults.size(); i++) {
                evidenceBuilder.append(i + 1).append(". ").append(pubmedResults.get(i)).append("\n");
            }
            String toolResults = evidenceBuilder.toString();
            log.info("Case analysis evidence retrieved (caseId: {}), guidelines: {}, pubmed articles: {}",
                    caseId, guidelines.size(), pubmedArticleCount);

            Integer patientAge = medicalCaseRepository.findById(caseId).map(MedicalCase::patientAge).orElse(null);
            logStreamService.sendLog(sessionId, "INFO", "MedGemma result interpretation", "Interpreting analysis results");
            String response = medicalAgentLlmSupportService.interpretResultsWithMedGemma(toolResults, caseAnalysis, patientAge);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("caseId", caseId);
            metadata.put("skills", List.of("case-analyzer", "evidence-retriever", "recommendation-engine"));
            metadata.put("hybridApproach", true);
            metadata.put("medgemmaUsed", true);

            return new MedicalAgentService.AgentResponse(response, metadata);
        } catch (Exception e) {
            log.error("Error in hybrid analyzeCase", e);
            logStreamService.logError(sessionId, "Case analysis failed", e.getMessage());
            throw e;
        } finally {
            logStreamService.clearCurrentSessionId();
        }
    }
}
