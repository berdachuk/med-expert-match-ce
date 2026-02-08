package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.core.util.LlmCallLimiter;
import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import com.berdachuk.medexpertmatch.embedding.service.EmbeddingService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.llm.tools.MedicalAgentTools;
import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.medicalcase.service.MedicalCaseDescriptionService;
import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;
import com.berdachuk.medexpertmatch.retrieval.domain.FacilityMatch;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Medical agent service implementation.
 * Orchestrates agent skills, tool invocations, and response generation using Spring AI ChatClient.
 */
@Slf4j
@Service
public class MedicalAgentServiceImpl implements MedicalAgentService {

    private static final int MAX_CASES_FOR_QUEUE = 20;
    private static final Pattern URGENCY_PATTERN = Pattern.compile("\"urgencyLevel\"\\s*:\\s*\"(CRITICAL|HIGH|MEDIUM|LOW)\"");
    private static final Pattern SPECIALTY_PATTERN = Pattern.compile("\"requiredSpecialty\"\\s*:\\s*\"([^\"]+)\"");
    private final ChatClient chatClient;  // Tool call LLM for tool calling
    private final ChatClient medGemmaChatClient;  // MedGemma for medical reasoning
    private final String functionGemmaModelName;  // Model name for logging
    private final String medGemmaModelName;  // Model name for logging
    private final MedicalCaseRepository medicalCaseRepository;
    private final EmbeddingService embeddingService;
    private final MedicalCaseDescriptionService descriptionService;
    private final PromptTemplate medgemmaCaseAnalysisSystemPromptTemplate;
    private final PromptTemplate medgemmaCaseAnalysisUserPromptTemplate;
    private final PromptTemplate medgemmaResultInterpretationSystemPromptTemplate;
    private final PromptTemplate medgemmaResultInterpretationUserPromptTemplate;
    private final ResourceLoader resourceLoader;
    private final String skillsDirectory;
    private final LogStreamService logStreamService;
    private final LlmCallLimiter llmCallLimiter;
    private final MedicalAgentTools medicalAgentTools;

