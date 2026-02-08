package com.berdachuk.medexpertmatch.llm.tools;

import com.berdachuk.medexpertmatch.caseanalysis.domain.CaseAnalysisResult;
import com.berdachuk.medexpertmatch.caseanalysis.service.CaseAnalysisService;
import com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience;
import com.berdachuk.medexpertmatch.clinicalexperience.repository.ClinicalExperienceRepository;
import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.evidence.domain.PubMedArticle;
import com.berdachuk.medexpertmatch.evidence.service.PubMedService;
import com.berdachuk.medexpertmatch.facility.domain.Facility;
import com.berdachuk.medexpertmatch.facility.repository.FacilityRepository;
import com.berdachuk.medexpertmatch.graph.service.GraphService;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.retrieval.domain.*;
import com.berdachuk.medexpertmatch.retrieval.service.MatchingService;
import com.berdachuk.medexpertmatch.retrieval.service.SemanticGraphRetrievalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Spring AI tools for MedExpertMatch (case analysis, doctor matching, evidence retrieval).
 */
@Slf4j
@Component
public class MedicalAgentTools {

    private final CaseAnalysisService caseAnalysisService;
    private final MedicalCaseRepository medicalCaseRepository;
    private final DoctorRepository doctorRepository;
    private final FacilityRepository facilityRepository;
    private final ClinicalExperienceRepository clinicalExperienceRepository;
    private final GraphService graphService;
    private final MatchingService matchingService;
    private final SemanticGraphRetrievalService semanticGraphRetrievalService;
    private final PubMedService pubmedService;
    private final ChatClient medGemmaChatClient;
    private final LogStreamService logStreamService;

    public MedicalAgentTools(
            @Lazy CaseAnalysisService caseAnalysisService,
            MedicalCaseRepository medicalCaseRepository,
            DoctorRepository doctorRepository,
            FacilityRepository facilityRepository,
            ClinicalExperienceRepository clinicalExperienceRepository,
            GraphService graphService,
            @Lazy MatchingService matchingService,
            @Lazy SemanticGraphRetrievalService semanticGraphRetrievalService,
            PubMedService pubmedService,
            @Qualifier("caseAnalysisChatClient") ChatClient medGemmaChatClient,
            LogStreamService logStreamService) {
        this.caseAnalysisService = caseAnalysisService;
        this.medicalCaseRepository = medicalCaseRepository;
        this.doctorRepository = doctorRepository;
        this.facilityRepository = facilityRepository;
        this.clinicalExperienceRepository = clinicalExperienceRepository;
        this.graphService = graphService;
        this.matchingService = matchingService;
        this.semanticGraphRetrievalService = semanticGraphRetrievalService;
        this.pubmedService = pubmedService;
        this.medGemmaChatClient = medGemmaChatClient;
        this.logStreamService = logStreamService;
    }

    private String getSessionId() {
        // Get session ID from thread-local context
        return logStreamService.getCurrentSessionId();
    }

    // ============================================
    // Case Analyzer Tools
    // ============================================

