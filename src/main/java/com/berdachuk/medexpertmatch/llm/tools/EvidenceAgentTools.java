package com.berdachuk.medexpertmatch.llm.tools;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.evidence.domain.PubMedArticle;
import com.berdachuk.medexpertmatch.evidence.service.PubMedService;
import com.berdachuk.medexpertmatch.llm.tools.support.AgentToolSessionSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Spring AI tools for clinical evidence retrieval (guidelines and PubMed).
 */
@Slf4j
@Component
public class EvidenceAgentTools {

    private final PubMedService pubmedService;
    private final ChatClient medGemmaChatClient;
    private final LogStreamService logStreamService;
    private final PromptTemplate clinicalGuidelinesSearchPromptTemplate;

    public EvidenceAgentTools(
            PubMedService pubmedService,
            @Qualifier("caseAnalysisChatClient") ChatClient medGemmaChatClient,
            LogStreamService logStreamService,
            @Qualifier("clinicalGuidelinesSearchPromptTemplate") PromptTemplate clinicalGuidelinesSearchPromptTemplate) {
        this.pubmedService = pubmedService;
        this.medGemmaChatClient = medGemmaChatClient;
        this.logStreamService = logStreamService;
        this.clinicalGuidelinesSearchPromptTemplate = clinicalGuidelinesSearchPromptTemplate;
    }

    @Tool(description = "Search clinical practice guidelines for a medical condition. Returns relevant guidelines with citations.")
    public List<String> search_clinical_guidelines(
            @ToolParam(description = "Medical condition or diagnosis") String condition,
            @ToolParam(description = "Medical specialty (optional)") String specialty,
            @ToolParam(description = "Maximum number of results (default: 10)") Integer maxResults
    ) {
        log.info("search_clinical_guidelines() tool called - condition: {}, specialty: {}, maxResults: {}",
                condition, specialty, maxResults);
        String sessionId = AgentToolSessionSupport.getSessionId();
        logStreamService.logToolCall(sessionId, "search_clinical_guidelines",
                String.format("condition: %s, specialty: %s, maxResults: %s", condition, specialty, maxResults));

        try {
            if (condition == null || condition.trim().isEmpty()) {
                log.warn("Condition is required for search_clinical_guidelines");
                logStreamService.logError(sessionId, "search_clinical_guidelines failed", "Condition is required");
                return List.of("Error: Condition is required");
            }

            int limit = (maxResults != null && maxResults > 0) ? maxResults : 10;

            String specialtyClause = specialty != null && !specialty.trim().isEmpty()
                    ? " in the specialty of " + specialty.trim()
                    : "";
            String prompt = clinicalGuidelinesSearchPromptTemplate.render(Map.of(
                    "condition", condition,
                    "specialtyClause", specialtyClause,
                    "limit", String.valueOf(limit)));

            log.info("Sending prompt to LLM for clinical guidelines (model: LLM, condition: {}, specialty: {}):\n{}",
                    condition, specialty, prompt);
            log.info("Calling LLM for clinical guidelines - condition: {}, specialty: {}", condition, specialty);
            String responseText = medGemmaChatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (responseText == null || responseText.trim().isEmpty()) {
                log.warn("Empty response from LLM for clinical guidelines");
                logStreamService.logToolResult(sessionId, "search_clinical_guidelines",
                        "No guidelines generated");
                return List.of("No clinical guidelines could be generated for condition: " + condition);
            }

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
                    if (currentGuideline.length() > 0) {
                        guidelines.add(currentGuideline.toString().trim());
                    }
                    currentGuideline = new StringBuilder(line);
                } else {
                    if (currentGuideline.length() > 0) {
                        currentGuideline.append(" ").append(line);
                    } else {
                        currentGuideline.append(line);
                    }
                }
            }

            if (currentGuideline.length() > 0) {
                guidelines.add(currentGuideline.toString().trim());
            }

            if (guidelines.size() > limit) {
                guidelines = guidelines.subList(0, limit);
            }

            if (guidelines.isEmpty()) {
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
        String sessionId = AgentToolSessionSupport.getSessionId();
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
}
