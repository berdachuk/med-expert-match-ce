package com.berdachuk.medexpertmatch.llm.tools;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentCaseIntakeWorkflowService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.llm.tool.MatchToolParameterSanitizer;
import com.berdachuk.medexpertmatch.llm.tools.support.AgentToolCaseIdValidator;
import com.berdachuk.medexpertmatch.llm.tools.support.AgentToolSessionSupport;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;
import com.berdachuk.medexpertmatch.retrieval.domain.MatchOptions;
import com.berdachuk.medexpertmatch.retrieval.domain.ScoreResult;
import com.berdachuk.medexpertmatch.retrieval.service.MatchingService;
import com.berdachuk.medexpertmatch.retrieval.service.SemanticGraphRetrievalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Spring AI tools for doctor matching and scoring.
 */
@Slf4j
@Component
public class DoctorMatchingAgentTools {

    private final MedicalCaseRepository medicalCaseRepository;
    private final DoctorRepository doctorRepository;
    private final MatchingService matchingService;
    private final SemanticGraphRetrievalService semanticGraphRetrievalService;
    private final LogStreamService logStreamService;
    private final MedicalAgentCaseIntakeWorkflowService caseIntakeWorkflowService;

    public DoctorMatchingAgentTools(
            MedicalCaseRepository medicalCaseRepository,
            DoctorRepository doctorRepository,
            @Lazy MatchingService matchingService,
            @Lazy SemanticGraphRetrievalService semanticGraphRetrievalService,
            LogStreamService logStreamService,
            @Lazy MedicalAgentCaseIntakeWorkflowService caseIntakeWorkflowService) {
        this.medicalCaseRepository = medicalCaseRepository;
        this.doctorRepository = doctorRepository;
        this.matchingService = matchingService;
        this.semanticGraphRetrievalService = semanticGraphRetrievalService;
        this.logStreamService = logStreamService;
        this.caseIntakeWorkflowService = caseIntakeWorkflowService;
    }

    @Tool(description = """
            Match doctors from anonymized case text when NO existing case ID is available.
            Creates a persisted case, runs GraphRAG matching, and returns a narrative summary.
            Use this instead of match_doctors_to_case when the user describes a case without a 24-character case ID.""")
    public String match_doctors_from_text(
            @ToolParam(description = "Full anonymized clinical case description") String caseText,
            @ToolParam(description = "Patient age in years (optional)") Integer patientAge,
            @ToolParam(description = "Case type: INPATIENT, OUTPATIENT, EMERGENCY, SECOND_OPINION (optional)") String caseType,
            @ToolParam(description = "Urgency: LOW, MEDIUM, HIGH, CRITICAL (optional)") String urgencyLevel,
            @ToolParam(description = "Maximum number of matches (default: 10)") Integer maxResults
    ) {
        if (caseText == null || caseText.isBlank()) {
            throw new IllegalArgumentException("caseText is required and cannot be empty");
        }
        String sessionId = AgentToolSessionSupport.getSessionId() != null
                ? AgentToolSessionSupport.getSessionId() : "default";
        logStreamService.logToolCall(sessionId, "match_doctors_from_text",
                "caseText length: " + caseText.length());
        Map<String, Object> request = new HashMap<>();
        request.put("sessionId", sessionId);
        if (patientAge != null) {
            request.put("patientAge", patientAge);
        }
        if (caseType != null && !caseType.isBlank()) {
            request.put("caseType", caseType.trim());
        }
        if (urgencyLevel != null && !urgencyLevel.isBlank()) {
            request.put("urgencyLevel", urgencyLevel.trim());
        }
        if (maxResults != null) {
            request.put("maxResults", maxResults);
        }
        try {
            MedicalAgentService.AgentResponse response =
                    caseIntakeWorkflowService.matchFromText(caseText.trim(), request);
            String summary = response.response();
            if (response.metadata() != null && response.metadata().get("caseId") != null) {
                summary = summary + "\n\nCreated case ID: " + response.metadata().get("caseId");
            }
            logStreamService.logToolResult(sessionId, "match_doctors_from_text", "Match workflow completed");
            return summary;
        } catch (Exception e) {
            logStreamService.logError(sessionId, "match_doctors_from_text failed", e.getMessage());
            throw e;
        }
    }