    @Tool(description = "Analyze a medical case by ID and extract key clinical information including chief complaint, symptoms, diagnosis, urgency level, and required specialty.")
    public CaseAnalysisResult analyze_case(
            @ToolParam(description = "Medical case ID - MUST be the exact case ID provided in the prompt (24-character hex string), not a placeholder or invented ID") String caseId
    ) {
        log.info("analyze_case() tool called - caseId: {}", caseId);
        String sessionId = getSessionId();

        // Validate case ID format (must be exactly 24-character hex string)
        if (caseId == null) {
            String errorMsg = "Invalid case ID: null (expected 24-character hex string)";
            log.error(errorMsg);
            logStreamService.logError(sessionId, "analyze_case validation failed", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        log.info("VALIDATING caseId format - original: '{}', length: {}", caseId, caseId.length());
        if (caseId.length() != 24) {
            String errorMsg = String.format("Invalid case ID format: '%s' (expected 24-character hex string, got %d characters)", caseId, caseId.length());
            log.error(errorMsg);
            logStreamService.logError(sessionId, "analyze_case validation failed", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        if (!caseId.matches("^[0-9a-fA-F]{24}$")) {
            String errorMsg = String.format("Invalid case ID format: '%s' (expected 24-character hex string, contains invalid characters)", caseId);
            log.error(errorMsg);
            logStreamService.logError(sessionId, "analyze_case validation failed", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        log.info("Case ID validation passed - original: '{}', length: {}, hex format: valid", caseId, caseId.length());

        // Normalize case ID to lowercase for case-insensitive lookup
        String normalizedCaseId = caseId.trim().toLowerCase();

        logStreamService.logToolCall(sessionId, "analyze_case", "caseId: " + caseId + " (normalized: " + normalizedCaseId + ")");

        try {
            // Load case from repository
            MedicalCase medicalCase = medicalCaseRepository.findById(normalizedCaseId)
                    .orElseThrow(() -> new IllegalArgumentException("Medical case not found: " + caseId));

            // Analyze the case
            CaseAnalysisResult result = caseAnalysisService.analyzeCase(medicalCase);
            logStreamService.logToolResult(sessionId, "analyze_case", "Analysis completed: " + (result != null ? result.toString() : "null"));
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
        String sessionId = getSessionId();
        logStreamService.logToolCall(sessionId, "analyze_case_text", "caseText: " + (caseText != null && caseText.length() > 100 ? caseText.substring(0, 100) + "..." : caseText));

        try {
            // Create temporary MedicalCase from text for analysis
            MedicalCase tempCase = new MedicalCase(
                    null, // No ID for temporary case
                    null,
                    caseText, // Use caseText as chiefComplaint
                    null,
                    null,
                    List.of(),
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    null  // abstractText
            );
            CaseAnalysisResult result = caseAnalysisService.analyzeCase(tempCase);
            logStreamService.logToolResult(sessionId, "analyze_case_text", "Analysis completed: " + (result != null ? result.toString() : "null"));
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
        // Create temporary MedicalCase from text
        MedicalCase tempCase = new MedicalCase(
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
                null  // abstractText
        );
        return caseAnalysisService.extractICD10Codes(tempCase);
    }

    @Tool(description = "Classify urgency level (CRITICAL, HIGH, MEDIUM, LOW) based on case text.")
    public UrgencyLevel classify_urgency(
            @ToolParam(description = "Case description text") String caseText
    ) {
        log.info("classify_urgency() tool called");
        // Create temporary MedicalCase from text
        MedicalCase tempCase = new MedicalCase(
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
                null  // abstractText
        );
        return caseAnalysisService.classifyUrgency(tempCase);
    }

    @Tool(description = "Determine required medical specialty based on case text.")
    public String determine_required_specialty(
            @ToolParam(description = "Case description text") String caseText
    ) {
        log.info("determine_required_specialty() tool called");
        // Create temporary MedicalCase from text
        MedicalCase tempCase = new MedicalCase(
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
                null  // abstractText
        );
        List<String> specialties = caseAnalysisService.determineRequiredSpecialty(tempCase);
        return specialties.isEmpty() ? null : specialties.get(0);
    }

    // ============================================
    // Doctor Matcher Tools
    // ============================================

    @Tool(description = "Query candidate doctors based on case requirements and filters (specialty, telehealth, etc.). NOTE: This is a simple database query WITHOUT scoring or graph relationships. For matching with scoring and graph analysis, use match_doctors_to_case instead. IMPORTANT: Use the exact case ID provided in the prompt - do NOT invent or generate a different ID.")
    public List<Doctor> query_candidate_doctors(
            @ToolParam(description = "Medical case ID - MUST be the exact case ID provided in the prompt (24-character hex string), not a placeholder or invented ID") String caseId,
            @ToolParam(description = "Required medical specialty (optional)") String specialty,
            @ToolParam(description = "Require telehealth capability (optional)") Boolean requireTelehealth,
            @ToolParam(description = "Maximum number of results (default: 10)") Integer maxResults
    ) {
        log.info("query_candidate_doctors() tool called - caseId: {}, specialty: {}, requireTelehealth: {}, maxResults: {}",
                caseId, specialty, requireTelehealth, maxResults);
        String sessionId = getSessionId();
        logStreamService.logToolCall(sessionId, "query_candidate_doctors",
                String.format("caseId: %s, specialty: %s, requireTelehealth: %s, maxResults: %s",
                        caseId, specialty, requireTelehealth, maxResults));

        try {
            if (maxResults == null || maxResults <= 0) {
                maxResults = 10;
            }

            String requiredSpecialty = specialty;

            // Load case to get requirements if caseId is provided
            if (caseId != null && !caseId.isEmpty()) {
                // Validate case ID format before lookup
                if (caseId.length() != 24 || !caseId.matches("^[0-9a-fA-F]{24}$")) {
                    String errorMsg = String.format("Invalid case ID format: '%s' (expected 24-character hex string, got %d characters)", caseId, caseId.length());
                    log.error(errorMsg);
                    logStreamService.logError(sessionId, "query_candidate_doctors validation failed", errorMsg);
                    throw new IllegalArgumentException(errorMsg);
                }

                log.info("Case ID validation passed - original: '{}', length: {}, hex format: valid", caseId, caseId.length());

                Optional<MedicalCase> caseOpt = medicalCaseRepository.findById(caseId);
                if (caseOpt.isPresent()) {
                    MedicalCase medicalCase = caseOpt.get();
                    // Use provided specialty or fall back to case's required specialty
                    if (requiredSpecialty == null || requiredSpecialty.isEmpty()) {
                        requiredSpecialty = medicalCase.requiredSpecialty();
                    }
                } else {
                    log.warn("Case not found: {}", caseId);
                    logStreamService.logToolResult(sessionId, "query_candidate_doctors", "Case not found: " + caseId);
                }
            }

            // Query doctors by specialty if available
            if (requiredSpecialty != null && !requiredSpecialty.isEmpty()) {
                List<Doctor> doctors = doctorRepository.findBySpecialty(requiredSpecialty, maxResults);
                // Filter by telehealth if required
                if (Boolean.TRUE.equals(requireTelehealth)) {
                    doctors = doctors.stream()
                            .filter(Doctor::telehealthEnabled)
                            .toList();
                }
                logStreamService.logToolResult(sessionId, "query_candidate_doctors",
                        String.format("Found %d doctors for specialty: %s", doctors.size(), requiredSpecialty));
                return doctors;
            }

            // Fallback: get all doctors (limited)
            List<String> doctorIds = doctorRepository.findAllIds(maxResults);
            List<Doctor> doctors = doctorRepository.findByIds(doctorIds);

            // Filter by telehealth if required
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

        // Validate case ID format before lookup
        if (caseId == null || caseId.length() != 24 || !caseId.matches("^[0-9a-fA-F]{24}$")) {
            String errorMsg = String.format("Invalid case ID format: '%s' (expected 24-character hex string, got %d characters)",
                    caseId, caseId != null ? caseId.length() : "null");
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        Optional<MedicalCase> caseOpt = medicalCaseRepository.findById(caseId);
        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);

        if (caseOpt.isEmpty() || doctorOpt.isEmpty()) {
            log.warn("Case or doctor not found - caseId: {}, doctorId: {}", caseId, doctorId);
            throw new IllegalArgumentException("Case or doctor not found");
        }

        return semanticGraphRetrievalService.score(caseOpt.get(), doctorOpt.get());
    }

    @Tool(description = "Match doctors to a medical case with comprehensive scoring and ranking using multiple signals: vector similarity (embeddings), graph relationships (Apache AGE), historical performance, specialty matching, and telehealth availability. This is the PRIMARY tool for doctor matching as it uses graph analysis and returns scored matches. Returns list of doctor matches sorted by score (best match first).")
    public List<DoctorMatch> match_doctors_to_case(
            @ToolParam(description = "Medical case ID - MUST be the exact case ID provided in the prompt (24-character hex string), not a placeholder or invented ID") String caseId,
            @ToolParam(description = "Maximum number of results (default: 10)") Integer maxResults,
            @ToolParam(description = "Minimum match score threshold (0-100, optional)") Double minScore,
            @ToolParam(description = "List of preferred specialties (optional)") List<String> preferredSpecialties,
            @ToolParam(description = "Require telehealth capability (optional)") Boolean requireTelehealth
    ) {
        // Validate case ID format before normalization (must be exactly 24-character hex string)
        if (caseId == null) {
            String errorMsg = "Invalid case ID: null (expected 24-character hex string)";
            log.error(errorMsg);
            logStreamService.logError(getSessionId(), "match_doctors_to_case validation failed", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        // Check length and hex format BEFORE normalization
        log.info("VALIDATING caseId format - original: '{}', length: {}", caseId, caseId.length());
        if (caseId.length() != 24) {
            String errorMsg = String.format("Invalid case ID format: '%s' (expected 24-character hex string, got %d characters)", caseId, caseId.length());
            log.error(errorMsg);
            logStreamService.logError(getSessionId(), "match_doctors_to_case validation failed", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        if (!caseId.matches("^[0-9a-fA-F]{24}$")) {
            String errorMsg = String.format("Invalid case ID format: '%s' (expected 24-character hex string, contains invalid characters)", caseId);
            log.error(errorMsg);
            logStreamService.logError(getSessionId(), "match_doctors_to_case validation failed", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        log.info("Case ID validation passed - original: '{}', length: {}, hex format: valid", caseId, caseId.length());

        // Normalize case ID to lowercase for case-insensitive lookup
        String normalizedCaseId = caseId.trim().toLowerCase();

        log.info("match_doctors_to_case() tool called - caseId: {} (normalized: {}), maxResults: {}, minScore: {}, preferredSpecialties: {}, requireTelehealth: {}",
                caseId, normalizedCaseId, maxResults, minScore, preferredSpecialties, requireTelehealth);
        String sessionId = getSessionId();
        String params = String.format("caseId: %s (normalized: %s), maxResults: %s, minScore: %s", caseId, normalizedCaseId, maxResults, minScore);
        logStreamService.logToolCall(sessionId, "match_doctors_to_case", params);

        try {
            MatchOptions options = MatchOptions.builder()
                    .maxResults(maxResults != null && maxResults > 0 ? maxResults : 10)
                    .minScore(minScore)
                    .preferredSpecialties(preferredSpecialties)
                    .requireTelehealth(requireTelehealth)
                    .build();

            List<DoctorMatch> result = matchingService.matchDoctorsToCase(normalizedCaseId, options);
            logStreamService.logToolResult(sessionId, "match_doctors_to_case", "Found " + (result != null ? result.size() : 0) + " matches");
            return result;
        } catch (Exception e) {
            logStreamService.logError(sessionId, "match_doctors_to_case failed", e.getMessage());
            throw e;
        }
    }

    // ============================================
    // Evidence Retriever Tools
    // ============================================

    @Tool(description = "Search clinical practice guidelines for a medical condition. Returns relevant guidelines with citations.")
    public List<String> search_clinical_guidelines(
            @ToolParam(description = "Medical condition or diagnosis") String condition,
            @ToolParam(description = "Medical specialty (optional)") String specialty,
            @ToolParam(description = "Maximum number of results (default: 10)") Integer maxResults
    ) {
        log.info("search_clinical_guidelines() tool called - condition: {}, specialty: {}, maxResults: {}",
                condition, specialty, maxResults);
        String sessionId = getSessionId();
        logStreamService.logToolCall(sessionId, "search_clinical_guidelines",
                String.format("condition: %s, specialty: %s, maxResults: %s", condition, specialty, maxResults));

        try {
            if (condition == null || condition.trim().isEmpty()) {
                log.warn("Condition is required for search_clinical_guidelines");
                logStreamService.logError(sessionId, "search_clinical_guidelines failed", "Condition is required");
                return List.of("Error: Condition is required");
            }

            int limit = (maxResults != null && maxResults > 0) ? maxResults : 10;

            // Build prompt for MedGemma to generate clinical guidelines
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("You are a medical expert providing clinical practice guidelines.\n");
            promptBuilder.append("IMPORTANT MEDICAL DISCLAIMER: These guidelines are for informational and educational purposes only. ");
            promptBuilder.append("They are not a substitute for professional medical advice, diagnosis, or treatment. ");
            promptBuilder.append("Always seek the advice of qualified health providers with questions regarding medical conditions. ");
            promptBuilder.append("Never disregard professional medical advice or delay seeking it because of information provided here.\n\n");
            promptBuilder.append("Provide clinical practice guidelines for the following condition");
            if (specialty != null && !specialty.trim().isEmpty()) {
                promptBuilder.append(" in the specialty of ").append(specialty);
            }
            promptBuilder.append(": ").append(condition).append("\n\n");
            promptBuilder.append("Please provide:\n");
            promptBuilder.append("1. Diagnostic criteria and workup recommendations\n");
            promptBuilder.append("2. Treatment guidelines and options\n");
            promptBuilder.append("3. Monitoring and follow-up recommendations\n");
            promptBuilder.append("4. Key considerations and contraindications\n\n");
            promptBuilder.append("Format your response as a numbered list of guidelines. ");
            promptBuilder.append("Limit your response to ").append(limit).append(" main guideline categories.\n");
            promptBuilder.append("Include citations to major clinical guidelines organizations (e.g., NICE, AHA, ACC, WHO) where applicable.\n\n");
            promptBuilder.append("CRITICAL OUTPUT LIMITS:\n");
            promptBuilder.append("- Provide EXACTLY ").append(limit).append(" guideline categories and STOP\n");
            promptBuilder.append("- Do NOT repeat the same guidelines multiple times\n");
            promptBuilder.append("- Maximum response length: 2000 words (approximately 10000 characters)\n");
            promptBuilder.append("- Stop immediately after providing the ").append(limit).append(" categories\n");
            promptBuilder.append("- Do NOT continue generating after the response is complete");

            String prompt = promptBuilder.toString();

            log.info("Sending prompt to LLM for clinical guidelines (model: MedGemma, condition: {}, specialty: {}):\n{}", condition, specialty, prompt);
            log.info("Calling MedGemma for clinical guidelines - condition: {}, specialty: {}", condition, specialty);
            String responseText = medGemmaChatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (responseText == null || responseText.trim().isEmpty()) {
                log.warn("Empty response from MedGemma for clinical guidelines");
                logStreamService.logToolResult(sessionId, "search_clinical_guidelines",
                        "No guidelines generated");
                return List.of("No clinical guidelines could be generated for condition: " + condition);
            }

            // Split response into individual guidelines (by numbered list or bullet points)
            List<String> guidelines = new ArrayList<>();
            String[] lines = responseText.split("\n");
            StringBuilder currentGuideline = new StringBuilder();

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) {
                    if (currentGuideline.length() > 0) {
                        guidelines.add(currentGuideline.toString().trim());
                        currentGuideline = new StringBuilder();
                    }
                } else if (line.matches("^\\d+[.)]\\s+.*") || line.matches("^[-*]\\s+.*")) {
                    // New guideline item
                    if (currentGuideline.length() > 0) {
                        guidelines.add(currentGuideline.toString().trim());
                    }
                    currentGuideline = new StringBuilder(line);
                } else {
                    // Continuation of current guideline
                    if (currentGuideline.length() > 0) {
                        currentGuideline.append(" ").append(line);
                    } else {
                        currentGuideline.append(line);
                    }
                }
            }

            // Add last guideline if exists
            if (currentGuideline.length() > 0) {
                guidelines.add(currentGuideline.toString().trim());
            }

            // Limit to maxResults
            if (guidelines.size() > limit) {
                guidelines = guidelines.subList(0, limit);
            }

            if (guidelines.isEmpty()) {
                // If parsing failed, return the full response as a single guideline
                guidelines.add(responseText);
            }

            logStreamService.logToolResult(sessionId, "search_clinical_guidelines",
                    String.format("Generated %d guidelines", guidelines.size()));
            return guidelines;
        } catch (Exception e) {
            log.error("Error searching clinical guidelines", e);
            logStreamService.logError(sessionId, "search_clinical_guidelines failed", e.getMessage());
            return List.of("Error searching clinical guidelines: " + e.getMessage());
        }
    }

    @Tool(description = "Query PubMed medical literature database. Returns relevant articles with titles, abstracts, and citations.")
    public List<String> query_pubmed(
            @ToolParam(description = "Search query string") String query,
            @ToolParam(description = "Maximum number of results (default: 10)") Integer maxResults
    ) {
        log.info("query_pubmed() tool called - query: {}, maxResults: {}", query, maxResults);
        String sessionId = getSessionId();
        logStreamService.logToolCall(sessionId, "query_pubmed",
                String.format("query: %s, maxResults: %s", query, maxResults));

        try {
            if (query == null || query.trim().isEmpty()) {
                log.warn("Query string is required for query_pubmed");
                logStreamService.logError(sessionId, "query_pubmed failed", "Query string is required");
                return List.of("Error: Query string is required");
            }

            int limit = (maxResults != null && maxResults > 0) ? maxResults : 10;

            List<PubMedArticle> articles = pubmedService.search(query, limit);
            log.info("query_pubmed: PubMed search returned {} articles for query: {}", articles.size(), query);

            if (articles.isEmpty()) {
                logStreamService.logToolResult(sessionId, "query_pubmed",
                        "No articles found for query: " + query);
                return List.of("No articles found for query: " + query);
            }

            List<String> articleSummaries = new ArrayList<>();
            for (PubMedArticle article : articles) {
                StringBuilder summary = new StringBuilder();
                summary.append("Title: ").append(article.title() != null ? article.title() : "N/A").append("\n");
                if (article.authors() != null && !article.authors().isEmpty()) {
                    summary.append("Authors: ").append(String.join(", ", article.authors())).append("\n");
                }
                if (article.journal() != null && !article.journal().isEmpty()) {
                    summary.append("Journal: ").append(article.journal());
                    if (article.year() != null) {
                        summary.append(" (").append(article.year()).append(")");
                    }
                    summary.append("\n");
                }
                if (article.pmid() != null && !article.pmid().isEmpty()) {
                    summary.append("PMID: ").append(article.pmid()).append("\n");
                }
                if (article.abstractText() != null && !article.abstractText().isEmpty()) {
                    // Truncate abstract to 500 characters for readability
                    String abstractText = article.abstractText();
                    if (abstractText.length() > 500) {
                        abstractText = abstractText.substring(0, 500) + "...";
                    }
                    summary.append("Abstract: ").append(abstractText).append("\n");
                }
                articleSummaries.add(summary.toString());
            }

            logStreamService.logToolResult(sessionId, "query_pubmed",
                    String.format("Found %d articles", articles.size()));
            return articleSummaries;
        } catch (Exception e) {
            log.error("Error querying PubMed", e);
            logStreamService.logError(sessionId, "query_pubmed failed", e.getMessage());
            return List.of("Error querying PubMed: " + e.getMessage());
        }
    }

    // ============================================
    // Recommendation Engine Tools
    // ============================================

    @Tool(description = "Generate clinical recommendations for a medical case (diagnostic, treatment, or follow-up).")
    public String generate_recommendations(
            @ToolParam(description = "Medical case ID - MUST be the exact case ID provided in the prompt (24-character hex string), not a placeholder or invented ID") String caseId,
            @ToolParam(description = "Type of recommendation: DIAGNOSTIC, TREATMENT, or FOLLOW_UP") String recommendationType,
            @ToolParam(description = "Include evidence citations (default: true)") Boolean includeEvidence
    ) {
        log.info("generate_recommendations() tool called - caseId: {}, recommendationType: {}, includeEvidence: {}",
                caseId, recommendationType, includeEvidence);
        String sessionId = getSessionId();
        logStreamService.logToolCall(sessionId, "generate_recommendations",
                String.format("caseId: %s, recommendationType: %s, includeEvidence: %s", caseId, recommendationType, includeEvidence));

        try {
            // Validate case ID format before lookup
            if (caseId == null || caseId.length() != 24 || !caseId.matches("^[0-9a-fA-F]{24}$")) {
                String errorMsg = String.format("Invalid case ID format: '%s' (expected 24-character hex string, got %d characters)",
                        caseId, caseId != null ? caseId.length() : "null");
                log.error(errorMsg);
                logStreamService.logError(sessionId, "generate_recommendations validation failed", errorMsg);
                return "Error: " + errorMsg;
            }

            Optional<MedicalCase> caseOpt = medicalCaseRepository.findById(caseId);
            if (caseOpt.isEmpty()) {
                log.warn("Medical case not found - caseId: {}", caseId);
                logStreamService.logError(sessionId, "generate_recommendations failed", "Medical case not found: " + caseId);
                return "Error: Medical case not found: " + caseId;
            }

            MedicalCase medicalCase = caseOpt.get();
            String normalizedType = recommendationType != null ? recommendationType.toUpperCase() : "DIAGNOSTIC";
            boolean includeEvidenceFlag = includeEvidence != null ? includeEvidence : true;

            // Build case context
            StringBuilder caseContext = new StringBuilder();
            caseContext.append("Chief Complaint: ").append(medicalCase.chiefComplaint() != null ? medicalCase.chiefComplaint() : "N/A").append("\n");
            caseContext.append("Symptoms: ").append(medicalCase.symptoms() != null ? medicalCase.symptoms() : "N/A").append("\n");
            if (medicalCase.icd10Codes() != null && !medicalCase.icd10Codes().isEmpty()) {
                caseContext.append("ICD-10 Codes: ").append(String.join(", ", medicalCase.icd10Codes())).append("\n");
            }
            if (medicalCase.requiredSpecialty() != null) {
                caseContext.append("Required Specialty: ").append(medicalCase.requiredSpecialty()).append("\n");
            }
            if (medicalCase.additionalNotes() != null && !medicalCase.additionalNotes().isEmpty()) {
                caseContext.append("Additional Notes: ").append(medicalCase.additionalNotes()).append("\n");
            }

            // Gather evidence if requested
            String evidenceSection = "";
            if (includeEvidenceFlag) {
                evidenceSection = "\n\nEvidence from Clinical Guidelines and Literature:\n";
                if (medicalCase.icd10Codes() != null && !medicalCase.icd10Codes().isEmpty()) {
                    for (String icd10Code : medicalCase.icd10Codes()) {
                        List<String> guidelines = search_clinical_guidelines(icd10Code, medicalCase.requiredSpecialty(), 3);
                        if (!guidelines.isEmpty()) {
                            evidenceSection += "Guidelines for " + icd10Code + ":\n";
                            for (String guideline : guidelines) {
                                evidenceSection += "- " + guideline + "\n";
                            }
                        }
                    }
                }
                // Query PubMed for relevant articles
                String pubmedQuery = medicalCase.chiefComplaint() != null ? medicalCase.chiefComplaint() :
                        (medicalCase.icd10Codes() != null && !medicalCase.icd10Codes().isEmpty() ?
                                medicalCase.icd10Codes().get(0) : "");
                if (!pubmedQuery.isEmpty()) {
                    List<String> articles = query_pubmed(pubmedQuery, 3);
                    if (!articles.isEmpty()) {
                        evidenceSection += "\nRelevant Literature:\n";
                        for (String article : articles) {
                            evidenceSection += article + "\n\n";
                        }
                    }
                }
            }

            // Build prompt based on recommendation type
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("You are a medical expert providing clinical recommendations.\n");
            promptBuilder.append("IMPORTANT MEDICAL DISCLAIMER: These recommendations are for informational and educational purposes only. ");
            promptBuilder.append("They are not a substitute for professional medical advice, diagnosis, or treatment. ");
            promptBuilder.append("Always seek the advice of qualified health providers with questions regarding medical conditions. ");
            promptBuilder.append("Never disregard professional medical advice or delay seeking it because of information provided here.\n\n");
            promptBuilder.append("Medical Case Information:\n").append(caseContext).append("\n");

            switch (normalizedType) {
                case "DIAGNOSTIC":
                    promptBuilder.append("Generate diagnostic workup recommendations for this case. ");
                    promptBuilder.append("Include recommended tests, imaging studies, laboratory work, and diagnostic procedures. ");
                    promptBuilder.append("Prioritize recommendations based on urgency and clinical relevance.\n");
                    break;
                case "TREATMENT":
                    promptBuilder.append("Generate treatment recommendations for this case. ");
                    promptBuilder.append("Include medication options, procedures, interventions, and therapeutic approaches. ");
                    promptBuilder.append("Consider contraindications, drug interactions, and patient-specific factors.\n");
                    break;
                case "FOLLOW_UP":
                    promptBuilder.append("Generate follow-up and monitoring recommendations for this case. ");
                    promptBuilder.append("Include recommended follow-up intervals, monitoring parameters, warning signs to watch for, ");
                    promptBuilder.append("and when to seek immediate medical attention.\n");
                    break;
                default:
                    promptBuilder.append("Generate clinical recommendations for this case.\n");
            }

            if (includeEvidenceFlag && !evidenceSection.isEmpty()) {
                promptBuilder.append(evidenceSection).append("\n");
                promptBuilder.append("Please incorporate the evidence from guidelines and literature into your recommendations.\n");
            }

            promptBuilder.append("\nFormat your response as a clear, structured list of recommendations with rationale.\n\n");
            promptBuilder.append("CRITICAL OUTPUT LIMITS:\n");
            promptBuilder.append("- Provide EXACTLY ONE response and STOP after completing the recommendations\n");
            promptBuilder.append("- Do NOT repeat the same recommendations multiple times\n");
            promptBuilder.append("- Maximum response length: 2000 words (approximately 10000 characters)\n");
            promptBuilder.append("- Stop immediately after providing the recommendations\n");
            promptBuilder.append("- Do NOT continue generating after the response is complete");

            String prompt = promptBuilder.toString();

            log.info("Sending prompt to LLM for recommendations (model: MedGemma, caseId: {}, type: {}):\n{}", caseId, normalizedType, prompt);
            log.info("Calling MedGemma for recommendations - caseId: {}, type: {}", caseId, normalizedType);
            String responseText = medGemmaChatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (responseText == null || responseText.trim().isEmpty()) {
                log.warn("Empty response from MedGemma for recommendations");
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

    // ============================================
    // Clinical Advisor Tools
    // ============================================

    @Tool(description = "Generate differential diagnosis list based on symptoms and clinical findings.")
    public String differential_diagnosis(
            @ToolParam(description = "Medical case ID - MUST be the exact case ID provided in the prompt (24-character hex string), not a placeholder or invented ID") String caseId,
            @ToolParam(description = "Maximum number of diagnoses (default: 10)") Integer maxResults
    ) {
        log.info("differential_diagnosis() tool called - caseId: {}, maxResults: {}", caseId, maxResults);
        String sessionId = getSessionId();
        logStreamService.logToolCall(sessionId, "differential_diagnosis",
                String.format("caseId: %s, maxResults: %s", caseId, maxResults));

        try {
            // Validate case ID format before lookup
            if (caseId == null || caseId.length() != 24 || !caseId.matches("^[0-9a-fA-F]{24}$")) {
                String errorMsg = String.format("Invalid case ID format: '%s' (expected 24-character hex string, got %d characters)",
                        caseId, caseId != null ? caseId.length() : "null");
                log.error(errorMsg);
                logStreamService.logError(sessionId, "differential_diagnosis validation failed", errorMsg);
                return "Error: " + errorMsg;
            }

            Optional<MedicalCase> caseOpt = medicalCaseRepository.findById(caseId);
            if (caseOpt.isEmpty()) {
                log.warn("Medical case not found - caseId: {}", caseId);
                logStreamService.logError(sessionId, "differential_diagnosis failed", "Medical case not found: " + caseId);
                return "Error: Medical case not found: " + caseId;
            }

            MedicalCase medicalCase = caseOpt.get();
            int limit = (maxResults != null && maxResults > 0) ? maxResults : 10;

            // Build case context
            StringBuilder caseContext = new StringBuilder();
            caseContext.append("Chief Complaint: ").append(medicalCase.chiefComplaint() != null ? medicalCase.chiefComplaint() : "N/A").append("\n");
            caseContext.append("Symptoms: ").append(medicalCase.symptoms() != null ? medicalCase.symptoms() : "N/A").append("\n");
            if (medicalCase.currentDiagnosis() != null && !medicalCase.currentDiagnosis().isEmpty()) {
                caseContext.append("Current Diagnosis: ").append(medicalCase.currentDiagnosis()).append("\n");
            }
            if (medicalCase.icd10Codes() != null && !medicalCase.icd10Codes().isEmpty()) {
                caseContext.append("ICD-10 Codes: ").append(String.join(", ", medicalCase.icd10Codes())).append("\n");
            }
            if (medicalCase.additionalNotes() != null && !medicalCase.additionalNotes().isEmpty()) {
                caseContext.append("Additional Notes: ").append(medicalCase.additionalNotes()).append("\n");
            }

            // Build prompt

            String prompt = "You are a medical expert providing differential diagnosis.\n" +
                    "IMPORTANT MEDICAL DISCLAIMER: This differential diagnosis is for informational and educational purposes only. " +
                    "It is not a substitute for professional medical advice, diagnosis, or treatment. " +
                    "Always seek the advice of qualified health providers with questions regarding medical conditions. " +
                    "Never disregard professional medical advice or delay seeking it because of information provided here.\n\n" +
                    "Medical Case Information:\n" + caseContext + "\n" +
                    "Generate a differential diagnosis list for this case based on the symptoms and clinical findings. " +
                    "List the most likely diagnoses in order of probability. " +
                    "For each diagnosis, provide:\n" +
                    "1. The diagnosis name\n" +
                    "2. Probability/confidence level (HIGH, MODERATE, LOW)\n" +
                    "3. Key supporting clinical features\n" +
                    "4. Recommended diagnostic tests to confirm or rule out\n\n" +
                    "Limit your response to " + limit + " diagnoses.\n" +
                    "Format your response as a numbered list.\n\n" +
                    "CRITICAL OUTPUT LIMITS:\n" +
                    "- Provide EXACTLY " + limit + " diagnoses and STOP\n" +
                    "- Do NOT repeat the same diagnoses multiple times\n" +
                    "- Maximum response length: 2000 words (approximately 10000 characters)\n" +
                    "- Stop immediately after providing the " + limit + " diagnoses\n" +
                    "- Do NOT continue generating after the response is complete";

            log.info("Sending prompt to LLM for differential diagnosis (model: MedGemma, caseId: {}):\n{}", caseId, prompt);
            log.info("Calling MedGemma for differential diagnosis - caseId: {}", caseId);
            String responseText = medGemmaChatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (responseText == null || responseText.trim().isEmpty()) {
                log.warn("Empty response from MedGemma for differential diagnosis");
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
        String sessionId = getSessionId();
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

            // Build case context
            StringBuilder caseContext = new StringBuilder();
            if (medicalCase.patientAge() != null) {
                caseContext.append("Patient Age: ").append(medicalCase.patientAge()).append("\n");
            }
            caseContext.append("Chief Complaint: ").append(medicalCase.chiefComplaint() != null ? medicalCase.chiefComplaint() : "N/A").append("\n");
            caseContext.append("Symptoms: ").append(medicalCase.symptoms() != null ? medicalCase.symptoms() : "N/A").append("\n");
            if (medicalCase.urgencyLevel() != null) {
                caseContext.append("Urgency Level: ").append(medicalCase.urgencyLevel()).append("\n");
            }
            if (medicalCase.icd10Codes() != null && !medicalCase.icd10Codes().isEmpty()) {
                caseContext.append("ICD-10 Codes: ").append(String.join(", ", medicalCase.icd10Codes())).append("\n");
            }
            if (medicalCase.additionalNotes() != null && !medicalCase.additionalNotes().isEmpty()) {
                caseContext.append("Additional Notes: ").append(medicalCase.additionalNotes()).append("\n");
            }

            // Query historical data for similar cases
            String historicalData = "";
            if (medicalCase.icd10Codes() != null && !medicalCase.icd10Codes().isEmpty()) {
                // Find similar cases and their outcomes
                List<MedicalCase> similarCases = medicalCaseRepository.findByIcd10Code(medicalCase.icd10Codes().get(0), 10);
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
                        historicalData = String.format("\nHistorical Data: Based on %d similar cases, complication rate: %.1f%%\n",
                                totalCases, complicationRate);
                    }
                }
            }

            // Build prompt based on risk type
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("You are a medical expert providing risk assessment.\n");
            promptBuilder.append("IMPORTANT MEDICAL DISCLAIMER: This risk assessment is for informational and educational purposes only. ");
            promptBuilder.append("It is not a substitute for professional medical advice, diagnosis, or treatment. ");
            promptBuilder.append("Always seek the advice of qualified health providers with questions regarding medical conditions. ");
            promptBuilder.append("Never disregard professional medical advice or delay seeking it because of information provided here.\n\n");
            promptBuilder.append("Medical Case Information:\n").append(caseContext);
            if (!historicalData.isEmpty()) {
                promptBuilder.append(historicalData);
            }

            switch (normalizedRiskType) {
                case "COMPLICATION":
                    promptBuilder.append("\nAssess the risk of complications for this case. ");
                    promptBuilder.append("Identify potential complications, their likelihood, and key risk factors.\n");
                    break;
                case "MORTALITY":
                    promptBuilder.append("\nAssess mortality risk for this case. ");
                    promptBuilder.append("Identify factors that increase or decrease mortality risk.\n");
                    break;
                case "READMISSION":
                    promptBuilder.append("\nAssess readmission risk for this case. ");
                    promptBuilder.append("Identify factors that may lead to hospital readmission.\n");
                    break;
                default:
                    promptBuilder.append("\nAssess patient risk factors for this case.\n");
            }

            promptBuilder.append("\nProvide:\n");
            promptBuilder.append("1. Overall risk level (LOW, MODERATE, HIGH, CRITICAL)\n");
            promptBuilder.append("2. Key risk factors identified\n");
            promptBuilder.append("3. Mitigation strategies and recommendations\n");
            promptBuilder.append("4. Monitoring parameters to watch\n");
            promptBuilder.append("\nFormat your response as a structured assessment.\n\n");
            promptBuilder.append("CRITICAL OUTPUT LIMITS:\n");
            promptBuilder.append("- Provide EXACTLY ONE assessment and STOP after completing it\n");
            promptBuilder.append("- Do NOT repeat the same assessment multiple times\n");
            promptBuilder.append("- Maximum response length: 2000 words (approximately 10000 characters)\n");
            promptBuilder.append("- Stop immediately after providing the assessment\n");
            promptBuilder.append("- Do NOT continue generating after the response is complete");

            String prompt = promptBuilder.toString();

            log.info("Sending prompt to LLM for risk assessment (model: MedGemma, caseId: {}, type: {}):\n{}", caseId, normalizedRiskType, prompt);
            log.info("Calling MedGemma for risk assessment - caseId: {}, type: {}", caseId, normalizedRiskType);
            String responseText = medGemmaChatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (responseText == null || responseText.trim().isEmpty()) {
                log.warn("Empty response from MedGemma for risk assessment");
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

    // ============================================
    // Network Analyzer Tools
    // ============================================

    @Tool(description = "Query graph to find top experts for a specific condition based on historical performance and outcomes.")
    public List<String> graph_query_top_experts(
            @ToolParam(description = "ICD-10 code for the condition") String conditionCode,
            @ToolParam(description = "Maximum number of experts (default: 10)") Integer maxResults
    ) {
        log.info("graph_query_top_experts() tool called - conditionCode: {}, maxResults: {}", conditionCode, maxResults);
        String sessionId = getSessionId();
        logStreamService.logToolCall(sessionId, "graph_query_top_experts",
                String.format("conditionCode: %s, maxResults: %s", conditionCode, maxResults));

        try {
            if (conditionCode == null || conditionCode.trim().isEmpty()) {
                log.warn("Condition code is required for graph_query_top_experts");
                logStreamService.logError(sessionId, "graph_query_top_experts failed", "Condition code is required");
                return List.of("Error: Condition code is required");
            }

            int limit = (maxResults != null && maxResults > 0) ? maxResults : 10;

            // Check if graph exists
            if (!graphService.graphExists()) {
                log.debug("Graph does not exist, returning empty results");
                logStreamService.logToolResult(sessionId, "graph_query_top_experts",
                        "Graph not available - returning empty results");
                return List.of("Graph not available. Please ensure Apache AGE graph is populated.");
            }

            // Cypher query to find doctors who treated cases with matching ICD-10 code
            String cypherQuery = """
                    MATCH (d:Doctor)-[:TREATED]->(c:MedicalCase)-[:HAS_CONDITION]->(i:ICD10Code {code: $conditionCode})
                    RETURN d.id as doctorId, count(DISTINCT c) as caseCount, collect(DISTINCT c.id) as caseIds
                    ORDER BY caseCount DESC
                    LIMIT $maxResults
                    """;

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("conditionCode", conditionCode);
            parameters.put("maxResults", limit);

            List<Map<String, Object>> results = graphService.executeCypher(cypherQuery, parameters);

            List<String> expertResults = new ArrayList<>();
            for (Map<String, Object> row : results) {
                Object doctorIdObj = row.get("doctorId");
                Object caseCountObj = row.get("caseCount");

                if (doctorIdObj != null) {
                    String doctorId = doctorIdObj.toString();
                    long caseCount = caseCountObj != null ? Long.parseLong(caseCountObj.toString()) : 0;

                    // Enrich with clinical experience data for ratings/outcomes
                    List<ClinicalExperience> experiences = clinicalExperienceRepository.findByDoctorId(doctorId);
                    double avgRating = 0.0;
                    int successCount = 0;
                    int totalExperiences = experiences.size();

                    if (!experiences.isEmpty()) {
                        double totalRating = 0.0;
                        for (ClinicalExperience exp : experiences) {
                            if (exp.rating() != null) {
                                totalRating += exp.rating();
                            }
                            if ("SUCCESS".equalsIgnoreCase(exp.outcome()) ||
                                    "IMPROVED".equalsIgnoreCase(exp.outcome())) {
                                successCount++;
                            }
                        }
                        avgRating = totalExperiences > 0 ? totalRating / totalExperiences : 0.0;
                    }

                    double successRate = totalExperiences > 0 ? (double) successCount / totalExperiences : 0.0;

                    String result = String.format("Doctor ID: %s, Cases: %d, Avg Rating: %.2f, Success Rate: %.2f%%",
                            doctorId, caseCount, avgRating, successRate * 100);
                    expertResults.add(result);
                }
            }

            if (expertResults.isEmpty()) {
                logStreamService.logToolResult(sessionId, "graph_query_top_experts",
                        "No experts found for condition: " + conditionCode);
                return List.of("No experts found for condition code: " + conditionCode);
            }

            logStreamService.logToolResult(sessionId, "graph_query_top_experts",
                    String.format("Found %d experts", expertResults.size()));
            return expertResults;
        } catch (Exception e) {
            log.error("Error querying top experts from graph", e);
            logStreamService.logError(sessionId, "graph_query_top_experts failed", e.getMessage());
            return List.of("Error querying graph: " + e.getMessage());
        }
    }

    @Tool(description = "Aggregate performance metrics for doctors, conditions, or facilities.")
    public String aggregate_metrics(
            @ToolParam(description = "Type of entity: DOCTOR, CONDITION, or FACILITY") String entityType,
            @ToolParam(description = "Entity ID (optional, for specific entity)") String entityId,
            @ToolParam(description = "Type of metrics: PERFORMANCE, OUTCOMES, or VOLUME") String metricType
    ) {
        log.info("aggregate_metrics() tool called - entityType: {}, entityId: {}, metricType: {}",
                entityType, entityId, metricType);
        String sessionId = getSessionId();
        logStreamService.logToolCall(sessionId, "aggregate_metrics",
                String.format("entityType: %s, entityId: %s, metricType: %s", entityType, entityId, metricType));

        try {
            if (entityType == null || entityType.trim().isEmpty()) {
                log.warn("Entity type is required for aggregate_metrics");
                logStreamService.logError(sessionId, "aggregate_metrics failed", "Entity type is required");
                return "Error: Entity type is required (DOCTOR, CONDITION, or FACILITY)";
            }

            String normalizedEntityType = entityType.toUpperCase();
            String normalizedMetricType = metricType != null ? metricType.toUpperCase() : "PERFORMANCE";

            StringBuilder result = new StringBuilder();
            result.append(String.format("Metrics for %s", normalizedEntityType));
            if (entityId != null && !entityId.trim().isEmpty()) {
                result.append(String.format(" (ID: %s)", entityId));
            }
            result.append(":\n");

            switch (normalizedEntityType) {
                case "DOCTOR":
                    result.append(aggregateDoctorMetrics(entityId, normalizedMetricType));
                    break;
                case "CONDITION":
                    result.append(aggregateConditionMetrics(entityId, normalizedMetricType));
                    break;
                case "FACILITY":
                    result.append(aggregateFacilityMetrics(entityId, normalizedMetricType));
                    break;
                default:
                    log.warn("Unknown entity type: {}", entityType);
                    logStreamService.logError(sessionId, "aggregate_metrics failed", "Unknown entity type: " + entityType);
                    return "Error: Unknown entity type. Must be DOCTOR, CONDITION, or FACILITY";
            }

            logStreamService.logToolResult(sessionId, "aggregate_metrics",
                    String.format("Aggregated %s metrics for %s", normalizedMetricType, normalizedEntityType));
            return result.toString();
        } catch (Exception e) {
            log.error("Error aggregating metrics", e);
            logStreamService.logError(sessionId, "aggregate_metrics failed", e.getMessage());
            return "Error aggregating metrics: " + e.getMessage();
        }
    }

    private String aggregateDoctorMetrics(String doctorId, String metricType) {
        StringBuilder metrics = new StringBuilder();

        if (doctorId != null && !doctorId.trim().isEmpty()) {
            // Aggregate for specific doctor
            List<ClinicalExperience> experiences = clinicalExperienceRepository.findByDoctorId(doctorId);
            return aggregateDoctorMetricsFromExperiences(experiences, metricType);
        } else {
            // Aggregate across all doctors
            List<String> allDoctorIds = doctorRepository.findAllIds(1000); // Limit to prevent memory issues
            Map<String, List<ClinicalExperience>> experiencesByDoctor =
                    clinicalExperienceRepository.findByDoctorIds(allDoctorIds);

            int totalDoctors = experiencesByDoctor.size();
            int totalCases = 0;
            double totalRating = 0.0;
            int totalSuccess = 0;
            int totalExperiences = 0;

            for (List<ClinicalExperience> experiences : experiencesByDoctor.values()) {
                totalExperiences += experiences.size();
                for (ClinicalExperience exp : experiences) {
                    totalCases++;
                    if (exp.rating() != null) {
                        totalRating += exp.rating();
                    }
                    if ("SUCCESS".equalsIgnoreCase(exp.outcome()) ||
                            "IMPROVED".equalsIgnoreCase(exp.outcome())) {
                        totalSuccess++;
                    }
                }
            }

            metrics.append(String.format("Total Doctors: %d\n", totalDoctors));
            metrics.append(String.format("Total Cases: %d\n", totalCases));
            if (totalExperiences > 0) {
                double avgRating = totalRating / totalExperiences;
                double successRate = (double) totalSuccess / totalExperiences;
                metrics.append(String.format("Average Rating: %.2f\n", avgRating));
                metrics.append(String.format("Success Rate: %.2f%%\n", successRate * 100));
            }
        }

        return metrics.toString();
    }

    private String aggregateDoctorMetricsFromExperiences(List<ClinicalExperience> experiences, String metricType) {
        StringBuilder metrics = new StringBuilder();

        if (experiences.isEmpty()) {
            return "No clinical experiences found for this doctor.\n";
        }

        int totalCases = experiences.size();
        double totalRating = 0.0;
        int successCount = 0;
        int improvedCount = 0;
        int stableCount = 0;
        int complicatedCount = 0;
        Map<String, Integer> complexityDistribution = new HashMap<>();
        int totalTimeToResolution = 0;
        int casesWithTime = 0;

        for (ClinicalExperience exp : experiences) {
            if (exp.rating() != null) {
                totalRating += exp.rating();
            }
            if (exp.outcome() != null) {
                switch (exp.outcome().toUpperCase()) {
                    case "SUCCESS":
                        successCount++;
                        break;
                    case "IMPROVED":
                        improvedCount++;
                        break;
                    case "STABLE":
                        stableCount++;
                        break;
                    case "COMPLICATED":
                        complicatedCount++;
                        break;
                }
            }
            if (exp.complexityLevel() != null) {
                complexityDistribution.merge(exp.complexityLevel().toUpperCase(), 1, Integer::sum);
            }
            if (exp.timeToResolution() != null) {
                totalTimeToResolution += exp.timeToResolution();
                casesWithTime++;
            }
        }

        double avgRating = totalCases > 0 ? totalRating / totalCases : 0.0;
        double successRate = totalCases > 0 ? (double) (successCount + improvedCount) / totalCases : 0.0;
        double avgTimeToResolution = casesWithTime > 0 ? (double) totalTimeToResolution / casesWithTime : 0.0;

        switch (metricType) {
            case "PERFORMANCE":
                metrics.append(String.format("Total Cases: %d\n", totalCases));
                metrics.append(String.format("Average Rating: %.2f\n", avgRating));
                metrics.append(String.format("Success Rate: %.2f%%\n", successRate * 100));
                metrics.append(String.format("Average Time to Resolution: %.1f days\n", avgTimeToResolution));
                break;
            case "OUTCOMES":
                metrics.append(String.format("Total Cases: %d\n", totalCases));
                metrics.append(String.format("Success: %d (%.2f%%)\n", successCount,
                        totalCases > 0 ? (double) successCount / totalCases * 100 : 0.0));
                metrics.append(String.format("Improved: %d (%.2f%%)\n", improvedCount,
                        totalCases > 0 ? (double) improvedCount / totalCases * 100 : 0.0));
                metrics.append(String.format("Stable: %d (%.2f%%)\n", stableCount,
                        totalCases > 0 ? (double) stableCount / totalCases * 100 : 0.0));
                metrics.append(String.format("Complicated: %d (%.2f%%)\n", complicatedCount,
                        totalCases > 0 ? (double) complicatedCount / totalCases * 100 : 0.0));
                break;
            case "VOLUME":
                metrics.append(String.format("Total Cases: %d\n", totalCases));
                metrics.append("Complexity Distribution:\n");
                for (Map.Entry<String, Integer> entry : complexityDistribution.entrySet()) {
                    metrics.append(String.format("  %s: %d (%.2f%%)\n", entry.getKey(), entry.getValue(),
                            totalCases > 0 ? (double) entry.getValue() / totalCases * 100 : 0.0));
                }
                break;
            default:
                // Default: show all metrics
                metrics.append(String.format("Total Cases: %d\n", totalCases));
                metrics.append(String.format("Average Rating: %.2f\n", avgRating));
                metrics.append(String.format("Success Rate: %.2f%%\n", successRate * 100));
                metrics.append(String.format("Average Time to Resolution: %.1f days\n", avgTimeToResolution));
        }

        return metrics.toString();
    }

    private String aggregateConditionMetrics(String conditionCode, String metricType) {
        StringBuilder metrics = new StringBuilder();

        if (conditionCode == null || conditionCode.trim().isEmpty()) {
            return "Error: Condition code (ICD-10) is required for CONDITION entity type.\n";
        }

        // Find cases with this ICD-10 code
        List<MedicalCase> cases = medicalCaseRepository.findByIcd10Code(conditionCode, 1000);

        if (cases.isEmpty()) {
            return String.format("No cases found for condition code: %s\n", conditionCode);
        }

        int totalCases = cases.size();
        Map<String, Integer> urgencyDistribution = new HashMap<>();
        Map<String, Integer> specialtyDistribution = new HashMap<>();

        for (MedicalCase medicalCase : cases) {
            if (medicalCase.urgencyLevel() != null) {
                urgencyDistribution.merge(medicalCase.urgencyLevel().name(), 1, Integer::sum);
            }
            if (medicalCase.requiredSpecialty() != null) {
                specialtyDistribution.merge(medicalCase.requiredSpecialty(), 1, Integer::sum);
            }
        }

        // Use graph to find doctors treating this condition
        int doctorCount = 0;
        if (graphService.graphExists()) {
            try {
                String cypherQuery = """
                        MATCH (d:Doctor)-[:TREATED]->(c:MedicalCase)-[:HAS_CONDITION]->(i:ICD10Code {code: $conditionCode})
                        RETURN count(DISTINCT d) as doctorCount
                        """;
                Map<String, Object> params = new HashMap<>();
                params.put("conditionCode", conditionCode);
                List<Map<String, Object>> results = graphService.executeCypher(cypherQuery, params);
                if (!results.isEmpty() && results.get(0).get("doctorCount") != null) {
                    doctorCount = Integer.parseInt(results.get(0).get("doctorCount").toString());
                }
            } catch (Exception e) {
                log.debug("Could not query graph for doctor count", e);
            }
        }

        switch (metricType) {
            case "PERFORMANCE":
                metrics.append(String.format("Total Cases: %d\n", totalCases));
                metrics.append(String.format("Doctors Treating Condition: %d\n", doctorCount));
                break;
            case "OUTCOMES":
                // Would need to query ClinicalExperience for cases with this condition
                metrics.append(String.format("Total Cases: %d\n", totalCases));
                metrics.append("Note: Outcome metrics require case ID to query clinical experiences.\n");
                break;
            case "VOLUME":
                metrics.append(String.format("Total Cases: %d\n", totalCases));
                metrics.append(String.format("Doctors Treating Condition: %d\n", doctorCount));
                metrics.append("Urgency Distribution:\n");
                for (Map.Entry<String, Integer> entry : urgencyDistribution.entrySet()) {
                    metrics.append(String.format("  %s: %d (%.2f%%)\n", entry.getKey(), entry.getValue(),
                            totalCases > 0 ? (double) entry.getValue() / totalCases * 100 : 0.0));
                }
                metrics.append("Specialty Distribution:\n");
                for (Map.Entry<String, Integer> entry : specialtyDistribution.entrySet()) {
                    metrics.append(String.format("  %s: %d (%.2f%%)\n", entry.getKey(), entry.getValue(),
                            totalCases > 0 ? (double) entry.getValue() / totalCases * 100 : 0.0));
                }
                break;
            default:
                metrics.append(String.format("Total Cases: %d\n", totalCases));
                metrics.append(String.format("Doctors Treating Condition: %d\n", doctorCount));
        }

        return metrics.toString();
    }

    private String aggregateFacilityMetrics(String facilityId, String metricType) {
        StringBuilder metrics = new StringBuilder();

        if (facilityId != null && !facilityId.trim().isEmpty()) {
            // Aggregate for specific facility
            Optional<Facility> facilityOpt = facilityRepository.findById(facilityId);
            if (facilityOpt.isEmpty()) {
                return String.format("Facility not found: %s\n", facilityId);
            }

            Facility facility = facilityOpt.get();
            metrics.append(String.format("Facility: %s\n", facility.name()));
            metrics.append(String.format("Type: %s\n", facility.facilityType()));
            metrics.append(String.format("Capacity: %d\n", facility.capacity() != null ? facility.capacity() : 0));

            // Query clinical experiences for doctors affiliated with this facility
            // This would require a join or graph query - simplified for now
            if (graphService.graphExists()) {
                try {
                    String cypherQuery = """
                            MATCH (f:Facility {id: $facilityId})<-[:AFFILIATED_WITH]-(d:Doctor)-[:TREATED]->(c:MedicalCase)
                            RETURN count(DISTINCT d) as doctorCount, count(DISTINCT c) as caseCount
                            """;
                    Map<String, Object> params = new HashMap<>();
                    params.put("facilityId", facilityId);
                    List<Map<String, Object>> results = graphService.executeCypher(cypherQuery, params);
                    if (!results.isEmpty()) {
                        Object doctorCountObj = results.get(0).get("doctorCount");
                        Object caseCountObj = results.get(0).get("caseCount");
                        if (doctorCountObj != null) {
                            metrics.append(String.format("Affiliated Doctors: %s\n", doctorCountObj));
                        }
                        if (caseCountObj != null) {
                            metrics.append(String.format("Total Cases: %s\n", caseCountObj));
                        }
                    }
                } catch (Exception e) {
                    log.debug("Could not query graph for facility metrics", e);
                }
            }
        } else {
            // Aggregate across all facilities
            List<Facility> facilities = facilityRepository.findAll();
            int totalFacilities = facilities.size();
            int totalCapacity = 0;
            Map<String, Integer> typeDistribution = new HashMap<>();

            for (Facility facility : facilities) {
                if (facility.capacity() != null) {
                    totalCapacity += facility.capacity();
                }
                if (facility.facilityType() != null) {
                    typeDistribution.merge(facility.facilityType(), 1, Integer::sum);
                }
            }

            metrics.append(String.format("Total Facilities: %d\n", totalFacilities));
            metrics.append(String.format("Total Capacity: %d\n", totalCapacity));
            metrics.append("Type Distribution:\n");
            for (Map.Entry<String, Integer> entry : typeDistribution.entrySet()) {
                metrics.append(String.format("  %s: %d\n", entry.getKey(), entry.getValue()));
            }
        }

        return metrics.toString();
    }

    // ============================================
    // Routing Planner Tools
    // ============================================

    @Tool(description = "Query graph to find candidate facilities/centers for a specific condition.")
    public List<String> graph_query_candidate_centers(
            @ToolParam(description = "ICD-10 code for the condition") String conditionCode,
            @ToolParam(description = "Maximum number of facilities (default: 10)") Integer maxResults
    ) {
        log.info("graph_query_candidate_centers() tool called - conditionCode: {}, maxResults: {}", conditionCode, maxResults);
        String sessionId = getSessionId();
        logStreamService.logToolCall(sessionId, "graph_query_candidate_centers",
                String.format("conditionCode: %s, maxResults: %s", conditionCode, maxResults));

        try {
            if (conditionCode == null || conditionCode.trim().isEmpty()) {
                log.warn("Condition code is required for graph_query_candidate_centers");
                logStreamService.logError(sessionId, "graph_query_candidate_centers failed", "Condition code is required");
                return List.of("Error: Condition code is required");
            }

            int limit = (maxResults != null && maxResults > 0) ? maxResults : 10;

            // Check if graph exists
            if (!graphService.graphExists()) {
                log.debug("Graph does not exist, returning empty results");
                logStreamService.logToolResult(sessionId, "graph_query_candidate_centers",
                        "Graph not available - returning empty results");
                return List.of("Graph not available. Please ensure Apache AGE graph is populated.");
            }

            // Cypher query to find facilities connected to cases with matching ICD-10 code
            String cypherQuery = """
                    MATCH (f:Facility)<-[:AFFILIATED_WITH]-(d:Doctor)-[:TREATED]->(c:MedicalCase)-[:HAS_CONDITION]->(i:ICD10Code {code: $conditionCode})
                    RETURN DISTINCT f.id as facilityId, count(DISTINCT c) as caseCount
                    ORDER BY caseCount DESC
                    LIMIT $maxResults
                    """;

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("conditionCode", conditionCode);
            parameters.put("maxResults", limit);

            List<Map<String, Object>> results = graphService.executeCypher(cypherQuery, parameters);

            List<String> facilityResults = new ArrayList<>();
            for (Map<String, Object> row : results) {
                Object facilityIdObj = row.get("facilityId");
                Object caseCountObj = row.get("caseCount");

                if (facilityIdObj != null) {
                    String facilityId = facilityIdObj.toString();
                    long caseCount = caseCountObj != null ? Long.parseLong(caseCountObj.toString()) : 0;
                    facilityResults.add(String.format("Facility ID: %s, Cases: %d", facilityId, caseCount));
                }
            }

            if (facilityResults.isEmpty()) {
                logStreamService.logToolResult(sessionId, "graph_query_candidate_centers",
                        "No facilities found for condition: " + conditionCode);
                return List.of("No facilities found for condition code: " + conditionCode);
            }

            logStreamService.logToolResult(sessionId, "graph_query_candidate_centers",
                    String.format("Found %d facilities", facilityResults.size()));
            return facilityResults;
        } catch (Exception e) {
            log.error("Error querying candidate centers from graph", e);
            logStreamService.logError(sessionId, "graph_query_candidate_centers failed", e.getMessage());
            return List.of("Error querying graph: " + e.getMessage());
        }
    }

    @Tool(description = "Score a facility-case routing match using Semantic Graph Retrieval (combines complexity match, historical outcomes, capacity, and geography).")
    public com.berdachuk.medexpertmatch.retrieval.domain.RouteScoreResult semantic_graph_retrieval_route_score(
            @ToolParam(description = "Medical case ID") String caseId,
            @ToolParam(description = "Facility ID") String facilityId
    ) {
        log.info("semantic_graph_retrieval_route_score() tool called - caseId: {}, facilityId: {}", caseId, facilityId);
        String sessionId = getSessionId();
        logStreamService.logToolCall(sessionId, "semantic_graph_retrieval_route_score",
                String.format("caseId: %s, facilityId: %s", caseId, facilityId));

        try {
            Optional<MedicalCase> caseOpt = medicalCaseRepository.findById(caseId);
            Optional<Facility> facilityOpt = facilityRepository.findById(facilityId);

            if (caseOpt.isEmpty()) {
                log.warn("Medical case not found - caseId: {}", caseId);
                logStreamService.logError(sessionId, "semantic_graph_retrieval_route_score failed", "Medical case not found: " + caseId);
                throw new IllegalArgumentException("Medical case not found: " + caseId);
            }

            if (facilityOpt.isEmpty()) {
                log.warn("Facility not found - facilityId: {}", facilityId);
                logStreamService.logError(sessionId, "semantic_graph_retrieval_route_score failed", "Facility not found: " + facilityId);
                throw new IllegalArgumentException("Facility not found: " + facilityId);
            }

            RouteScoreResult result = semanticGraphRetrievalService.semanticGraphRetrievalRouteScore(caseOpt.get(), facilityOpt.get());
            logStreamService.logToolResult(sessionId, "semantic_graph_retrieval_route_score",
                    String.format("Route score: %.2f", result.overallScore()));
            return result;
        } catch (IllegalArgumentException e) {
            logStreamService.logError(sessionId, "semantic_graph_retrieval_route_score failed", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error scoring facility-case routing match", e);
            logStreamService.logError(sessionId, "semantic_graph_retrieval_route_score failed", e.getMessage());
            throw e;
        }
    }

    @Tool(description = "Match facilities for case routing with scoring and ranking. Returns list of facility matches sorted by route score.")
    public List<FacilityMatch> match_facilities_for_case(
            @ToolParam(description = "Medical case ID") String caseId,
            @ToolParam(description = "Maximum number of results (default: 5)") Integer maxResults,
            @ToolParam(description = "Minimum route score threshold (0-100, optional)") Double minScore,
            @ToolParam(description = "List of preferred facility types (optional)") List<String> preferredFacilityTypes,
            @ToolParam(description = "List of required capabilities (optional)") List<String> requiredCapabilities,
            @ToolParam(description = "Maximum distance in kilometers (optional)") Double maxDistanceKm
    ) {
        // Normalize case ID to lowercase for case-insensitive lookup
        String normalizedCaseId = caseId != null ? caseId.trim().toLowerCase() : null;

        log.info("match_facilities_for_case() tool called - caseId: {} (normalized: {}), maxResults: {}, minScore: {}, preferredFacilityTypes: {}, requiredCapabilities: {}, maxDistanceKm: {}",
                caseId, normalizedCaseId, maxResults, minScore, preferredFacilityTypes, requiredCapabilities, maxDistanceKm);

        RoutingOptions options = RoutingOptions.builder()
                .maxResults(maxResults != null && maxResults > 0 ? maxResults : 5)
                .minScore(minScore)
                .preferredFacilityTypes(preferredFacilityTypes)
                .requiredCapabilities(requiredCapabilities)
                .maxDistanceKm(maxDistanceKm)
                .build();

        return matchingService.matchFacilitiesForCase(normalizedCaseId, options);
    }
}
