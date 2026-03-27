package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.core.util.LlmCallLimiter;
import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentLlmSupportService;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Shared LLM support for workflow-oriented agent services.
 */
@Slf4j
@Service
public class MedicalAgentLlmSupportServiceImpl implements MedicalAgentLlmSupportService {

    private final ChatClient medGemmaChatClient;
    private final String medGemmaModelName;
    private final MedicalCaseRepository medicalCaseRepository;
    private final PromptTemplate medgemmaCaseAnalysisSystemPromptTemplate;
    private final PromptTemplate medgemmaCaseAnalysisUserPromptTemplate;
    private final PromptTemplate medgemmaResultInterpretationSystemPromptTemplate;
    private final PromptTemplate medgemmaResultInterpretationUserPromptTemplate;
    private final LogStreamService logStreamService;
    private final LlmCallLimiter llmCallLimiter;

    public MedicalAgentLlmSupportServiceImpl(
            @Qualifier("primaryChatModel") ChatModel primaryChatModel,
            MedicalCaseRepository medicalCaseRepository,
            @Qualifier("medgemmaCaseAnalysisSystemPromptTemplate") PromptTemplate medgemmaCaseAnalysisSystemPromptTemplate,
            @Qualifier("medgemmaCaseAnalysisUserPromptTemplate") PromptTemplate medgemmaCaseAnalysisUserPromptTemplate,
            @Qualifier("medgemmaResultInterpretationSystemPromptTemplate") PromptTemplate medgemmaResultInterpretationSystemPromptTemplate,
            @Qualifier("medgemmaResultInterpretationUserPromptTemplate") PromptTemplate medgemmaResultInterpretationUserPromptTemplate,
            @Value("${spring.ai.custom.chat.model:medgemma:1.5-4b}") String medGemmaModelName,
            LogStreamService logStreamService,
            LlmCallLimiter llmCallLimiter) {
        this.medGemmaChatClient = ChatClient.builder(primaryChatModel).build();
        this.medGemmaModelName = medGemmaModelName;
        this.medicalCaseRepository = medicalCaseRepository;
        this.medgemmaCaseAnalysisSystemPromptTemplate = medgemmaCaseAnalysisSystemPromptTemplate;
        this.medgemmaCaseAnalysisUserPromptTemplate = medgemmaCaseAnalysisUserPromptTemplate;
        this.medgemmaResultInterpretationSystemPromptTemplate = medgemmaResultInterpretationSystemPromptTemplate;
        this.medgemmaResultInterpretationUserPromptTemplate = medgemmaResultInterpretationUserPromptTemplate;
        this.logStreamService = logStreamService;
        this.llmCallLimiter = llmCallLimiter;
    }

    @Override
    public String analyzeCaseWithMedGemma(String caseId) {
        log.info("Analyzing case {} with LLM", caseId);
        String sessionId = logStreamService.getCurrentSessionId();
        logStreamService.sendLog(sessionId, "INFO", "LLM case analysis", "Starting case analysis for: " + caseId);

        try {
            MedicalCase medicalCase = medicalCaseRepository.findById(caseId)
                    .orElseThrow(() -> new IllegalArgumentException("Medical case not found: " + caseId));

            Map<String, Object> variables = new HashMap<>();
            variables.put("caseId", caseId);
            variables.put("patientAge", medicalCase.patientAge() != null ? medicalCase.patientAge().toString() : "Not provided");
            variables.put("chiefComplaint", medicalCase.chiefComplaint() != null ? medicalCase.chiefComplaint() : "");
            variables.put("symptoms", medicalCase.symptoms() != null ? medicalCase.symptoms() : "");
            variables.put("additionalNotes", medicalCase.additionalNotes() != null ? medicalCase.additionalNotes() : "");

            String systemPrompt = medgemmaCaseAnalysisSystemPromptTemplate.render(Collections.emptyMap());
            String userPrompt = medgemmaCaseAnalysisUserPromptTemplate.render(variables);

            log.info("Sending prompt to LLM for case analysis (model: {}, caseId: {})", medGemmaModelName, caseId);
            log.debug("System prompt: {}", systemPrompt);
            log.debug("User prompt: {}", userPrompt);
            log.info("Calling LLM model: {} for case analysis (caseId: {})", medGemmaModelName, caseId);
            logStreamService.sendLog(sessionId, "INFO", "LLM Call",
                    String.format("Calling model: %s for case analysis", medGemmaModelName));

            String analysis = llmCallLimiter.execute(LlmClientType.CHAT, () -> medGemmaChatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content());

            log.info("LLM model: {} completed case analysis (caseId: {}), response length: {}",
                    medGemmaModelName, caseId, analysis != null ? analysis.length() : 0);
            logStreamService.sendLog(sessionId, "INFO", "LLM case analysis",
                    String.format("Analysis completed successfully using model: %s", medGemmaModelName));
            log.debug("LLM case analysis result: {}", analysis);
            return analysis;
        } catch (Exception e) {
            log.error("Error analyzing case with LLM: {}", caseId, e);
            logStreamService.logError(sessionId, "LLM case analysis failed", e.getMessage());
            return String.format("{\"requiredSpecialty\":\"General\",\"urgencyLevel\":\"MEDIUM\",\"clinicalFindings\":[],\"icd10Codes\":[],\"caseSummary\":\"Case %s - Analysis incomplete due to error\"}", caseId);
        }
    }