    @Tool(description = "Query candidate doctors based on case requirements and filters (specialty, telehealth, etc.). NOTE: This is a simple database query WITHOUT scoring or graph relationships. For matching with scoring and graph analysis, use match_doctors_to_case instead. IMPORTANT: Use the exact case ID provided in the prompt - do NOT invent or generate a different ID.")
    public List<Doctor> query_candidate_doctors(
            @ToolParam(description = "Medical case ID - MUST be the exact case ID provided in the prompt (24-character hex string), not a placeholder or invented ID") String caseId,
            @ToolParam(description = "Required medical specialty (optional)") String specialty,
            @ToolParam(description = "Require telehealth capability (optional)") Boolean requireTelehealth,
            @ToolParam(description = "Maximum number of results (default: 10)") Integer maxResults
    ) {
        log.info("query_candidate_doctors() tool called - caseId: {}, specialty: {}, requireTelehealth: {}, maxResults: {}",
                caseId, specialty, requireTelehealth, maxResults);
        String sessionId = AgentToolSessionSupport.getSessionId();
        logStreamService.logToolCall(sessionId, "query_candidate_doctors",
                String.format("caseId: %s, specialty: %s, requireTelehealth: %s, maxResults: %s",
                        caseId, specialty, requireTelehealth, maxResults));

        try {
            if (maxResults == null || maxResults <= 0) {
                maxResults = 10;
            }

            String requiredSpecialty = specialty;

            if (caseId != null && !caseId.isEmpty()) {
                if (AgentToolCaseIdValidator.normalizeIfValid(caseId).isEmpty()) {
                    String errorMsg = String.format(
                            "Invalid case ID format: '%s' (expected 24-character hex string, got %d characters)",
                            caseId, caseId.length());
                    log.error(errorMsg);
                    logStreamService.logError(sessionId, "query_candidate_doctors validation failed", errorMsg);
                    throw new IllegalArgumentException(errorMsg);
                }

                log.info("Case ID validation passed - original: '{}', length: {}, hex format: valid",
                        caseId, caseId.length());

                Optional<MedicalCase> caseOpt = medicalCaseRepository.findById(caseId);
                if (caseOpt.isPresent()) {
                    MedicalCase medicalCase = caseOpt.get();
                    if (requiredSpecialty == null || requiredSpecialty.isEmpty()) {
                        requiredSpecialty = medicalCase.requiredSpecialty();
                    }
                } else {
                    log.warn("Case not found: {}", caseId);
                    logStreamService.logToolResult(sessionId, "query_candidate_doctors", "Case not found: " + caseId);
                }
            }

            if (requiredSpecialty != null && !requiredSpecialty.isEmpty()) {
                List<Doctor> doctors = doctorRepository.findBySpecialty(requiredSpecialty, maxResults);
                if (Boolean.TRUE.equals(requireTelehealth)) {
                    doctors = doctors.stream()
                            .filter(Doctor::telehealthEnabled)
                            .toList();
                }
                logStreamService.logToolResult(sessionId, "query_candidate_doctors",
                        String.format("Found %d doctors for specialty: %s", doctors.size(), requiredSpecialty));
                return doctors;
            }

            List<String> doctorIds = doctorRepository.findAllIds(maxResults);
            List<Doctor> doctors = doctorRepository.findByIds(doctorIds);

            if (Boolean.TRUE.equals(requireTelehealth)) {
                doctors = doctors.stream()
                        .filter(Doctor::telehealthEnabled)
                        .toList();
            }

            logStreamService.logToolResult(sessionId, "query_candidate_doctors",
                    String.format("Found %d doctors (no specialty filter)", doctors.size()));
            return doctors;
        } catch (Exception e) {
            logStreamService.logError(sessionId, "query_candidate_doctors failed", e.getMessage());
            log.error("Error querying candidate doctors", e);
            throw e;
        }
    }