    public MedicalAgentServiceImpl(
            @Qualifier("medicalAgentChatClient") ChatClient chatClient,
            @Qualifier("primaryChatModel") ChatModel primaryChatModel,
            @Qualifier("toolCallingChatModel") ChatModel toolCallingChatModel,
            MedicalCaseRepository medicalCaseRepository,
            EmbeddingService embeddingService,
            MedicalCaseDescriptionService descriptionService,
            @Qualifier("medgemmaCaseAnalysisSystemPromptTemplate") PromptTemplate medgemmaCaseAnalysisSystemPromptTemplate,
            @Qualifier("medgemmaCaseAnalysisUserPromptTemplate") PromptTemplate medgemmaCaseAnalysisUserPromptTemplate,
            @Qualifier("medgemmaResultInterpretationSystemPromptTemplate") PromptTemplate medgemmaResultInterpretationSystemPromptTemplate,
            @Qualifier("medgemmaResultInterpretationUserPromptTemplate") PromptTemplate medgemmaResultInterpretationUserPromptTemplate,
            ResourceLoader resourceLoader,
            @Value("${medexpertmatch.skills.directory:.claude/skills}") String skillsDirectory,
            @Value("${spring.ai.custom.chat.model:medgemma:1.5-4b}") String medGemmaModelName,
            @Value("${spring.ai.custom.tool-calling.model:functiongemma}") String functionGemmaModelName,
            LogStreamService logStreamService,
            LlmCallLimiter llmCallLimiter,
            MedicalAgentTools medicalAgentTools) {
        this.chatClient = chatClient;
        this.medGemmaChatClient = ChatClient.builder(primaryChatModel).build();
        this.functionGemmaModelName = functionGemmaModelName;
        this.medGemmaModelName = medGemmaModelName;
        this.medicalCaseRepository = medicalCaseRepository;
        this.embeddingService = embeddingService;
        this.descriptionService = descriptionService;
        this.medgemmaCaseAnalysisSystemPromptTemplate = medgemmaCaseAnalysisSystemPromptTemplate;
        this.medgemmaCaseAnalysisUserPromptTemplate = medgemmaCaseAnalysisUserPromptTemplate;
        this.medgemmaResultInterpretationSystemPromptTemplate = medgemmaResultInterpretationSystemPromptTemplate;
        this.medgemmaResultInterpretationUserPromptTemplate = medgemmaResultInterpretationUserPromptTemplate;
        this.resourceLoader = resourceLoader;
        this.skillsDirectory = skillsDirectory;
        this.logStreamService = logStreamService;
        this.llmCallLimiter = llmCallLimiter;
        this.medicalAgentTools = medicalAgentTools;
        log.info("MedicalAgentServiceImpl initialized with Tool call LLM ({} - tool calling) and MedGemma ({} - medical reasoning)",
                functionGemmaModelName, medGemmaModelName);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AgentResponse matchDoctors(String caseId, Map<String, Object> request) {
        log.info("matchDoctors() called - caseId: {}", caseId);
        String sessionId = (String) request.getOrDefault("sessionId", "default");
        log.info("Using sessionId: {} for log streaming", sessionId);

        // Set session ID in thread-local for tools to access
        logStreamService.setCurrentSessionId(sessionId);

        logStreamService.logMatchDoctorsStep(sessionId, "Starting match doctors operation", "Case ID: " + caseId);
        logStreamService.sendProgress(sessionId, 5);

        String response;
        try {
            // Step 1: Analyze case with MedGemma
            logStreamService.sendLog(sessionId, "INFO", "Step 1: MedGemma case analysis", "Analyzing case with MedGemma");
            logStreamService.sendProgress(sessionId, 15);
            String caseAnalysisJson = analyzeCaseWithMedGemma(caseId);
            logStreamService.sendLog(sessionId, "INFO", "MedGemma analysis complete", "Case analysis received from MedGemma");
            logStreamService.sendProgress(sessionId, 35);

            // Step 2: Call match_doctors_to_case tool directly
            logStreamService.sendProgress(sessionId, 45);
            Integer maxResults = (Integer) request.getOrDefault("maxResults", 10);
            List<DoctorMatch> matches = medicalAgentTools.match_doctors_to_case(caseId, maxResults, null, null, null);

            // Convert matches to JSON string for interpretation
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonResponse = objectMapper.writeValueAsString(matches);
            logStreamService.sendProgress(sessionId, 55);

            // Step 3: Interpret results with MedGemma
            logStreamService.sendLog(sessionId, "INFO", "Step 3: MedGemma result interpretation", "Interpreting tool results with MedGemma");
            logStreamService.sendProgress(sessionId, 65);
            response = interpretResultsWithMedGemma(jsonResponse, caseAnalysisJson);
            logStreamService.sendLog(sessionId, "INFO", "MedGemma interpretation complete", "Final response generated");
            logStreamService.sendProgress(sessionId, 85);

        } catch (Exception e) {
            log.error("Error in matchDoctors", e);
            logStreamService.logError(sessionId, "Unexpected error", e.getMessage() + "\n" + getStackTrace(e));
            // Try to provide at least MedGemma analysis on error
            try {
                String caseAnalysis = analyzeCaseWithMedGemma(caseId);
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
        metadata.put("medgemmaUsed", true);
        metadata.put("toolLlmUsed", false);

        logStreamService.sendProgress(sessionId, 100);
        logStreamService.logCompletion(sessionId, "Match doctors operation", "Successfully matched doctors for case: " + caseId);

        // Clear session ID from thread-local
        logStreamService.clearCurrentSessionId();

        return new AgentResponse(response, metadata);
    }

    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    @Override
    public AgentResponse networkAnalytics(Map<String, Object> request) {
        log.info("networkAnalytics() called");
        String sessionId = (String) request.getOrDefault("sessionId", "default");
        logStreamService.setCurrentSessionId(sessionId);

        try {
            logStreamService.sendLog(sessionId, "INFO", "Network analytics", "Starting network analytics (run tools, then summarize)");

            List<String> conditionCodes = resolveConditionCodes(request);
            logStreamService.sendLog(sessionId, "INFO", "Network analytics", "Condition codes: " + conditionCodes);

            StringBuilder raw = new StringBuilder();
            int maxConditions = Math.min(3, conditionCodes.size());
            int maxExpertsPerCondition = 10;

            for (int i = 0; i < maxConditions; i++) {
                String code = conditionCodes.get(i);
                logStreamService.sendLog(sessionId, "INFO", "Graph query", "Querying top experts for condition: " + code);
                List<String> experts = medicalAgentTools.graph_query_top_experts(code, maxExpertsPerCondition);
                raw.append("## Top experts for condition ").append(code).append("\n");
                for (String line : experts) {
                    raw.append("- ").append(line).append("\n");
                }
                raw.append("\n");
            }

            logStreamService.sendLog(sessionId, "INFO", "Aggregate metrics", "Aggregating condition and doctor metrics");
            String conditionMetrics = medicalAgentTools.aggregate_metrics("CONDITION", null, "PERFORMANCE");
            String doctorMetrics = medicalAgentTools.aggregate_metrics("DOCTOR", null, "PERFORMANCE");
            raw.append("## Aggregate metrics (condition)\n").append(conditionMetrics).append("\n");
            raw.append("## Aggregate metrics (doctor)\n").append(doctorMetrics).append("\n");

            logStreamService.sendLog(sessionId, "INFO", "MedGemma", "Summarizing results for user");
            String response = summarizeNetworkAnalyticsResults(raw.toString());

            logStreamService.logCompletion(sessionId, "Network analytics", "Successfully completed network analytics");

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("skills", List.of("network-analyzer"));
            metadata.put("conditionCodes", conditionCodes);

            return new AgentResponse(response, metadata);
        } catch (Exception e) {
            log.error("Error in network analytics", e);
            logStreamService.logError(sessionId, "Network analytics failed", e.getMessage());
            throw e;
        } finally {
            logStreamService.clearCurrentSessionId();
        }
    }

    private List<String> resolveConditionCodes(Map<String, Object> request) {
        Object single = request.get("conditionCode");
        if (single != null && !single.toString().isBlank()) {
            return List.of(single.toString().trim());
        }
        @SuppressWarnings("unchecked")
        List<String> list = (List<String>) request.get("conditionCodes");
        if (list != null && !list.isEmpty()) {
            return list.stream().filter(c -> c != null && !c.isBlank()).map(String::trim).limit(5).toList();
        }
        List<String> caseIds = medicalCaseRepository.findAllIds(30);
        if (caseIds.isEmpty()) {
            return List.of("I21.9");
        }
        List<MedicalCase> cases = medicalCaseRepository.findByIds(caseIds);
        java.util.Set<String> codes = new java.util.LinkedHashSet<>();
        for (MedicalCase c : cases) {
            if (c.icd10Codes() != null) {
                codes.addAll(c.icd10Codes());
            }
        }
        if (codes.isEmpty()) {
            return List.of("I21.9");
        }
        return new java.util.ArrayList<>(codes).subList(0, Math.min(5, codes.size()));
    }

    private String summarizeNetworkAnalyticsResults(String rawResults) {
        String prompt = """
                Summarize the following network analytics results for the user in 1-2 short paragraphs.
                Focus on key findings: which conditions were analyzed, how many experts, and main metrics.
                Do not include tool names, step numbers, code snippets, or procedural text.
                Write in plain language for a medical or analytics reader.
                Raw results:
                %s
                """.formatted(rawResults);
        try {
            return llmCallLimiter.execute(LlmClientType.CHAT, () ->
                    medGemmaChatClient.prompt().user(prompt).call().content());
        } catch (Exception e) {
            log.warn("Summarization failed, returning raw results", e);
            return rawResults;
        }
    }

    @Override
    public AgentResponse analyzeCase(String caseId, Map<String, Object> request) {
        log.info("analyzeCase() called - caseId: {}", caseId);
        String sessionId = (String) request.getOrDefault("sessionId", "default");
        logStreamService.setCurrentSessionId(sessionId);

        try {
            // Step 1: Analyze case with MedGemma
            logStreamService.sendLog(sessionId, "INFO", "MedGemma case analysis", "Starting comprehensive case analysis");
            String caseAnalysis = analyzeCaseWithMedGemma(caseId);

            // Step 2: Call evidence tools directly (guarantees search_clinical_guidelines and query_pubmed run)
            String condition = null;
            String specialty = null;
            String pubmedQuery = null;
            Optional<MedicalCase> caseOpt = medicalCaseRepository.findById(caseId);
            if (caseOpt.isPresent()) {
                MedicalCase mc = caseOpt.get();
                if (mc.icd10Codes() != null && !mc.icd10Codes().isEmpty()) {
                    condition = mc.icd10Codes().get(0);
                }
                if (condition == null || condition.isBlank()) {
                    condition = mc.currentDiagnosis() != null && !mc.currentDiagnosis().isBlank()
                            ? mc.currentDiagnosis() : mc.chiefComplaint();
                }
                specialty = mc.requiredSpecialty();
                pubmedQuery = mc.chiefComplaint() != null && !mc.chiefComplaint().isBlank()
                        ? mc.chiefComplaint()
                        : (mc.currentDiagnosis() != null ? mc.currentDiagnosis() : condition);
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
                    String.format("Evidence retrieved: search_clinical_guidelines (%d), query_pubmed (%d articles)", guidelines.size(), pubmedArticleCount));

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
            log.info("Case analysis evidence retrieved (caseId: {}), guidelines: {}, pubmed articles: {}", caseId, guidelines.size(), pubmedArticleCount);

            // Step 3: Use MedGemma to interpret and enhance the final response
            logStreamService.sendLog(sessionId, "INFO", "MedGemma result interpretation", "Interpreting analysis results");
            String response = interpretResultsWithMedGemma(toolResults, caseAnalysis);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("caseId", caseId);
            metadata.put("skills", List.of("case-analyzer", "evidence-retriever", "recommendation-engine"));
            metadata.put("hybridApproach", true);
            metadata.put("medgemmaUsed", true);

            logStreamService.clearCurrentSessionId();
            return new AgentResponse(response, metadata);
        } catch (Exception e) {
            log.error("Error in hybrid analyzeCase", e);
            logStreamService.logError(sessionId, "Case analysis failed", e.getMessage());
            throw e;
        }
    }

    @Override
    public AgentResponse generateRecommendations(String matchId, Map<String, Object> request) {
        log.info("generateRecommendations() called - matchId: {}", matchId);
        String sessionId = (String) request.getOrDefault("sessionId", "default");
        logStreamService.setCurrentSessionId(sessionId);

        try {
            // Extract caseId from matchId or request if available
            String caseId = (String) request.get("caseId");
            if (caseId == null && matchId != null) {
                // Try to extract caseId from matchId (format may vary)
                caseId = matchId;
            }

            String toolResults = null;
            if (caseId != null) {
                // Use Tool call LLM for tool orchestration
                String doctorMatcherSkill = loadSkill("doctor-matcher");
                String prompt = buildPrompt(
                        List.of(doctorMatcherSkill),
                        String.format("Generate expert recommendations for match %s. Use tools to get doctor and match details.", matchId),
                        request
                );

                log.info("Sending prompt to LLM for recommendation generation (model: {}, matchId: {}):\n{}", functionGemmaModelName, matchId, prompt);
                log.info("Calling LLM model: {} for recommendation generation (matchId: {})", functionGemmaModelName, matchId);
                logStreamService.sendLog(sessionId, "INFO", "LLM Call",
                        String.format("Calling model: %s for recommendation generation", functionGemmaModelName));
                toolResults = llmCallLimiter.execute(LlmClientType.TOOL_CALLING, () -> {
                    return chatClient.prompt()
                            .user(prompt)
                            .call()
                            .content();
                });
                log.info("LLM model: {} completed recommendation generation (matchId: {}), response length: {}",
                        functionGemmaModelName, matchId, toolResults != null ? toolResults.length() : 0);

                // Use MedGemma to enhance recommendations with medical reasoning
                String caseAnalysis = analyzeCaseWithMedGemma(caseId);
                String response = interpretResultsWithMedGemma(toolResults, caseAnalysis);

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("matchId", matchId);
                metadata.put("skills", List.of("doctor-matcher"));
                metadata.put("hybridApproach", true);
                metadata.put("medgemmaUsed", true);

                logStreamService.clearCurrentSessionId();
                return new AgentResponse(response, metadata);
            }
        } catch (Exception e) {
            log.warn("Error in hybrid generateRecommendations, falling back to standard approach", e);
        }

        // Fallback to standard approach
        String doctorMatcherSkill = loadSkill("doctor-matcher");
        String prompt = buildPrompt(
                List.of(doctorMatcherSkill),
                String.format("Generate expert recommendations for match %s.", matchId),
                request
        );

        log.info("Sending prompt to LLM for recommendation generation fallback (model: {}, matchId: {}):\n{}", functionGemmaModelName, matchId, prompt);
        log.info("Calling LLM model: {} for recommendation generation (fallback, matchId: {})", functionGemmaModelName, matchId);
        String response = llmCallLimiter.execute(LlmClientType.TOOL_CALLING, () -> {
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        });
        log.info("LLM model: {} completed recommendation generation (fallback, matchId: {}), response length: {}",
                functionGemmaModelName, matchId, response != null ? response.length() : 0);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("matchId", matchId);
        metadata.put("skills", List.of("doctor-matcher"));

        logStreamService.clearCurrentSessionId();
        return new AgentResponse(response, metadata);
    }

    @Override
    public AgentResponse routeCase(String caseId, Map<String, Object> request) {
        log.info("routeCase() called - caseId: {}", caseId);
        String sessionId = (String) request.getOrDefault("sessionId", "default");
        logStreamService.setCurrentSessionId(sessionId);

        try {
            // Step 1: Analyze case with MedGemma for routing context
            logStreamService.sendLog(sessionId, "INFO", "MedGemma routing analysis", "Analyzing case for routing");
            String caseAnalysis = analyzeCaseWithMedGemma(caseId);

            // Step 2: Call routing tools directly (match_facilities_for_case)
            logStreamService.sendLog(sessionId, "INFO", "Routing tools", "Calling match_facilities_for_case");
            List<FacilityMatch> facilityMatches = medicalAgentTools.match_facilities_for_case(
                    caseId, 5, null, null, null, null);
            logStreamService.sendLog(sessionId, "INFO", "Routing tools",
                    String.format("match_facilities_for_case returned %d facility matches", facilityMatches != null ? facilityMatches.size() : 0));

            StringBuilder raw = new StringBuilder();
            raw.append("## Facility routing matches (match_facilities_for_case)\n");
            if (facilityMatches == null || facilityMatches.isEmpty()) {
                raw.append("No facility matches found for this case.\n");
            } else {
                for (FacilityMatch m : facilityMatches) {
                    var f = m.facility();
                    raw.append("- Rank ").append(m.rank()).append(": ")
                            .append(f != null ? f.name() : "Unknown").append(" (")
                            .append(f != null && f.facilityType() != null ? f.facilityType() : "N/A").append("), ")
                            .append(f != null && f.locationCity() != null ? f.locationCity() : "").append(" ")
                            .append(f != null && f.locationState() != null ? f.locationState() : "").append("; ")
                            .append("score: ").append(String.format("%.1f", m.routeScore())).append("; ")
                            .append(m.rationale() != null ? m.rationale() : "").append("\n");
                }
            }
            String toolResults = raw.toString();

            // Step 3: Use MedGemma to summarize routing results for the user
            logStreamService.sendLog(sessionId, "INFO", "MedGemma routing interpretation", "Summarizing routing results");
            String response = summarizeRoutingResults(toolResults, caseAnalysis);

            logStreamService.logCompletion(sessionId, "Case routing", "Successfully completed facility routing");

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("caseId", caseId);
            metadata.put("skills", List.of("case-analyzer", "routing-planner"));
            metadata.put("hybridApproach", true);
            metadata.put("medgemmaUsed", true);
            metadata.put("facilityMatchCount", facilityMatches != null ? facilityMatches.size() : 0);

            logStreamService.clearCurrentSessionId();
            return new AgentResponse(response, metadata);
        } catch (Exception e) {
            log.error("Error in routeCase", e);
            logStreamService.logError(sessionId, "Case routing failed", e.getMessage());
            throw e;
        }
    }

    private String summarizeRoutingResults(String rawToolResults, String caseAnalysis) {
        String prompt = """
                Summarize the following facility routing results for the user in 1-2 short paragraphs.
                Use the case analysis for context. Focus on: recommended facilities, why they match, and any next steps.
                Do not include tool names or procedural text. Write in plain language for a medical or operations reader.
                Case analysis (context):
                %s
                
                Routing tool results:
                %s
                """.formatted(caseAnalysis != null ? caseAnalysis : "", rawToolResults != null ? rawToolResults : "");
        try {
            return llmCallLimiter.execute(LlmClientType.CHAT, () ->
                    medGemmaChatClient.prompt().user(prompt).call().content());
        } catch (Exception e) {
            log.warn("Routing summarization failed, returning raw results", e);
            return rawToolResults != null ? rawToolResults : "No routing results available.";
        }
    }

    /**
     * Loads skill instructions from .claude/skills/{skillName}/SKILL.md file.
     */
    private String loadSkill(String skillName) {
        try {
            String skillPath = skillsDirectory + "/" + skillName + "/SKILL.md";
            Resource resource = resourceLoader.getResource("classpath:" + skillPath);
            if (!resource.exists()) {
                // Try file system path
                resource = resourceLoader.getResource("file:" + skillPath);
            }
            if (resource.exists()) {
                return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            } else {
                log.warn("Skill file not found: {}", skillPath);
                return "Skill instructions not available for: " + skillName;
            }
        } catch (IOException e) {
            log.error("Failed to load skill: {}", skillName, e);
            return "Error loading skill: " + skillName;
        }
    }

    /**
     * Analyzes a medical case using MedGemma for medical reasoning.
     * Extracts specialty, urgency, clinical findings, and ICD-10 codes.
     *
     * @param caseId The medical case ID
     * @return Case analysis result as JSON string
     */
    private String analyzeCaseWithMedGemma(String caseId) {
        log.info("Analyzing case {} with MedGemma", caseId);
        String sessionId = logStreamService.getCurrentSessionId();
        logStreamService.sendLog(sessionId, "INFO", "MedGemma case analysis", "Starting case analysis for: " + caseId);

        try {
            // Load medical case
            MedicalCase medicalCase = medicalCaseRepository.findById(caseId)
                    .orElseThrow(() -> new IllegalArgumentException("Medical case not found: " + caseId));

            // Build prompt with case data
            Map<String, Object> variables = new HashMap<>();
            variables.put("caseId", caseId);
            variables.put("patientAge", medicalCase.patientAge() != null ? medicalCase.patientAge().toString() : "Not provided");
            variables.put("chiefComplaint", medicalCase.chiefComplaint() != null ? medicalCase.chiefComplaint() : "");
            variables.put("symptoms", medicalCase.symptoms() != null ? medicalCase.symptoms() : "");
            variables.put("additionalNotes", medicalCase.additionalNotes() != null ? medicalCase.additionalNotes() : "");

            String systemPrompt = medgemmaCaseAnalysisSystemPromptTemplate.render(Collections.emptyMap());
            String userPrompt = medgemmaCaseAnalysisUserPromptTemplate.render(variables);

            // Call MedGemma
            log.info("Sending prompt to LLM for case analysis (model: {}, caseId: {})", medGemmaModelName, caseId);
            log.debug("System prompt: {}", systemPrompt);
            log.debug("User prompt: {}", userPrompt);
            log.info("Calling LLM model: {} for case analysis (caseId: {})", medGemmaModelName, caseId);
            logStreamService.sendLog(sessionId, "INFO", "LLM Call",
                    String.format("Calling model: %s for case analysis", medGemmaModelName));
            String analysis = llmCallLimiter.execute(LlmClientType.CHAT, () -> {
                return medGemmaChatClient.prompt()
                        .system(systemPrompt)
                        .user(userPrompt)
                        .call()
                        .content();
            });

            log.info("LLM model: {} completed case analysis (caseId: {}), response length: {}",
                    medGemmaModelName, caseId, analysis != null ? analysis.length() : 0);
            logStreamService.sendLog(sessionId, "INFO", "MedGemma case analysis",
                    String.format("Analysis completed successfully using model: %s", medGemmaModelName));
            log.debug("MedGemma case analysis result: {}", analysis);
            return analysis;
        } catch (Exception e) {
            log.error("Error analyzing case with MedGemma: {}", caseId, e);
            logStreamService.logError(sessionId, "MedGemma case analysis failed", e.getMessage());
            // Return basic analysis on error
            return String.format("{\"requiredSpecialty\":\"General\",\"urgencyLevel\":\"MEDIUM\",\"clinicalFindings\":[],\"icd10Codes\":[],\"caseSummary\":\"Case %s - Analysis incomplete due to error\"}", caseId);
        }
    }

    @Override
    public AgentResponse prioritizeConsults(Map<String, Object> request) {
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
                logStreamService.sendLog(sessionId, "INFO", "MedGemma urgency analysis", "Analyzing urgency for " + caseIds.size() + " cases");

                List<CaseUrgencyEntry> entries = new ArrayList<>();
                for (String caseId : caseIds) {
                    try {
                        String caseAnalysis = analyzeCaseWithMedGemma(caseId);
                        entries.add(parseUrgencyFromAnalysis(caseId, caseAnalysis));
                    } catch (Exception e) {
                        log.warn("Could not analyze case {} for prioritization", caseId, e);
                        entries.add(parseUrgencyFromAnalysis(caseId, null));
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
                metadata.put("medgemmaUsed", true);
                metadata.put("deterministicOrder", true);

                logStreamService.clearCurrentSessionId();
                return new AgentResponse(response, metadata);
            }
        } catch (Exception e) {
            log.warn("Error in hybrid prioritizeConsults, falling back to standard approach", e);
        }

        // Fallback when no cases in DB or hybrid path failed
        String caseAnalyzerSkill = loadSkill("case-analyzer");
        String userRequest = (caseIds != null && !caseIds.isEmpty())
                ? "Prioritize consultation queue based on case urgency and complexity."
                : "There are no cases in the consultation queue. Briefly describe how consultation queue prioritization would work when cases are present (by urgency and complexity). Do not invent specific case details or doctor names.";
        String prompt = buildPrompt(
                List.of(caseAnalyzerSkill),
                userRequest,
                request
        );

        log.info("Sending prompt to LLM for consult prioritization fallback (model: {}):\n{}", functionGemmaModelName, prompt);
        log.info("Calling LLM model: {} for consult prioritization (fallback)", functionGemmaModelName);
        String response = llmCallLimiter.execute(LlmClientType.TOOL_CALLING, () -> {
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        });
        log.info("LLM model: {} completed consult prioritization (fallback), response length: {}",
                functionGemmaModelName, response != null ? response.length() : 0);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("skills", List.of("case-analyzer"));

        logStreamService.clearCurrentSessionId();
        return new AgentResponse(response, metadata);
    }

    /**
     * Builds a prompt with skill instructions and user request.
     * Framed to avoid Tool call LLM's safety guardrails by emphasizing expert matching, not medical diagnosis.
     */
    private String buildPrompt(List<String> skills, String userRequest, Map<String, Object> requestParams) {
        StringBuilder promptBuilder = new StringBuilder();

        // Frame as expert matching system, not diagnostic tool
        promptBuilder.append("You are an expert matching assistant. Your role is to match healthcare specialists to medical cases.\n\n");
        promptBuilder.append("IMPORTANT: This is NOT a diagnostic system. Medical analysis is handled by MedGemma.\n");
        promptBuilder.append("Your task is to orchestrate tool calls to find matching doctors, not to provide medical diagnosis.\n\n");

        // Add skill instructions (framed as matching guidance)
        promptBuilder.append("Use the following guidance for expert matching:\n\n");
        for (String skill : skills) {
            promptBuilder.append("---\n");
            promptBuilder.append(skill);
            promptBuilder.append("\n---\n\n");
        }

        // Add user request (reframed if needed)
        promptBuilder.append("Task: ").append(userRequest).append("\n");
        promptBuilder.append("Focus on matching specialists to cases, not on medical diagnosis.\n\n");

        // Add request parameters if provided (exclude sessionId - internal, not for LLM)
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

    /**
     * Parses urgency and specialty from MedGemma case analysis output.
     * Tries JSON first, then regex. Defaults to MEDIUM and Unknown on failure.
     */
    private CaseUrgencyEntry parseUrgencyFromAnalysis(String caseId, String caseAnalysis) {
        String urgency = UrgencyLevel.MEDIUM.name();
        String specialty = "Unknown";
        if (caseAnalysis != null && !caseAnalysis.isBlank()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = new ObjectMapper().readValue(caseAnalysis.trim(), Map.class);
                Object u = map.get("urgencyLevel");
                if (u != null) {
                    String uStr = u.toString().toUpperCase();
                    if ("CRITICAL".equals(uStr) || "HIGH".equals(uStr) || "MEDIUM".equals(uStr) || "LOW".equals(uStr)) {
                        urgency = uStr;
                    }
                }
                Object s = map.get("requiredSpecialty");
                if (s != null && s.toString().length() > 0) {
                    specialty = s.toString().trim();
                }
            } catch (Exception e) {
                Matcher um = URGENCY_PATTERN.matcher(caseAnalysis);
                if (um.find()) {
                    urgency = um.group(1);
                }
                Matcher sm = SPECIALTY_PATTERN.matcher(caseAnalysis);
                if (sm.find()) {
                    specialty = sm.group(1).trim();
                }
            }
        }
        return new CaseUrgencyEntry(caseId, urgency, specialty);
    }

    /**
     * Builds prioritization response text from sorted entries. Lists all cases in strict urgency order with exact IDs.
     */
    private String buildPrioritizationResponse(List<CaseUrgencyEntry> entries) {
        String lines = entries.stream()
                .map(e -> "Case " + e.caseId() + " (" + e.specialty() + " - " + e.urgencyLevel() + ")")
                .collect(Collectors.joining("\n"));
        String disclaimer = "Note: This analysis is for research and educational purposes only. Do not use this system for diagnostic decisions without human-in-the-loop validation.";
        return "Prioritized consultation queue (by urgency):\n\n" + lines + "\n\n" + disclaimer;
    }

    /**
     * Generates a tool call plan using Tool call LLM.
     * Frames the task as expert matching, not medical diagnosis, to avoid safety guardrails.
     *
     * @param caseId       The medical case ID
     * @param caseAnalysis The case analysis from MedGemma
     * @return Tool execution plan
     */
    private String generateToolPlanWithToolCallLlm(String caseId, String caseAnalysis) {
        log.info("Generating tool plan with Tool call LLM for case {}", caseId);
        String sessionId = logStreamService.getCurrentSessionId();
        logStreamService.sendLog(sessionId, "INFO", "Tool call LLM tool planning", "Generating tool call plan");

        try {
            // Build prompt that avoids medical diagnosis terminology
            String prompt = buildToolCallingPrompt(caseId, caseAnalysis);

            // Call Tool call LLM for tool orchestration
            log.info("Sending prompt to LLM for tool orchestration (model: {}, caseId: {}):\n{}", functionGemmaModelName, caseId, prompt);
            log.info("Calling LLM model: {} for tool orchestration (caseId: {})", functionGemmaModelName, caseId);
            logStreamService.sendLog(sessionId, "INFO", "LLM Call",
                    String.format("Calling model: %s for tool orchestration", functionGemmaModelName));
            String toolPlan = llmCallLimiter.execute(LlmClientType.TOOL_CALLING, () -> {
                return chatClient.prompt()
                        .user(prompt)
                        .call()
                        .content();
            });

            log.info("LLM model: {} completed tool orchestration (caseId: {}), response length: {}",
                    functionGemmaModelName, caseId, toolPlan != null ? toolPlan.length() : 0);
            logStreamService.sendLog(sessionId, "INFO", "Tool call LLM tool planning",
                    String.format("Tool plan generated successfully using model: %s", functionGemmaModelName));
            log.debug("Tool call LLM tool plan: {}", toolPlan);
            return toolPlan;
        } catch (Exception e) {
            log.error("Error generating tool plan with Tool call LLM: {}", caseId, e);
            logStreamService.logError(sessionId, "Tool call LLM tool planning failed", e.getMessage());
            // Return default tool plan on error
            return String.format("Call query_candidate_doctors with caseId=%s, then score matches", caseId);
        }
    }

    /**
     * Interprets tool execution results using MedGemma for medical reasoning.
     * Generates final response with medical context and reasoning.
     *
     * @param toolResults  The results from tool executions
     * @param caseAnalysis The original case analysis from MedGemma
     * @return Final interpreted response
     */
    private String interpretResultsWithMedGemma(String toolResults, String caseAnalysis) {
        log.info("Interpreting tool results with MedGemma");
        String sessionId = logStreamService.getCurrentSessionId();
        logStreamService.sendLog(sessionId, "INFO", "MedGemma result interpretation", "Interpreting tool results");

        try {
            // Validate inputs
            if (toolResults == null || toolResults.trim().isEmpty()) {
                log.warn("Empty tool results provided to interpretResultsWithMedGemma, returning case analysis");
                return "Based on MedGemma case analysis:\n\n" + caseAnalysis;
            }

            // Limit tool results size to prevent excessive prompts
            String limitedToolResults = toolResults;
            if (toolResults.length() > 3000) {
                log.warn("Tool results too long ({} chars), truncating to 3000 chars", toolResults.length());
                limitedToolResults = toolResults.substring(0, 3000) + "\n\n[Tool results truncated due to length]";
            }

            // Limit case analysis size
            String limitedCaseAnalysis = caseAnalysis;
            if (caseAnalysis != null && caseAnalysis.length() > 1500) {
                log.warn("Case analysis too long ({} chars), truncating to 1500 chars", caseAnalysis.length());
                limitedCaseAnalysis = caseAnalysis.substring(0, 1500) + "\n\n[Case analysis truncated]";
            }

            // Build prompt with case analysis and tool results
            Map<String, Object> variables = new HashMap<>();
            variables.put("caseAnalysis", limitedCaseAnalysis != null ? limitedCaseAnalysis : "No case analysis available");
            variables.put("toolResults", limitedToolResults);

            String systemPrompt = medgemmaResultInterpretationSystemPromptTemplate.render(Collections.emptyMap());
            String userPrompt = medgemmaResultInterpretationUserPromptTemplate.render(variables);

            // Limit prompt size to prevent issues (check combined length)
            int totalPromptLength = systemPrompt.length() + userPrompt.length();
            if (totalPromptLength > 8000) {
                log.warn("Prompt too long ({} chars), truncating user prompt", totalPromptLength);
                int maxUserPromptLength = Math.max(1000, 8000 - systemPrompt.length());
                userPrompt = userPrompt.substring(0, Math.min(userPrompt.length(), maxUserPromptLength)) + "\n\n[Prompt truncated - please provide a concise response]";
            }

            // Call MedGemma for final interpretation
            log.info("Sending prompt to LLM for result interpretation (model: {}, total prompt length: {})", medGemmaModelName, systemPrompt.length() + userPrompt.length());
            log.debug("System prompt: {}", systemPrompt);
            log.debug("User prompt: {}", userPrompt);
            log.info("Calling LLM model: {} for result interpretation (prompt length: {})", medGemmaModelName, systemPrompt.length() + userPrompt.length());
            logStreamService.sendLog(sessionId, "INFO", "LLM Call",
                    String.format("Calling model: %s for result interpretation", medGemmaModelName));
            final String finalSystemPrompt = systemPrompt;
            final String finalUserPrompt = userPrompt;
            String interpretation = llmCallLimiter.execute(LlmClientType.CHAT, () -> {
                return medGemmaChatClient.prompt()
                        .system(finalSystemPrompt)
                        .user(finalUserPrompt)
                        .call()
                        .content();
            });

            // Validate and limit response size to prevent loops
            if (interpretation != null) {
                // Check for repetitive patterns (same JSON repeated)
                if (interpretation.length() > 10000) {
                    log.warn("MedGemma response very long ({} chars), checking for repetition", interpretation.length());
                    // Try to detect repetition by checking if same substring appears multiple times
                    String firstPart = interpretation.substring(0, Math.min(1000, interpretation.length()));
                    int occurrences = (interpretation.length() - interpretation.replace(firstPart, "").length()) / firstPart.length();
                    if (occurrences > 3) {
                        log.warn("Detected repetitive response pattern, truncating to first occurrence");
                        interpretation = firstPart + "\n\n[Response truncated - repetitive content detected]";
                    } else if (interpretation.length() > 15000) {
                        // Hard limit on response size
                        interpretation = interpretation.substring(0, 15000) + "\n\n[Response truncated due to excessive length]";
                    }
                }
            }

            log.info("LLM model: {} completed result interpretation, response length: {}",
                    medGemmaModelName, interpretation != null ? interpretation.length() : 0);
            logStreamService.sendLog(sessionId, "INFO", "MedGemma result interpretation",
                    String.format("Interpretation completed successfully using model: %s, length: %d",
                            medGemmaModelName, interpretation != null ? interpretation.length() : 0));
            log.debug("MedGemma interpretation result (first 500 chars): {}",
                    interpretation != null && interpretation.length() > 500 ? interpretation.substring(0, 500) + "..." : interpretation);
            return interpretation != null ? interpretation : "Error: Empty response from MedGemma";
        } catch (Exception e) {
            log.error("Error interpreting results with MedGemma", e);
            logStreamService.logError(sessionId, "MedGemma result interpretation failed", e.getMessage());
            throw e;
        }
    }

    /**
     * Builds a prompt for Tool call LLM tool calling that avoids medical diagnosis terminology.
     * Frames the task as expert matching/orchestration, not medical reasoning.
     *
     * @param caseId       The medical case ID
     * @param caseAnalysis The case analysis from MedGemma
     * @return Tool calling prompt
     */
    private String buildToolCallingPrompt(String caseId, String caseAnalysis) {

        // CRITICAL: Emphasize the case ID at the very beginning

        String promptBuilder = String.format(
                "CRITICAL: The medical case ID you MUST use is: %s\n" +
                        "REQUIRED: When calling ANY tool that requires a caseId parameter, you MUST use exactly: %s\n" +
                        "FORBIDDEN: Do NOT use placeholder IDs like 'abc1234567', 'test123', or any other invented ID.\n" +
                        "FORBIDDEN: Do NOT generate, invent, or create a different case ID.\n" +
                        "REQUIRED: Copy and paste this exact case ID: %s\n\n",
                caseId, caseId, caseId
        ) +
                "You are an expert matching assistant. Your role is to match healthcare specialists to medical cases.\n\n" +
                "IMPORTANT: This is NOT a diagnostic system. Medical analysis is already completed by MedGemma.\n" +
                "Your task is to:\n" +
                "1. Use the provided case analysis (already completed by MedGemma)\n" +
                "2. Call tools to find matching doctors\n" +
                "3. Generate a recommendation summary\n\n" +
                String.format("REMINDER: Use case ID %s when calling tools. Do NOT invent a different ID.\n\n", caseId) +
                "Case Analysis (from MedGemma):\n" +
                caseAnalysis + "\n\n" +
                "CRITICAL: Available Tools (use ONLY these exact tool names):\n" +
                "- analyze_case: Analyze a medical case by ID (caseId: string)\n" +
                "- query_candidate_doctors: Find doctors matching case requirements\n" +
                "  Parameters: caseId (REQUIRED - use: " + caseId + "), specialty (optional), requireTelehealth (optional), maxResults (optional)\n" +
                "- match_doctors_to_case: Match doctors to a case with scoring (RECOMMENDED for matching)\n" +
                "  Parameters: caseId (REQUIRED - use: " + caseId + "), maxResults (optional), minScore (optional), preferredSpecialties (optional), requireTelehealth (optional)\n" +
                "- score_doctor_match: Score how well a doctor matches a case\n" +
                "  Parameters: doctorId, caseId (REQUIRED - use: " + caseId + ")\n\n" +
                "FORBIDDEN: Do NOT call tools with names that don't exist above.\n" +
                "FORBIDDEN: Do NOT invent tool names like 'query_medical_case', 'get_case', 'find_case', etc.\n" +
                "REQUIRED: Use ONLY the exact tool names listed above.\n\n" +
                "Task: Match doctors to case " + caseId + "\n" +
                "RECOMMENDED: Use 'match_doctors_to_case' tool with caseId: " + caseId + "\n" +
                "Use the tools to find and score matching doctors. Provide a clear summary of matched doctors.\n\n" +
                "CRITICAL OUTPUT LIMITS:\n" +
                "- Provide EXACTLY ONE response and STOP after completing the task\n" +
                "- Do NOT repeat the same content multiple times\n" +
                "- Maximum response length: 2000 words (approximately 10000 characters)\n" +
                "- Stop immediately after providing the response\n" +
                "- Do NOT continue generating after the response is complete";

        return promptBuilder;
    }

    /**
     * Extracts tool call information from ChatClient response.
     * Attempts to identify which tools were called by the LLM.
     *
     * @param callResponse The ChatClient call response (var type)
     * @param content      The response content string
     * @return String describing tools called, or "none" if cannot determine
     */
    private String extractToolCalls(Object callResponse, String content) {
        List<String> foundTools = new ArrayList<>();

        try {
            // Try to get ChatResponse to check for tool calls
            // Use reflection to call chatResponse() method
            ChatResponse chatResponse = null;
            try {
                java.lang.reflect.Method chatResponseMethod = callResponse.getClass().getMethod("chatResponse");
                chatResponse = (ChatResponse) chatResponseMethod.invoke(callResponse);
            } catch (Exception e) {
                log.debug("Could not call chatResponse() method", e);
            }

            if (chatResponse == null) {
                // Try alternative: check if callResponse implements ChatResponse directly
                if (callResponse instanceof ChatResponse) {
                    chatResponse = (ChatResponse) callResponse;
                }
            }

            if (chatResponse != null && chatResponse.getResult() != null) {
                // Check if response has tool calls in metadata
                var metadata = chatResponse.getResult().getMetadata();
                if (metadata != null) {
                    // Check various metadata keys that might contain tool call info
                    for (String key : metadata.keySet()) {
                        if (key.toLowerCase().contains("tool") || key.toLowerCase().contains("function")) {
                            Object value = metadata.get(key);
                            if (value != null) {
                                String valueStr = value.toString();
                                // Extract tool names from metadata value
                                extractToolNamesFromText(valueStr, foundTools);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract tool calls from ChatResponse metadata", e);
        }

        // Parse content for tool call patterns
        if (content != null && foundTools.isEmpty()) {
            // List of all available tools (must match @Tool method names in MedicalAgentTools)
            List<String> knownTools = List.of(
                    "query_candidate_doctors", "score_doctor_match",
                    "match_doctors_to_case",  // CRITICAL: Main tool for doctor matching
                    "analyze_case_text", "get_medical_case_details",
                    "query_facilities", "route_case_to_facility"
            );

            // Method 1: Direct tool name mentions
            for (String tool : knownTools) {
                // Check for tool name with various patterns
                String toolPattern = tool.replace("_", "[_\\s-]");
                Pattern pattern = Pattern.compile(
                        "(?i)\\b" + toolPattern + "\\b",
                        Pattern.MULTILINE
                );
                Matcher matcher = pattern.matcher(content);
                if (matcher.find() && !foundTools.contains(tool)) {
                    foundTools.add(tool);
                }
            }

            // Method 2: Look for function/tool call patterns
            if (foundTools.isEmpty()) {
                // Pattern for "function_name(" or "tool_name("
                Pattern funcPattern = Pattern.compile(
                        "(?i)(?:called|invoked|executed|using)[\\s]+([a-z_]+)(?:\\s|\\()",
                        Pattern.MULTILINE
                );
                Matcher funcMatcher = funcPattern.matcher(content);
                while (funcMatcher.find()) {
                    String potentialTool = funcMatcher.group(1);
                    if (knownTools.contains(potentialTool) && !foundTools.contains(potentialTool)) {
                        foundTools.add(potentialTool);
                    }
                }
            }

            // Method 3: Look for JSON-like tool call structures
            if (foundTools.isEmpty()) {
                Pattern jsonPattern = Pattern.compile(
                        "(?i)[\"'](?:name|function|tool)[\"']\\s*[:=]\\s*[\"']([a-z_]+)[\"']",
                        Pattern.MULTILINE
                );
                Matcher jsonMatcher = jsonPattern.matcher(content);
                while (jsonMatcher.find()) {
                    String potentialTool = jsonMatcher.group(1);
                    if (knownTools.contains(potentialTool) && !foundTools.contains(potentialTool)) {
                        foundTools.add(potentialTool);
                    }
                }
            }

            // Method 4: Look for tool results or tool execution mentions
            if (foundTools.isEmpty()) {
                // Check if content mentions tool results (indicates tools were called)
                for (String tool : knownTools) {
                    String toolDisplayName = tool.replace("_", " ");
                    if (content.toLowerCase().contains(toolDisplayName) && !foundTools.contains(tool)) {
                        foundTools.add(tool);
                    }
                }
            }
        }

        return foundTools.isEmpty() ? "none detected" : String.join(", ", foundTools);
    }

    /**
     * Helper method to extract tool names from text.
     */
    private void extractToolNamesFromText(String text, List<String> foundTools) {
        if (text == null) return;

        List<String> knownTools = List.of(
                "query_candidate_doctors", "score_doctor_match",
                "match_doctors_to_case",  // CRITICAL: Main tool for doctor matching
                "analyze_case_text", "get_medical_case_details",
                "query_facilities", "route_case_to_facility"
        );

        for (String tool : knownTools) {
            if (text.contains(tool) && !foundTools.contains(tool)) {
                foundTools.add(tool);
            }
        }
    }

    @Override
    @Transactional
    public AgentResponse matchFromText(String caseText, Map<String, Object> request) {
        log.info("matchFromText() called - caseText length: {}", caseText != null ? caseText.length() : 0);

        // Validate required caseText
        if (caseText == null || caseText.isBlank()) {
            throw new IllegalArgumentException("caseText is required and cannot be empty");
        }

        String sessionId = (String) request.getOrDefault("sessionId", "default");
        logStreamService.setCurrentSessionId(sessionId);
        logStreamService.sendLog(sessionId, "INFO", "matchFromText started", "Creating case from text input");

        try {
            // Extract optional parameters
            Integer patientAge = extractInteger(request, "patientAge");
            String caseTypeStr = extractString(request, "caseType");
            String urgencyLevelStr = extractString(request, "urgencyLevel");
            String symptoms = extractString(request, "symptoms");
            String additionalNotes = extractString(request, "additionalNotes");

            // Parse caseType or default to INPATIENT
            CaseType caseType = CaseType.INPATIENT;
            if (caseTypeStr != null && !caseTypeStr.isBlank()) {
                try {
                    caseType = CaseType.valueOf(caseTypeStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid caseType: {}, using default INPATIENT", caseTypeStr);
                }
            }

            // Parse urgencyLevel or default to MEDIUM
            UrgencyLevel urgencyLevel = UrgencyLevel.MEDIUM;
            if (urgencyLevelStr != null && !urgencyLevelStr.isBlank()) {
                try {
                    urgencyLevel = UrgencyLevel.valueOf(urgencyLevelStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid urgencyLevel: {}, using default MEDIUM", urgencyLevelStr);
                }
            }

            // Validate patientAge if provided
            if (patientAge != null && patientAge <= 0) {
                throw new IllegalArgumentException("patientAge must be positive if provided");
            }

            // Generate case ID
            String caseId = IdGenerator.generateId();
            logStreamService.sendLog(sessionId, "INFO", "Case ID generated", "Case ID: " + caseId);

            // Create MedicalCase entity
            MedicalCase medicalCase = new MedicalCase(
                    caseId,
                    patientAge,
                    caseText,
                    symptoms,
                    null, // currentDiagnosis
                    List.of(), // icd10Codes
                    List.of(), // snomedCodes
                    urgencyLevel, // urgencyLevel - parsed from request or default MEDIUM
                    null, // requiredSpecialty
                    caseType,
                    additionalNotes,
                    null  // abstractText
            );

            // Insert case
            logStreamService.sendLog(sessionId, "INFO", "Inserting case", "Saving case to database");
            medicalCaseRepository.insert(medicalCase);
            logStreamService.sendLog(sessionId, "INFO", "Case inserted", "Case saved successfully");

            // Generate abstract and embedding
            try {
                logStreamService.sendLog(sessionId, "INFO", "Generating abstract", "Creating comprehensive case description");
                String abstractText = descriptionService.generateDescription(medicalCase);
                medicalCaseRepository.updateAbstract(caseId, abstractText);
                logStreamService.sendLog(sessionId, "INFO", "Abstract stored", "Case description saved for reuse");

                logStreamService.sendLog(sessionId, "INFO", "Generating embedding", "Creating vector embedding for case");
                List<Double> embedding = embeddingService.generateEmbedding(abstractText);

                if (embedding != null && !embedding.isEmpty()) {
                    int dimension = embedding.size();
                    medicalCaseRepository.updateEmbedding(caseId, embedding, dimension);
                    logStreamService.sendLog(sessionId, "INFO", "Embedding generated",
                            String.format("Embedding created with dimension: %d", dimension));
                } else {
                    log.warn("Empty embedding generated for case: {}", caseId);
                    logStreamService.sendLog(sessionId, "WARN", "Empty embedding", "Embedding generation returned empty result");
                }
            } catch (Exception e) {
                log.warn("Failed to generate abstract/embedding for case: {}", caseId, e);
                logStreamService.sendLog(sessionId, "WARN", "Abstract/embedding generation failed",
                        "Continuing without embedding: " + e.getMessage());
                // Continue without embedding - matching will use neutral vector score
            }

            // Call existing matchDoctors method
            // Use REQUIRES_NEW propagation to isolate matching from case creation transaction
            logStreamService.sendLog(sessionId, "INFO", "Matching doctors", "Starting doctor matching process");
            try {
                return matchDoctors(caseId, request);
            } catch (Exception e) {
                // Log error but don't propagate - case creation succeeded, matching can fail independently
                log.error("Error during doctor matching for case {}: {}", caseId, e.getMessage(), e);
                logStreamService.sendLog(sessionId, "ERROR", "Matching failed",
                        "Case created successfully but matching failed: " + e.getMessage());
                // Return a response indicating case was created but matching failed
                Map<String, Object> errorMetadata = new HashMap<>();
                errorMetadata.put("caseId", caseId);
                errorMetadata.put("error", e.getMessage());
                return new AgentResponse(
                        "Medical case created successfully (ID: " + caseId + "), but doctor matching encountered an error: " +
                                e.getMessage() + ". You can try matching again using the case ID.",
                        errorMetadata
                );
            }

        } catch (IllegalArgumentException e) {
            log.error("Validation error in matchFromText: {}", e.getMessage());
            logStreamService.sendLog(sessionId, "ERROR", "Validation error", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error in matchFromText", e);
            logStreamService.sendLog(sessionId, "ERROR", "matchFromText failed", e.getMessage());
            throw new RuntimeException("Failed to match doctors from text: " + e.getMessage(), e);
        }
    }


    /**
     * Extracts integer value from request map.
     */
    private Integer extractInteger(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                log.warn("Invalid integer value for {}: {}", key, value);
                return null;
            }
        }
        return null;
    }

    /**
     * Extracts string value from request map.
     */
    private String extractString(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof String str) {
            return str.isBlank() ? null : str;
        }
        return value.toString();
    }

    /**
     * Extracts tool execution results from ChatClient response.
     * Attempts to get actual tool execution results rather than tool call parameters.
     *
     * @param callResponse The ChatClient call response
     * @return String containing tool execution results or null if none found
     */
    private String extractToolExecutionResults(Object callResponse) {
        String toolResults = null;

        try {
            // Use reflection to get the ChatResponse object
            java.lang.reflect.Method chatResponseMethod = callResponse.getClass().getMethod("chatResponse");
            ChatResponse chatResponse = (ChatResponse) chatResponseMethod.invoke(callResponse);

            // Check if there are tool execution results in the response
            if (chatResponse != null && chatResponse.getResult() != null) {
                // First check if there's a direct tool execution result in the output
                Object outputObj = chatResponse.getResult().getOutput();
                if (outputObj != null) {
                    String output = outputObj.toString();
                    if (!output.trim().isEmpty()) {
                        // Check if this looks like actual tool execution results
                        // (contains structured data like doctor matches rather than just tool call parameters)
                        if (output.contains("\"doctorId\"") ||
                                output.contains("\"score\"") ||
                                output.contains("DoctorMatch") ||
                                output.contains("[{") ||  // JSON array
                                output.contains("\"id\"")) {
                            toolResults = output;
                        }
                    }
                }

                // If we don't have tool results from output, check metadata
                if (toolResults == null || toolResults.trim().isEmpty()) {
                    var metadata = chatResponse.getResult().getMetadata();
                    if (metadata != null) {
                        // Look for tool execution results in metadata
                        // Different LLM providers might put results in different metadata keys
                        for (String key : metadata.keySet()) {
                            if (key.toLowerCase().contains("tool") ||
                                    key.toLowerCase().contains("function") ||
                                    key.toLowerCase().contains("result")) {
                                Object value = metadata.get(key);
                                if (value != null) {
                                    String resultStr = value.toString();
                                    // Check if this looks like actual tool execution results
                                    if (resultStr.contains("\"doctorId\"") ||
                                            resultStr.contains("\"score\"") ||
                                            resultStr.contains("DoctorMatch") ||
                                            resultStr.contains("[{") ||  // JSON array
                                            resultStr.contains("\"id\"")) {
                                        toolResults = resultStr;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract tool execution results from ChatResponse: {}", e.getMessage());
        }

        // If we still don't have tool results, fall back to content but check if it's tool parameters
        if (toolResults == null || toolResults.trim().isEmpty()) {
            try {
                // Use reflection to get content
                java.lang.reflect.Method contentMethod = callResponse.getClass().getMethod("content");
                String content = (String) contentMethod.invoke(callResponse);
                if (content != null) {
                    // Check if content contains tool execution results rather than just tool call parameters
                    if (content.contains("\"doctorId\"") ||
                            content.contains("\"score\"") ||
                            content.contains("DoctorMatch") ||
                            (content.contains("[{") && content.contains("\"id\""))) {
                        toolResults = content;
                    } else if (!content.trim().isEmpty()) {
                        // Content might be tool execution results even if it doesn't match our patterns
                        toolResults = content;
                    }
                }
            } catch (Exception e) {
                log.debug("Could not extract content from callResponse: {}", e.getMessage());
            }
        }

        return toolResults;
    }
}