    @Override
    public String interpretResultsWithMedGemma(String toolResults, String caseAnalysis, Integer patientAgeFromCase) {
        log.info("Interpreting tool results with LLM");
        String sessionId = logStreamService.getCurrentSessionId();
        logStreamService.sendLog(sessionId, "INFO", "LLM result interpretation", "Interpreting tool results");

        try {
            if (toolResults == null || toolResults.trim().isEmpty()) {
                log.warn("Empty tool results provided, returning case analysis only");
                return "Based on LLM case analysis:\n\n" + caseAnalysis;
            }

            String limitedToolResults = toolResults;
            if (toolResults.length() > 3000) {
                log.warn("Tool results too long ({} chars), truncating to 3000 chars", toolResults.length());
                limitedToolResults = toolResults.substring(0, 3000) + "\n\n[Tool results truncated due to length]";
            }

            String limitedCaseAnalysis = caseAnalysis;
            if (caseAnalysis != null && caseAnalysis.length() > 1500) {
                log.warn("Case analysis too long ({} chars), truncating to 1500 chars", caseAnalysis.length());
                limitedCaseAnalysis = caseAnalysis.substring(0, 1500) + "\n\n[Case analysis truncated]";
            }

            Map<String, Object> variables = new HashMap<>();
            variables.put("caseAnalysis", limitedCaseAnalysis != null ? limitedCaseAnalysis : "No case analysis available");
            variables.put("toolResults", limitedToolResults);
            variables.put("patientAgeFromCase", patientAgeFromCase != null ? patientAgeFromCase.toString() : "Not provided");

            String systemPrompt = medgemmaResultInterpretationSystemPromptTemplate.render(Collections.emptyMap());
            String userPrompt = medgemmaResultInterpretationUserPromptTemplate.render(variables);

            int totalPromptLength = systemPrompt.length() + userPrompt.length();
            if (totalPromptLength > 8000) {
                log.warn("Prompt too long ({} chars), truncating user prompt", totalPromptLength);
                int maxUserPromptLength = Math.max(1000, 8000 - systemPrompt.length());
                userPrompt = userPrompt.substring(0, Math.min(userPrompt.length(), maxUserPromptLength))
                        + "\n\n[Prompt truncated - please provide a concise response]";
            }

            log.info("Sending prompt to LLM for result interpretation (model: {}, total prompt length: {})",
                    medGemmaModelName, systemPrompt.length() + userPrompt.length());
            log.debug("System prompt: {}", systemPrompt);
            log.debug("User prompt: {}", userPrompt);
            log.info("Calling LLM model: {} for result interpretation (prompt length: {})",
                    medGemmaModelName, systemPrompt.length() + userPrompt.length());
            logStreamService.sendLog(sessionId, "INFO", "LLM Call",
                    String.format("Calling model: %s for result interpretation", medGemmaModelName));

            String finalSystemPrompt = systemPrompt;
            String finalUserPrompt = userPrompt;
            String interpretation = llmCallLimiter.execute(LlmClientType.CHAT, () -> medGemmaChatClient.prompt()
                    .system(finalSystemPrompt)
                    .user(finalUserPrompt)
                    .call()
                    .content());

            if (interpretation != null && interpretation.length() > 10000) {
                log.warn("LLM response very long ({} chars), checking for repetition", interpretation.length());
                String firstPart = interpretation.substring(0, Math.min(1000, interpretation.length()));
                int occurrences = (interpretation.length() - interpretation.replace(firstPart, "").length()) / firstPart.length();
                if (occurrences > 3) {
                    log.warn("Detected repetitive response pattern, truncating to first occurrence");
                    interpretation = firstPart + "\n\n[Response truncated - repetitive content detected]";
                } else if (interpretation.length() > 15000) {
                    interpretation = interpretation.substring(0, 15000) + "\n\n[Response truncated due to excessive length]";
                }
            }

            log.info("LLM model: {} completed result interpretation, response length: {}",
                    medGemmaModelName, interpretation != null ? interpretation.length() : 0);
            logStreamService.sendLog(sessionId, "INFO", "LLM result interpretation",
                    String.format("Interpretation completed successfully using model: %s, length: %d",
                            medGemmaModelName, interpretation != null ? interpretation.length() : 0));
            log.debug("LLM interpretation result (first 500 chars): {}",
                    interpretation != null && interpretation.length() > 500 ? interpretation.substring(0, 500) + "..." : interpretation);
            return interpretation != null ? interpretation : "Error: Empty response from LLM";
        } catch (Exception e) {
            log.error("Error interpreting results with LLM", e);
            logStreamService.logError(sessionId, "LLM result interpretation failed", e.getMessage());
            throw e;
        }
    }

    @Override
    public String summarizeRoutingResults(String rawToolResults, String caseAnalysis) {
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
            String response = llmCallLimiter.execute(LlmClientType.CHAT,
                    () -> medGemmaChatClient.prompt().user(prompt).call().content());
            return LlmResponseSanitizer.stripLlmReasoning(response);
        } catch (Exception e) {
            log.warn("Routing summarization failed, returning raw results", e);
            return rawToolResults != null ? rawToolResults : "No routing results available.";
        }
    }

    @Override
    public String summarizeNetworkAnalyticsResults(String rawResults) {
        String prompt = """
                Summarize the following network analytics results for the user in 1-2 short paragraphs.
                Focus on key findings: which conditions were analyzed, how many experts, and main metrics.
                Do not include tool names, step numbers, code snippets, or procedural text.
                Write in plain language for a medical or analytics reader.
                Raw results:
                %s
                """.formatted(rawResults);
        try {
            return llmCallLimiter.execute(LlmClientType.CHAT,
                    () -> medGemmaChatClient.prompt().user(prompt).call().content());
        } catch (Exception e) {
            log.warn("Summarization failed, returning raw results", e);
            return rawResults;
        }
    }
}