    @Tool(description = "Score a doctor-case match using multiple signals (vector similarity, graph relationships, historical performance).")
    public ScoreResult score_doctor_match(
            @ToolParam(description = "Medical case ID - MUST be the exact case ID provided in the prompt (24-character hex string), not a placeholder or invented ID") String caseId,
            @ToolParam(description = "Doctor ID") String doctorId
    ) {
        log.info("score_doctor_match() tool called - caseId: {}, doctorId: {}", caseId, doctorId);

        String normalizedCaseId = AgentToolCaseIdValidator.requireValid(caseId);

        Optional<MedicalCase> caseOpt = medicalCaseRepository.findById(normalizedCaseId);
        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);

        if (caseOpt.isEmpty() || doctorOpt.isEmpty()) {
            log.warn("Case or doctor not found - caseId: {}, doctorId: {}", caseId, doctorId);
            throw new IllegalArgumentException("Case or doctor not found");
        }

        return semanticGraphRetrievalService.score(caseOpt.get(), doctorOpt.get());
    }

    @Tool(description = "Match doctors to a medical case with comprehensive scoring and ranking using multiple signals: vector similarity (embeddings), graph relationships (Apache AGE), historical performance, specialty matching, and telehealth availability. Requires an EXISTING 24-character case ID. If no case ID is available, use match_doctors_from_text instead.")
    public List<DoctorMatch> match_doctors_to_case(
            @ToolParam(description = "Medical case ID - MUST be the exact case ID provided in the prompt (24-character hex string), not a placeholder or invented ID") String caseId,
            @ToolParam(description = "Maximum number of results (default: 10)") Integer maxResults,
            @ToolParam(description = "Minimum match score threshold (0-100, optional)") Double minScore,
            @ToolParam(description = "List of preferred medical specialties (e.g. Surgery, Cardiology). Do NOT pass agent or skill names.") List<String> preferredSpecialties,
            @ToolParam(description = "Require telehealth capability (optional)") Boolean requireTelehealth
    ) {
        String sessionId = AgentToolSessionSupport.getSessionId();
        String normalizedCaseId = AgentToolCaseIdValidator.requireValid(
                caseId, "match_doctors_to_case", logStreamService, sessionId);

        log.info("match_doctors_to_case() tool called - caseId: {} (normalized: {}), maxResults: {}, minScore: {}, preferredSpecialties: {}, requireTelehealth: {}",
                caseId, normalizedCaseId, maxResults, minScore, preferredSpecialties, requireTelehealth);
        List<String> sanitizedSpecialties = MatchToolParameterSanitizer.sanitizePreferredSpecialties(preferredSpecialties);
        Boolean sanitizedTelehealth = MatchToolParameterSanitizer.sanitizeRequireTelehealth(
                preferredSpecialties, requireTelehealth);
        if (sanitizedSpecialties != preferredSpecialties || sanitizedTelehealth != requireTelehealth) {
            log.warn("Sanitized match_doctors_to_case filters — preferredSpecialties: {} -> {}, requireTelehealth: {} -> {}",
                    preferredSpecialties, sanitizedSpecialties, requireTelehealth, sanitizedTelehealth);
        }
        String params = String.format("caseId: %s (normalized: %s), maxResults: %s, minScore: %s",
                caseId, normalizedCaseId, maxResults, minScore);
        logStreamService.logToolCall(sessionId, "match_doctors_to_case", params);

        try {
            MatchOptions options = MatchOptions.builder()
                    .maxResults(maxResults != null && maxResults > 0 ? maxResults : 10)
                    .minScore(minScore)
                    .preferredSpecialties(sanitizedSpecialties)
                    .requireTelehealth(sanitizedTelehealth)
                    .build();

            List<DoctorMatch> result = matchingService.matchDoctorsToCase(normalizedCaseId, options);
            logStreamService.logToolResult(sessionId, "match_doctors_to_case",
                    "Found " + (result != null ? result.size() : 0) + " matches");
            return result;
        } catch (Exception e) {
            logStreamService.logError(sessionId, "match_doctors_to_case failed", e.getMessage());
            throw e;
        }
    }
}
