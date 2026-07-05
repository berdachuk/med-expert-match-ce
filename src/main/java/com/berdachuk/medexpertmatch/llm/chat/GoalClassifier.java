package com.berdachuk.medexpertmatch.llm.chat;

import com.berdachuk.medexpertmatch.core.util.CaseIdExtractor;
import com.berdachuk.medexpertmatch.core.util.LenientJsonOutputConverter;
import com.berdachuk.medexpertmatch.core.util.LlmCallLimiter;
import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import com.berdachuk.medexpertmatch.core.util.LlmOperation;
import com.berdachuk.medexpertmatch.core.util.LlmResponseSanitizer;
import com.berdachuk.medexpertmatch.core.util.LlmUsageContext;
import com.berdachuk.medexpertmatch.core.util.LlmUsageContextRunner;
import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import com.berdachuk.medexpertmatch.llm.event.GoalIdentifiedEvent;
import com.berdachuk.medexpertmatch.llm.harness.CaseContextIntent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
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
 * Classifies user requests into high-level goals using session rules, keywords, and LLM fallback.
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

    private static final Pattern MATCH_MORE_DOCTORS = Pattern.compile(
            "(?:^|\\b)(?:find|show|get|list|any)\\s+(?:other|more|another|additional)\\s+"
                    + "(?:doctor|doctors|specialist|specialists)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SHOW_ALL_MATCHES = Pattern.compile(
            "(?:^|\\b)(?:show|list|display|see|view)\\s+(?:me\\s+)?(?:all|every(?:one)?|full\\s+list|everything)"
                    + "(?:\\s+(?:doctor|doctors|specialist|specialists|match(?:es)?|result(?:s)?|candidates?))?"
                    + "\\b|^all\\s+(?:doctor|doctors|specialist|specialists|match(?:es)?|results?)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern RUSSIAN_MORE_DOCTORS = Pattern.compile(
            "(?:найди|покажи|дай|подбери).{0,24}(?:ещё|еще).{0,24}(?:доктор|врач|специалист)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private final ChatClient chatClient;
    private final PromptTemplate goalClassificationTemplate;
    private final PromptTemplate goalClassificationUserTemplate;
    private final ObjectMapper objectMapper;
    private final LlmCallLimiter llmCallLimiter;
    private final ApplicationEventPublisher eventPublisher;

    public GoalClassifier(
            @Qualifier("utilityChatClient") ChatClient utilityChatClient,
            @Qualifier("goalClassificationPromptTemplate") PromptTemplate goalClassificationTemplate,
            @Qualifier("goalClassificationUserPromptTemplate") PromptTemplate goalClassificationUserTemplate,
            ObjectMapper objectMapper,
            LlmCallLimiter llmCallLimiter,
            ApplicationEventPublisher eventPublisher) {
        this.chatClient = utilityChatClient;
        this.goalClassificationTemplate = goalClassificationTemplate;
        this.goalClassificationUserTemplate = goalClassificationUserTemplate;
        this.objectMapper = objectMapper;
        this.llmCallLimiter = llmCallLimiter;
        this.eventPublisher = eventPublisher;
    }

    public static boolean requestsMoreDoctors(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String trimmed = message.trim();
        return MATCH_MORE_DOCTORS.matcher(trimmed).find()
                || RUSSIAN_MORE_DOCTORS.matcher(trimmed).find();
    }

    public static boolean requestsShowAllMatches(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        return SHOW_ALL_MATCHES.matcher(message.trim()).find();
    }

    public GoalClassification classify(String userMessage) {
        return classify(userMessage, GoalClassificationContext.empty());
    }

    public GoalClassification classify(String userMessage, GoalClassificationContext context) {
        if (userMessage == null || userMessage.isBlank()) {
            return GoalClassification.general();
        }

        Optional<String> extractedCaseId = CaseIdExtractor.extractFromText(userMessage);

        GoalClassification result = detectSessionContinuation(userMessage, extractedCaseId);
        if (result == null) {
            result = classifyByKeywords(userMessage, extractedCaseId);
        }
        if (result == null) {
            try {
                result = classifyByLlm(userMessage, extractedCaseId, context);
            } catch (Exception e) {
                log.warn("LLM goal classification failed, falling back to general: {}", e.getMessage());
                result = extractedCaseId.isPresent()
                        ? GoalClassification.matchDoctors(extractedCaseId.get(), "keyword fallback with case ID")
                        : GoalClassification.general();
            }
        }
        result = inheritSessionCaseId(result, userMessage);
        result = applySessionOverrides(result, userMessage);
        publishIfRoutable(result);
        return result;
    }

    GoalClassification detectSessionContinuation(String message, Optional<String> caseId) {
        if (CASE_SWITCH_PATTERN.matcher(message).find()) {
            clearCurrentContext();
            return null;
        }

        ConversationGoalContext.Entry ctx = currentSessionEntry();
        if (ctx == null || ctx.lastCaseId() == null || ctx.lastCaseId().isBlank()) {
            return null;
        }

        String inheritedCaseId = caseId.filter(id -> !id.isBlank())
                .orElse(ctx.lastCaseId());

        if (requestsMoreDoctors(message)) {
            return GoalClassification.matchDoctors(inheritedCaseId,
                    "session: more doctors with inherited case");
        }

        if (GoalIntentPatterns.looksLikeCaseDetailRequest(message)) {
            return GoalClassification.analyzeCase(inheritedCaseId,
                    "session: case detail with inherited case");
        }

        if (GoalIntentPatterns.matchesRouteCaseKeywords(message)) {
            return GoalClassification.routeCase(inheritedCaseId,
                    "session: route case with inherited case");
        }

        if (GoalIntentPatterns.looksLikeElaborationFollowUp(message)) {
            return GoalClassification.analyzeCase(inheritedCaseId,
                    "session: elaboration follow-up with inherited case");
        }

        return detectFollowUp(message, caseId);
    }

    GoalClassification inheritSessionCaseId(GoalClassification goal, String message) {
        if (goal.hasCaseId()) {
            return goal;
        }
        ConversationGoalContext.Entry ctx = currentSessionEntry();
        if (ctx == null || ctx.lastCaseId() == null || ctx.lastCaseId().isBlank()) {
            return goal;
        }

        return switch (goal.goalType()) {
            case ANALYZE_CASE, SEARCH_EVIDENCE, GENERATE_RECOMMENDATIONS, ROUTE_CASE -> withInheritedCase(goal, ctx);
            case MATCH_DOCTORS -> shouldInheritRoutableGoal(goal, ctx, message)
                    ? withInheritedCase(goal, ctx)
                    : goal;
            default -> goal;
        };
    }

    GoalClassification applySessionOverrides(GoalClassification goal, String message) {
        if (goal.goalType() != GoalType.GENERAL_QUESTION) {
            return goal;
        }
        ConversationGoalContext.Entry ctx = currentSessionEntry();
        if (ctx == null || ctx.lastCaseId() == null || ctx.lastCaseId().isBlank()) {
            return goal;
        }

        if (GoalIntentPatterns.looksLikeCaseDetailRequest(message)) {
            log.info("Goal override: GENERAL_QUESTION → ANALYZE_CASE (session case present)");
            return GoalClassification.analyzeCase(ctx.lastCaseId(),
                    "override: case detail with session case");
        }
        if (requestsMoreDoctors(message)) {
            log.info("Goal override: GENERAL_QUESTION → MATCH_DOCTORS (session case present)");
            return GoalClassification.matchDoctors(ctx.lastCaseId(),
                    "override: more doctors with session case");
        }
        if (GoalIntentPatterns.looksLikeCaseContinuation(message)) {
            log.info("Goal override: GENERAL_QUESTION → {} (session continuation)",
                    ctx.lastGoal());
            return new GoalClassification(
                    ctx.lastGoal(),
                    Optional.of(ctx.lastCaseId()),
                    Optional.empty(),
                    "override: session continuation");
        }
        return goal;
    }

    private boolean shouldInheritRoutableGoal(
            GoalClassification goal, ConversationGoalContext.Entry ctx, String message) {
        if (ctx.lastGoal() == goal.goalType()) {
            return true;
        }
        if (goal.summary() != null && goal.summary().startsWith("follow-up:")) {
            return true;
        }
        return requestsMoreDoctors(message);
    }

    private GoalClassification withInheritedCase(GoalClassification goal, ConversationGoalContext.Entry ctx) {
        return new GoalClassification(
                goal.goalType(),
                Optional.of(ctx.lastCaseId()),
                goal.matchId(),
                goal.summary() + " (inherited case ID from session)");
    }

    private ConversationGoalContext.Entry currentSessionEntry() {
        String sessionId = OrchestrationContextHolder.sessionIdOrNull();
        if (sessionId == null) {
            return null;
        }
        return ConversationGoalContext.get(sessionId);
    }

    private void publishIfRoutable(GoalClassification goal) {
        if (!goal.isRoutableToEngine() || !goal.hasCaseId()) {
            return;
        }
        String caseId = goal.caseId().orElse("");
        String sessionId = OrchestrationContextHolder.sessionIdOrNull();
        eventPublisher.publishEvent(new GoalIdentifiedEvent(
                sessionId, goal, caseId, Instant.now()));
        log.debug("Published GoalIdentifiedEvent: session={} goal={} caseId={}",
                sessionId, goal.goalType(), caseId);
    }

    GoalClassification classifyByKeywords(String message, Optional<String> caseId) {
        GoalClassification followUp = detectFollowUp(message, caseId);
        if (followUp != null) {
            return followUp;
        }

        if (GoalIntentPatterns.matchesMatchDoctorsKeywords(message)) {
            clearCurrentContext();
            return caseId.isPresent()
                    ? GoalClassification.matchDoctors(caseId.get(), "keyword: doctor matching with case ID")
                    : GoalClassification.matchDoctors("", "keyword: doctor matching from text");
        }

        if (GoalIntentPatterns.matchesAnalyzeCaseKeywords(message)) {
            return caseId.map(id -> GoalClassification.analyzeCase(id, "keyword: case analysis"))
                    .orElse(GoalClassification.analyzeCase("", "keyword: case analysis from text"));
        }

        if (GoalIntentPatterns.matchesRouteCaseKeywords(message)) {
            return caseId.map(id -> GoalClassification.routeCase(id, "keyword: facility routing"))
                    .orElseGet(() -> GoalClassification.routeCase("", "keyword: facility routing from text"));
        }

        if (GoalIntentPatterns.matchesTriageKeywords(message)) {
            clearCurrentContext();
            return GoalClassification.triageIntake("keyword: triage");
        }

        if (GoalIntentPatterns.matchesEvidenceKeywords(message)) {
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
        String inheritedCaseId = caseId.filter(id -> !id.isBlank())
                .orElse(ctx.lastCaseId() != null ? ctx.lastCaseId() : "");
        GoalType followUpGoal = resolveFollowUpGoal(message, ctx.lastGoal());
        return new GoalClassification(
                followUpGoal,
                inheritedCaseId.isBlank() ? Optional.empty() : Optional.of(inheritedCaseId),
                Optional.empty(),
                "follow-up: " + followUpGoal.name()
        );
    }

    private static final Set<String> SAME_GOAL_AFFIRMATIVES = Set.of(
            "yes", "yeah", "yep", "ok", "okay", "sure");

    private GoalType resolveFollowUpGoal(String message, GoalType lastGoal) {
        if (requestsMoreDoctors(message) || requestsShowAllMatches(message)) {
            return GoalType.MATCH_DOCTORS;
        }
        if (lastGoal != GoalType.MATCH_DOCTORS && lastGoal != GoalType.ROUTE_CASE) {
            return lastGoal;
        }
        if (GoalIntentPatterns.looksLikeElaborationFollowUp(message)) {
            return GoalType.ANALYZE_CASE;
        }
        String lower = message.trim().toLowerCase();
        if (FOLLOW_UP_AFFIRMATIVES.contains(lower) && !SAME_GOAL_AFFIRMATIVES.contains(lower)) {
            return GoalType.ANALYZE_CASE;
        }
        return lastGoal;
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
        if (requestsMoreDoctors(trimmed)) {
            return true;
        }
        if (requestsShowAllMatches(trimmed)) {
            return true;
        }
        if (FOLLOW_UP_PHRASING.matcher(trimmed).find()) {
            return true;
        }
        if (GoalIntentPatterns.matchesRussianFollowUp(trimmed)) {
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
                || lower.contains("urgency") || lower.contains("clinical") || lower.contains("patient");
    }

    private static void clearCurrentContext() {
        String sessionId = OrchestrationContextHolder.sessionIdOrNull();
        if (sessionId != null) {
            ConversationGoalContext.clear(sessionId);
        }
    }

    GoalClassification classifyByLlm(
            String message, Optional<String> caseId, GoalClassificationContext context) {
        String systemPrompt = goalClassificationTemplate.render(Collections.emptyMap());
        ConversationGoalContext.Entry ctx = currentSessionEntry();
        String lastGoal = ctx != null ? ctx.lastGoal().name() : "none";
        String lastCaseId = ctx != null && ctx.lastCaseId() != null ? ctx.lastCaseId() : "none";
        String classificationPrompt = goalClassificationUserTemplate.render(Map.of(
                "userMessage", message,
                "lastGoal", lastGoal,
                "lastCaseId", lastCaseId,
                "recentHistory", context.recentHistoryOrNone()));

        log.info("Classifying goal via LLM for message length: {}", message.length());

        String response = LlmUsageContextRunner.execute(
                new LlmUsageContext(OrchestrationContextHolder.sessionIdOrNull(),
                        LlmClientType.UTILITY, LlmOperation.GOAL_CLASSIFY, null, null, null),
                () -> llmCallLimiter.execute(LlmClientType.UTILITY, () ->
                        chatClient.prompt()
                                .system(systemPrompt)
                                .user(classificationPrompt)
                                .call()
                                .content()));

        if (response == null || response.isBlank()) {
            return GoalClassification.general();
        }

        try {
            var converter = new LenientJsonOutputConverter<>(GoalClassificationJson.class);
            GoalClassificationJson parsed = converter.convert(response);
            return parseClassification(parsed, caseId, ctx);
        } catch (Exception e) {
            log.warn("Failed to parse goal classification response: {}", e.getMessage());
            return GoalClassification.general();
        }
    }

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

    GoalClassification parseClassification(
            GoalClassificationJson parsed, Optional<String> caseId, ConversationGoalContext.Entry sessionCtx) {
        if (parsed == null || parsed.g() == null) {
            return GoalClassification.general();
        }

        String goalType = parsed.g();
        String summary = parsed.s() != null ? parsed.s() : "";
        boolean useSessionCase = Boolean.TRUE.equals(parsed.u());

        Optional<String> effectiveCaseId = caseId.filter(id -> !id.isBlank());
        if (effectiveCaseId.isEmpty() && useSessionCase && sessionCtx != null
                && sessionCtx.lastCaseId() != null && !sessionCtx.lastCaseId().isBlank()) {
            effectiveCaseId = Optional.of(sessionCtx.lastCaseId());
        }

        return switch (goalType.toUpperCase()) {
            case "MATCH_DOCTORS" -> effectiveCaseId
                    .map(id -> GoalClassification.matchDoctors(id, summary))
                    .orElseGet(() -> GoalClassification.matchDoctors("", summary));
            case "ANALYZE_CASE" -> effectiveCaseId
                    .map(id -> GoalClassification.analyzeCase(id, summary))
                    .orElseGet(() -> GoalClassification.analyzeCase("", summary));
            case "ROUTE_CASE" -> effectiveCaseId
                    .map(id -> GoalClassification.routeCase(id, summary))
                    .orElse(GoalClassification.general());
            case "TRIAGE_INTAKE" -> GoalClassification.triageIntake(summary);
            case "SEARCH_EVIDENCE" -> GoalClassification.searchEvidence(summary);
            case "GENERATE_RECOMMENDATIONS" -> GoalClassification.generateRecommendations("", summary);
            case "GENERAL_QUESTION" -> GoalClassification.general();
            default -> GoalClassification.general();
        };
    }

    GoalClassification parseClassification(
            String response, Optional<String> caseId, ConversationGoalContext.Entry sessionCtx) {
        if (response == null || response.isBlank()) {
            return GoalClassification.general();
        }

        try {
            String json = LlmResponseSanitizer.extractJson(response);
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
            String goalType = (String) parsed.getOrDefault("g", parsed.get("goalType"));
            String summary = (String) parsed.getOrDefault("s", parsed.getOrDefault("summary", ""));
            boolean useSessionCase = Boolean.TRUE.equals(parsed.getOrDefault("u", parsed.get("useSessionCase")));

            if (goalType == null) {
                return GoalClassification.general();
            }

            Optional<String> effectiveCaseId = caseId.filter(id -> !id.isBlank());
            if (effectiveCaseId.isEmpty() && useSessionCase && sessionCtx != null
                    && sessionCtx.lastCaseId() != null && !sessionCtx.lastCaseId().isBlank()) {
                effectiveCaseId = Optional.of(sessionCtx.lastCaseId());
            }

            return switch (goalType.toUpperCase()) {
                case "MATCH_DOCTORS" -> effectiveCaseId
                        .map(id -> GoalClassification.matchDoctors(id, summary))
                        .orElseGet(() -> GoalClassification.matchDoctors("", summary));
                case "ANALYZE_CASE" -> effectiveCaseId
                        .map(id -> GoalClassification.analyzeCase(id, summary))
                        .orElseGet(() -> GoalClassification.analyzeCase("", summary));
                case "ROUTE_CASE" -> effectiveCaseId
                        .map(id -> GoalClassification.routeCase(id, summary))
                        .orElse(GoalClassification.general());
                case "TRIAGE_INTAKE" -> GoalClassification.triageIntake(summary);
                case "SEARCH_EVIDENCE" -> GoalClassification.searchEvidence(summary);
                case "GENERATE_RECOMMENDATIONS" -> GoalClassification.generateRecommendations("", summary);
                case "GENERAL_QUESTION" -> GoalClassification.general();
                default -> GoalClassification.general();
            };
        } catch (Exception e) {
            log.warn("Failed to parse goal classification response: {}", e.getMessage());
            return GoalClassification.general();
        }
    }

    GoalClassification parseClassification(String response, Optional<String> caseId) {
        return parseClassification(response, caseId, currentSessionEntry());
    }
}
