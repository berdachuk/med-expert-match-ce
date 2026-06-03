package com.berdachuk.medexpertmatch.llm.chat;

import com.berdachuk.medexpertmatch.core.util.CaseIdExtractor;
import com.berdachuk.medexpertmatch.core.util.LlmCallLimiter;
import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import com.berdachuk.medexpertmatch.core.util.LlmResponseSanitizer;
import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import com.berdachuk.medexpertmatch.llm.event.GoalIdentifiedEvent;
import com.berdachuk.medexpertmatch.llm.harness.CaseContextIntent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Classifies user requests into high-level goals using LLM analysis with keyword fallback.
 * This is the first step in the harness pipeline: identify goal → plan → route to engine.
 */
@Slf4j
@Service
public class GoalClassifier {

    private static final Set<String> FOLLOW_UP_AFFIRMATIVES = Set.of(
            "yes", "yeah", "yep", "ok", "okay", "sure", "go ahead", "please", "proceed", "continue",
            "go on", "tell me more");

    private static final Pattern FOLLOW_UP_PREFIX = Pattern.compile(
            "^\\s*(more|other|another|next|show me more|show more|any other|additional)\\b.*",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern CASE_SWITCH_PATTERN = Pattern.compile(
            "\\b(different|other|another|separate)\\s+case\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern FOLLOW_UP_PHRASING = Pattern.compile(
            "\\b(tell me more|more (?:details|info|information)|provide more|what about|" +
            "how about|elaborate|expand|details? (?:about|on|for)|explain (?:more|further))\\b",
            Pattern.CASE_INSENSITIVE);

    private final ChatClient chatClient;
    private final PromptTemplate goalClassificationTemplate;
    private final ObjectMapper objectMapper;
    private final LlmCallLimiter llmCallLimiter;
    private final ApplicationEventPublisher eventPublisher;

    public GoalClassifier(
            @Qualifier("primaryChatModel") ChatModel primaryChatModel,
            @Qualifier("goalClassificationPromptTemplate") PromptTemplate goalClassificationTemplate,
            ObjectMapper objectMapper,
            LlmCallLimiter llmCallLimiter,
            ApplicationEventPublisher eventPublisher) {
        this.chatClient = ChatClient.builder(primaryChatModel).build();
        this.goalClassificationTemplate = goalClassificationTemplate;
        this.objectMapper = objectMapper;
        this.llmCallLimiter = llmCallLimiter;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Classifies the user's request into a {@link GoalClassification}.
     * Uses lightweight keyword matching first, falls back to LLM classification.
     */
    public GoalClassification classify(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return GoalClassification.general();
        }

        Optional<String> extractedCaseId = CaseIdExtractor.extractFromText(userMessage);

        GoalClassification keywordResult = classifyByKeywords(userMessage, extractedCaseId);
        if (keywordResult != null) {
            publishIfRoutable(keywordResult);
            return keywordResult;
        }

        try {
            GoalClassification llmResult = classifyByLlm(userMessage, extractedCaseId);
            publishIfRoutable(llmResult);
            return llmResult;
        } catch (Exception e) {
            log.warn("LLM goal classification failed, falling back to general: {}", e.getMessage());
            GoalClassification fallback = extractedCaseId.isPresent()
                    ? GoalClassification.matchDoctors(extractedCaseId.get(), "keyword fallback with case ID")
                    : GoalClassification.general();
            publishIfRoutable(fallback);
            return fallback;
        }
    }

    private void publishIfRoutable(GoalClassification goal) {
        if (goal.isRoutableToEngine() && goal.hasCaseId()) {
            String caseId = goal.caseId().orElse("");
            String sessionId = OrchestrationContextHolder.sessionIdOrNull();
            eventPublisher.publishEvent(new GoalIdentifiedEvent(
                    sessionId, goal, caseId, Instant.now()));
            log.debug("Published GoalIdentifiedEvent: session={} goal={} caseId={}",
                    sessionId, goal.goalType(), caseId);
        }
    }

    /**
     * Lightweight keyword-based classification as a fast path.
     * Returns null if the intent is ambiguous, triggering LLM classification.
     */
    GoalClassification classifyByKeywords(String message, Optional<String> caseId) {
        GoalClassification followUp = detectFollowUp(message, caseId);
        if (followUp != null) {
            return followUp;
        }

        String lower = message.toLowerCase();

        if (lower.contains("find specialist") || lower.contains("match doctor")
                || lower.contains("recommend doctor") || lower.contains("expert match")
                || lower.contains("find doctors") || lower.contains("rank doctors")
                || lower.contains("best doctor") || lower.contains("find expert")) {
            clearCurrentContext();
            return caseId.isPresent()
                    ? GoalClassification.matchDoctors(caseId.get(), "keyword: doctor matching with case ID")
                    : GoalClassification.matchDoctors("", "keyword: doctor matching from text");
        }

        if (lower.contains("analyze case") || lower.contains("analyze this case")
                || lower.contains("icd") || lower.contains("diagnosis hint")
                || lower.contains("clinical findings") || lower.contains("case summary")) {
            clearCurrentContext();
            return caseId.map(id -> GoalClassification.analyzeCase(id, "keyword: case analysis"))
                    .orElse(null);
        }

        if (lower.contains("route") || lower.contains("facility")
                || lower.contains("referral") || lower.contains("where to send")) {
            clearCurrentContext();
            return caseId.map(id -> GoalClassification.routeCase(id, "keyword: facility routing"))
                    .orElse(null);
        }

        if (lower.contains("urgency") || lower.contains("triage")
                || lower.contains("intake") || lower.contains("red flag")) {
            clearCurrentContext();
            return GoalClassification.triageIntake("keyword: triage");
        }

        if (lower.contains("pubmed") || lower.contains("evidence")
                || lower.contains("guideline") || lower.contains("literature")) {
            clearCurrentContext();
            return GoalClassification.searchEvidence("keyword: evidence search");
        }

        return null;
    }

    GoalClassification detectFollowUp(String message, Optional<String> caseId) {
        if (CASE_SWITCH_PATTERN.matcher(message).find()) {
            clearCurrentContext();
            return null;
        }
        if (!isFollowUpSignal(message)) {
            return null;
        }
        String sessionId = OrchestrationContextHolder.sessionIdOrNull();
        if (sessionId == null) {
            return null;
        }
        ConversationGoalContext.Entry ctx = ConversationGoalContext.get(sessionId);
        if (ctx == null || ctx.lastGoal() == GoalType.GENERAL_QUESTION) {
            return null;
        }
        if (!sessionId.equals(ctx.sessionId())) {
            return null;
        }
        String inheritedCaseId = caseId.orElse(ctx.lastCaseId() != null ? ctx.lastCaseId() : "");
        return new GoalClassification(
                ctx.lastGoal(),
                inheritedCaseId.isEmpty() ? Optional.empty() : Optional.of(inheritedCaseId),
                Optional.empty(),
                "follow-up: " + ctx.lastGoal().name()
        );
    }

    private boolean isFollowUpSignal(String message) {
        if (message == null) {
            return false;
        }
        if (CASE_SWITCH_PATTERN.matcher(message).find()) {
            return false;
        }
        String trimmed = message.trim().toLowerCase();
        if (FOLLOW_UP_AFFIRMATIVES.contains(trimmed)) {
            return true;
        }
        if (FOLLOW_UP_PREFIX.matcher(trimmed).matches()) {
            return true;
        }
        if (FOLLOW_UP_PHRASING.matcher(trimmed).find()) {
            return true;
        }
        if (trimmed.length() <= 20
                && !trimmed.matches(".*\\b(no|cancel|stop|quit|help|hello|hi|what|why|how|who|when|where)\\b.*")
                && !containsDomainKeyword(trimmed)) {
            return true;
        }
        return false;
    }

    private boolean containsDomainKeyword(String lower) {
        return lower.contains("find") || lower.contains("match") || lower.contains("doctor")
                || lower.contains("case") || lower.contains("analyze") || lower.contains("specialist")
                || lower.contains("facility") || lower.contains("route") || lower.contains("evidence")
                || lower.contains("pubmed") || lower.contains("icd") || lower.contains("triage")
                || lower.contains("urgency");
    }

    private static void clearCurrentContext() {
        String sessionId = OrchestrationContextHolder.sessionIdOrNull();
        if (sessionId != null) {
            ConversationGoalContext.clear(sessionId);
        }
    }

    /**
     * LLM-based goal classification for ambiguous requests.
     */
    GoalClassification classifyByLlm(String message, Optional<String> caseId) {
        String systemPrompt = goalClassificationTemplate.render(Collections.emptyMap());
        String classificationPrompt = "User request:\n" + message;

        log.info("Classifying goal via LLM for message length: {}", message.length());

        String response = llmCallLimiter.execute(LlmClientType.CHAT, () ->
                chatClient.prompt()
                        .system(systemPrompt)
                        .user(classificationPrompt)
                        .call()
                        .content());

        return parseClassification(response, caseId);
    }

    /**
     * Maps a high-level goal type to the appropriate {@link CaseContextIntent}
     * for building context bundles with the right fields per goal.
     */
    public static CaseContextIntent toContextIntent(GoalType goalType) {
        return switch (goalType) {
            case MATCH_DOCTORS -> CaseContextIntent.MATCH;
            case ANALYZE_CASE -> CaseContextIntent.ANALYZE;
            case ROUTE_CASE -> CaseContextIntent.ROUTE;
            case TRIAGE_INTAKE -> CaseContextIntent.MATCH;
            case SEARCH_EVIDENCE -> CaseContextIntent.EVIDENCE;
            default -> CaseContextIntent.CHAT_AUTO;
        };
    }

    /**
     * Parses LLM JSON response into GoalClassification.
     */
    GoalClassification parseClassification(String response, Optional<String> caseId) {
        if (response == null || response.isBlank()) {
            return GoalClassification.general();
        }

        try {
            String json = LlmResponseSanitizer.extractJson(response);
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
            String goalType = (String) parsed.get("goalType");
            String summary = (String) parsed.getOrDefault("summary", "");

            if (goalType == null) {
                return GoalClassification.general();
            }

            switch (goalType.toUpperCase()) {
                case "MATCH_DOCTORS":
                    return caseId.isPresent()
                            ? GoalClassification.matchDoctors(caseId.get(), summary)
                            : GoalClassification.matchDoctors("", summary);
                case "ANALYZE_CASE":
                    return caseId.map(id -> GoalClassification.analyzeCase(id, summary))
                            .orElse(GoalClassification.general());
                case "ROUTE_CASE":
                    return caseId.map(id -> GoalClassification.routeCase(id, summary))
                            .orElse(GoalClassification.general());
                case "TRIAGE_INTAKE":
                    return GoalClassification.triageIntake(summary);
                case "SEARCH_EVIDENCE":
                    return GoalClassification.searchEvidence(summary);
                case "GENERATE_RECOMMENDATIONS":
                    return GoalClassification.generateRecommendations("", summary);
                case "GENERAL_QUESTION":
                default:
                    return GoalClassification.general();
            }
        } catch (Exception e) {
            log.warn("Failed to parse goal classification response: {}", e.getMessage());
            return GoalClassification.general();
        }
    }
}
