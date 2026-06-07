package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.chat.domain.Chat;
import com.berdachuk.medexpertmatch.chat.domain.ChatMessage;
import com.berdachuk.medexpertmatch.chat.service.ChatAssistantService;
import com.berdachuk.medexpertmatch.chat.service.ChatTurnMetrics;
import com.berdachuk.medexpertmatch.chat.service.ChatService;
import com.berdachuk.medexpertmatch.core.domain.RateLimitTier;
import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.llm.chat.ChatCasePromptSupport;
import com.berdachuk.medexpertmatch.llm.chat.ChatLanguageService;
import com.berdachuk.medexpertmatch.llm.chat.ChatLanguageTurn;
import com.berdachuk.medexpertmatch.llm.chat.ChatUserContentSanitizer;
import com.berdachuk.medexpertmatch.llm.chat.GoalClassificationContext;
import com.berdachuk.medexpertmatch.core.util.LlmCallLimiter;
import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import com.berdachuk.medexpertmatch.llm.agent.SessionAdvisorSupport;
import com.berdachuk.medexpertmatch.llm.chat.ChatAgentProfile;
import com.berdachuk.medexpertmatch.llm.chat.ChatToolContextHolder;
import com.berdachuk.medexpertmatch.llm.chat.ConversationGoalContext;
import com.berdachuk.medexpertmatch.llm.chat.GoalClassification;
import com.berdachuk.medexpertmatch.llm.chat.GoalClassifier;
import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import com.berdachuk.medexpertmatch.llm.config.HarnessProperties;
import com.berdachuk.medexpertmatch.core.config.LlmTierProperties;
import com.berdachuk.medexpertmatch.core.util.LlmResponseSanitizer;
import com.berdachuk.medexpertmatch.llm.harness.MedicalAgentPolicyGateService;
import com.berdachuk.medexpertmatch.llm.monitoring.LlmRoutingMetrics;
import com.berdachuk.medexpertmatch.llm.routing.RoutingTier;
import com.berdachuk.medexpertmatch.llm.routing.RoutingTierResolver;
import com.berdachuk.medexpertmatch.llm.service.ChatStreamActivityPublisher;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentPromptSupportService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.llm.service.PipelineProgressCollector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class ChatAssistantServiceImpl implements ChatAssistantService {

    private final ChatService chatService;
    private final ChatClient chatClient;
    private final MedicalAgentPromptSupportService promptSupportService;
    private final LogStreamService logStreamService;
    private final ChatStreamActivityPublisher chatStreamActivityPublisher;
    private final LlmCallLimiter llmCallLimiter;
    private final ChatTurnMetrics chatTurnMetrics;
    private final PromptTemplate chatAgentSystemTemplate;
    private final PromptTemplate chatAgentOrchestratorInstructionsTemplate;
    private final PromptTemplate chatUserMessageTemplate;
    private final ChatCasePromptSupport chatCasePromptSupport;
    private final MedicalAgentPolicyGateService medicalAgentPolicyGateService;
    private final HarnessProperties harnessProperties;
    private final String functionGemmaModelName;
    private final GoalClassifier goalClassifier;
    private final ChatLanguageService chatLanguageService;
    private final MedicalAgentService medicalAgentService;
    private final SessionService sessionService;
    private final PipelineProgressCollector pipelineProgressCollector;
    private final LlmRoutingMetrics llmRoutingMetrics;
    private final LlmTierProperties llmTierProperties;

    public ChatAssistantServiceImpl(
            ChatService chatService,
            @Qualifier("medicalAgentChatClient") ChatClient chatClient,
            MedicalAgentPromptSupportService promptSupportService,
            LogStreamService logStreamService,
            ChatStreamActivityPublisher chatStreamActivityPublisher,
            LlmCallLimiter llmCallLimiter,
            ChatTurnMetrics chatTurnMetrics,
            @Qualifier("chatAgentSystemPromptTemplate") PromptTemplate chatAgentSystemTemplate,
            @Qualifier("chatAgentOrchestratorInstructionsPromptTemplate") PromptTemplate chatAgentOrchestratorInstructionsTemplate,
            @Qualifier("chatUserMessagePromptTemplate") PromptTemplate chatUserMessageTemplate,
            ChatCasePromptSupport chatCasePromptSupport,
            MedicalAgentPolicyGateService medicalAgentPolicyGateService,
            HarnessProperties harnessProperties,
            @Value("${spring.ai.custom.tool-calling.model:functiongemma}") String functionGemmaModelName,
            GoalClassifier goalClassifier,
            ChatLanguageService chatLanguageService,
            MedicalAgentService medicalAgentService,
            SessionService sessionService,
            PipelineProgressCollector pipelineProgressCollector,
            LlmRoutingMetrics llmRoutingMetrics,
            LlmTierProperties llmTierProperties) {
        this.chatService = chatService;
        this.chatClient = chatClient;
        this.promptSupportService = promptSupportService;
        this.logStreamService = logStreamService;
        this.chatStreamActivityPublisher = chatStreamActivityPublisher;
        this.llmCallLimiter = llmCallLimiter;
        this.chatTurnMetrics = chatTurnMetrics;
        this.chatAgentSystemTemplate = chatAgentSystemTemplate;
        this.chatAgentOrchestratorInstructionsTemplate = chatAgentOrchestratorInstructionsTemplate;
        this.chatUserMessageTemplate = chatUserMessageTemplate;
        this.chatCasePromptSupport = chatCasePromptSupport;
        this.medicalAgentPolicyGateService = medicalAgentPolicyGateService;
        this.harnessProperties = harnessProperties;
        this.functionGemmaModelName = functionGemmaModelName;
        this.goalClassifier = goalClassifier;
        this.chatLanguageService = chatLanguageService;
        this.medicalAgentService = medicalAgentService;
        this.sessionService = sessionService;
        this.pipelineProgressCollector = pipelineProgressCollector;
        this.llmRoutingMetrics = llmRoutingMetrics;
        this.llmTierProperties = llmTierProperties;
    }

    @Override
    public Map<String, ChatMessage> processMessage(String chatId, String userId, String content, String agentIdOverride) {
        String sessionId = userId + "-" + chatId;
        OrchestrationContextHolder.setSessionId(sessionId);
        try {
            String normalizedContent = ChatUserContentSanitizer.sanitize(content);
            ChatLanguageTurn languageTurn = chatLanguageService.prepareTurn(normalizedContent);
            GoalClassification goal = classifyWithContext(chatId, userId, languageTurn.processingText());
            recordRoutingDecision(goal);

            if (goal.isRoutableToEngine() && goal.hasCaseId()) {
                return processViaHarnessEngine(chatId, userId, languageTurn, goal);
            }
            if (harnessProperties.analyzeCaseHarnessEnabled() && goal.isAnalyzableViaHarness()) {
                return processViaCaseAnalysisEngine(chatId, userId, languageTurn, goal);
            }

            TurnContext ctx = prepareTurn(chatId, userId, languageTurn, agentIdOverride, goal);
            try {
                String reply = localizeReply(languageTurn, invokeSync(ctx));
                ChatMessage assistantMessage = chatService.appendAssistantMessage(chatId, userId, reply);
                try {
                    sessionService.appendMessage(sessionId, new AssistantMessage(reply));
                } catch (Exception ignored) {
                }
                ConversationGoalContext.set(goal.goalType(),
                        goal.caseId().orElse(null), sessionId);
                return Map.of("userMessage", ctx.userMessage(), "assistantMessage", assistantMessage);
            } catch (Exception e) {
                log.warn("Chat LLM turn failed for chat {}: {}", chatId, e.getMessage());
                ChatMessage assistantMessage = chatService.appendAssistantMessage(
                        chatId, userId, "Sorry, the assistant encountered an error. Please try again.");
                return Map.of("userMessage", ctx.userMessage(), "assistantMessage", assistantMessage);
            } finally {
                clearTurnContext(sessionId);
            }
        } finally {
            OrchestrationContextHolder.clear();
        }
    }

    @Override
    public SseEmitter streamMessage(String chatId, String userId, String content, String agentIdOverride,
                                      RateLimitTier tier) {
        String normalizedContent = ChatUserContentSanitizer.sanitize(content);
        ChatLanguageTurn languageTurn = chatLanguageService.prepareTurn(normalizedContent);
        String sessionId = userId + "-" + chatId;
        OrchestrationContextHolder.setSessionId(sessionId);
        GoalClassification goal;
        try {
            goal = classifyWithContext(chatId, userId, languageTurn.processingText());
        } finally {
            OrchestrationContextHolder.clear();
        }
        recordRoutingDecision(goal);

        if (goal.isRoutableToEngine() && goal.hasCaseId()) {
            return streamViaHarnessEngine(chatId, userId, languageTurn, goal, tier);
        }
        if (harnessProperties.analyzeCaseHarnessEnabled() && goal.isAnalyzableViaHarness()) {
            return streamViaCaseAnalysisEngine(chatId, userId, languageTurn, goal, tier);
        }

        TurnContext ctx = prepareTurn(chatId, userId, languageTurn, agentIdOverride, goal);
        SseEmitter emitter = new SseEmitter(120_000L);
        RateLimitTier metricsTier = tier != null ? tier : RateLimitTier.DEFAULT;
        CompletableFuture.runAsync(() -> {
            StringBuilder full = new StringBuilder();
            Timer.Sample turnSample = chatTurnMetrics.startTurn(metricsTier);
            chatStreamActivityPublisher.register(ctx.sessionId(), emitter);
            try {
                sendAgentEvent(emitter, Map.of(
                        "type", "agent_start",
                        "agentId", ctx.profile().agentId(),
                        "orchestrator", ctx.profile().orchestrator()));
                chatStreamActivityPublisher.publishReasoning(ctx.sessionId(), "Planning response…");
                logStreamService.sendLog(ctx.sessionId(), "INFO", "Chat Agent",
                        "Streaming turn for agent " + ctx.profile().agentId());

                Flux<String> tokenFlux = llmCallLimiter.execute(LlmClientType.TOOL_CALLING, () -> chatClient.prompt()
                        .system(ctx.systemPrompt())
                        .user(ctx.userPrompt())
                        .advisors(a -> SessionAdvisorSupport.applyOrchestratorContext(a, ctx.sessionId()))
                        .stream()
                        .content());

                tokenFlux.doOnNext(chunk -> {
                            full.append(chunk);
                            try {
                                emitter.send(SseEmitter.event().name("token").data(Map.of("t", chunk)));
                            } catch (IOException e) {
                                throw new IllegalStateException(e);
                            }
                        })
                        .doOnComplete(() -> {
                            try {
                                String reply = localizeReply(ctx.languageTurn(),
                                        resolveReplyAfterStream(ctx, full.toString()));
                                streamReplyTokens(emitter, full.toString(), reply);
                                ChatMessage assistant = chatService.appendAssistantMessage(chatId, userId, reply);
                                try {
                                    sessionService.appendMessage(ctx.sessionId(), new AssistantMessage(reply));
                                } catch (Exception ignored) {
                                }
                                ConversationGoalContext.set(ctx.goal().goalType(),
                                        ctx.goal().caseId().orElse(null), ctx.sessionId());
                                sendAgentEvent(emitter, Map.of("type", "agent_done", "agentId", ctx.profile().agentId()));
                                sendPipelineStageEvents(emitter, ctx.sessionId());
                                emitter.send(SseEmitter.event().name("done").data(Map.of(
                                        "id", assistant.id(),
                                        "content", reply)));
                                chatTurnMetrics.recordTurnSuccess(turnSample, metricsTier);
                                emitter.complete();
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            } finally {
                                clearTurnContext(ctx.sessionId());
                                chatStreamActivityPublisher.unregister(ctx.sessionId());
                            }
                        })
                        .doOnError(error -> {
                            log.warn("Chat stream failed for chat {}: {}", chatId, error.getMessage());
                            chatTurnMetrics.recordStreamError();
                            try {
                                chatService.appendAssistantMessage(chatId, userId,
                                        "Sorry, the assistant encountered an error. Please try again.");
                            } catch (Exception ignored) {
                                // best effort
                            }
                            emitter.completeWithError(error);
                            clearTurnContext(ctx.sessionId());
                            chatStreamActivityPublisher.unregister(ctx.sessionId());
                        })
                        .subscribe();
            } catch (Exception e) {
                log.warn("Chat stream setup failed for chat {}: {}", chatId, e.getMessage());
                emitter.completeWithError(e);
                clearTurnContext(ctx.sessionId());
                chatStreamActivityPublisher.unregister(ctx.sessionId());
            }
        });
        return emitter;
    }

    private GoalClassification classifyWithContext(String chatId, String userId, String processingText) {
        return goalClassifier.classify(processingText, buildClassificationContext(chatId, userId));
    }

    private void recordRoutingDecision(GoalClassification goal) {
        RoutingTier tier = RoutingTierResolver.fromClassification(goal);
        llmRoutingMetrics.recordRoutingDecision(tier, goal.goalType());
        log.info("Goal classified: {} (caseId={}, routingTier={}, maxTokens={})",
                goal.goalType(), goal.caseId().orElse("none"), tier,
                RoutingTierResolver.maxTokensFor(tier, llmTierProperties));
    }

    private void recordHarnessRoute(GoalClassification goal) {
        llmRoutingMetrics.recordHarnessInvocation(goal.goalType());
    }

    private GoalClassificationContext buildClassificationContext(String chatId, String userId) {
        return new GoalClassificationContext(buildHistoryBlock(chatId, userId, "", 4));
    }

    private String localizeReply(ChatLanguageTurn languageTurn, String englishReply) {
        return chatLanguageService.localizeReply(languageTurn, englishReply);
    }

    private String formatHarnessReply(ChatLanguageTurn languageTurn, String engineReply) {
        return LlmResponseSanitizer.formatForChatDisplay(localizeReply(languageTurn, engineReply));
    }

    private SseEmitter streamViaHarnessEngine(
            String chatId, String userId, ChatLanguageTurn languageTurn, GoalClassification goal, RateLimitTier tier) {
        SseEmitter emitter = new SseEmitter(300_000L);
        String sessionId = userId + "-" + chatId;
        RateLimitTier metricsTier = tier != null ? tier : RateLimitTier.DEFAULT;
        CompletableFuture.runAsync(() -> {
            Timer.Sample turnSample = chatTurnMetrics.startTurn(metricsTier);
            chatStreamActivityPublisher.register(sessionId, emitter);
            try {
                sendAgentEvent(emitter, Map.of(
                        "type", "agent_start",
                        "agentId", "doctor-match-harness",
                        "orchestrator", false));
                chatStreamActivityPublisher.publishReasoning(sessionId, "Running GraphRAG specialist matching…");

                Map<String, ChatMessage> result = processViaHarnessEngine(chatId, userId, languageTurn, goal);
                ChatMessage assistant = result.get("assistantMessage");
                String reply = assistant.content();

                emitter.send(SseEmitter.event().name("token").data(Map.of("t", reply)));
                sendPipelineStageEvents(emitter, sessionId);
                sendAgentEvent(emitter, Map.of("type", "agent_done", "agentId", "doctor-match-harness"));
                emitter.send(SseEmitter.event().name("done").data(Map.of(
                        "id", assistant.id(),
                        "content", reply)));
                chatTurnMetrics.recordTurnSuccess(turnSample, metricsTier);
                emitter.complete();
            } catch (Exception e) {
                log.warn("Harness stream failed for chat {}: {}", chatId, e.getMessage());
                chatTurnMetrics.recordStreamError();
                try {
                    chatService.appendAssistantMessage(chatId, userId,
                            "Sorry, the specialist matching engine encountered an error. Please try again.");
                } catch (Exception ignored) {
                    // best effort
                }
                emitter.completeWithError(e);
            } finally {
                chatStreamActivityPublisher.unregister(sessionId);
            }
        });
        return emitter;
    }

    private String invokeSync(TurnContext ctx) {
        log.info("Chat LLM turn — agent: {}, session: {}, model: {}, tier: {}, tokenBudget: {}",
                ctx.profile().agentId(), ctx.sessionId(), functionGemmaModelName,
                ctx.routingTier(), RoutingTierResolver.maxTokensFor(ctx.routingTier(), llmTierProperties));
        logStreamService.sendLog(ctx.sessionId(), "INFO", "Chat Agent",
                "Invoking agent " + ctx.profile().agentId() + " via " + functionGemmaModelName
                        + " (tier=" + ctx.routingTier() + ")");

        llmRoutingMetrics.recordLlmCall(LlmClientType.TOOL_CALLING, ctx.routingTier(), ctx.goal().goalType());
        String reply = llmCallLimiter.execute(LlmClientType.TOOL_CALLING, () -> chatClient.prompt()
                .system(ctx.systemPrompt())
                .user(ctx.userPrompt())
                .advisors(a -> SessionAdvisorSupport.applyOrchestratorContext(a, ctx.sessionId()))
                .call()
                .content());

        if (reply == null || reply.isBlank()) {
            return "I could not generate a response. Please try again or rephrase your question.";
        }
        return applyChatPolicyGate(reply.trim(), Map.of("agentId", ctx.profile().agentId()));
    }

    private String applyChatPolicyGate(String reply, Map<String, Object> metadata) {
        if (!harnessProperties.policyGateChatEnabled()) {
            return reply;
        }
        MedicalAgentPolicyGateService.PolicyGateResult policyGate = medicalAgentPolicyGateService.review(reply, metadata);
        return policyGate.sanitizedResponse();
    }

    private String resolveReplyAfterStream(TurnContext ctx, String streamedText) {
        if (streamedText != null && !streamedText.isBlank()) {
            return applyChatPolicyGate(streamedText.trim(), Map.of("agentId", ctx.profile().agentId()));
        }
        log.warn("Chat stream returned no text for session {}; retrying sync tool-calling turn",
                ctx.sessionId());
        logStreamService.sendLog(ctx.sessionId(), "WARN", "Chat Agent",
                "Stream empty — retrying sync " + functionGemmaModelName + " turn");
        return invokeSync(ctx);
    }

    private void streamReplyTokens(SseEmitter emitter, String streamedText, String reply) throws IOException {
        if (streamedText != null && !streamedText.isBlank()) {
            return;
        }
        emitter.send(SseEmitter.event().name("token").data(Map.of("t", reply)));
    }

    private TurnContext prepareTurn(String chatId, String userId, ChatLanguageTurn languageTurn,
                                     String agentIdOverride, GoalClassification goal) {
        Chat chat = chatService.requireOwnedChat(chatId, userId);
        ChatAgentProfile profile = resolveProfile(chat, agentIdOverride);

        if (agentIdOverride != null && !agentIdOverride.isBlank()
                && !agentIdOverride.equals(chat.agentId())) {
            chatService.updateAgentId(chatId, userId, profile.agentId());
        }

        String displayContent = languageTurn.originalText().trim();
        ChatMessage userMessage = chatService.appendUserMessage(chatId, userId, displayContent);
        String sessionId = userId + "-" + chatId;
        try {
            sessionService.appendMessage(sessionId, new UserMessage(displayContent));
        } catch (Exception ignored) {
        }
        logStreamService.setCurrentSessionId(sessionId);
        OrchestrationContextHolder.setSessionId(sessionId);
        ChatToolContextHolder.setProfile(profile);
        ChatToolContextHolder.setGoalType(goal.goalType());

        String systemPrompt = buildSystemPrompt(profile);
        String userPrompt = buildUserPrompt(profile, languageTurn.processingText(), goal, chatId, userId);
        RoutingTier routingTier = RoutingTierResolver.fromClassification(goal);
        return new TurnContext(userMessage, sessionId, profile, goal, routingTier, systemPrompt, userPrompt, languageTurn);
    }

    private void clearTurnContext(String sessionId) {
        ChatToolContextHolder.clear();
        OrchestrationContextHolder.clear();
        logStreamService.clearCurrentSessionId();
    }

    private ChatAgentProfile resolveProfile(Chat chat, String agentIdOverride) {
        String requested = agentIdOverride != null && !agentIdOverride.isBlank()
                ? agentIdOverride
                : chat.agentId();
        return ChatAgentProfile.fromAgentId(requested).orElse(ChatAgentProfile.AUTO);
    }

    private String buildSystemPrompt(ChatAgentProfile profile) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("agentId", profile.agentId());
        vars.put("agentInstructions", buildAgentInstructions(profile));
        return chatAgentSystemTemplate.render(vars);
    }

    private String buildAgentInstructions(ChatAgentProfile profile) {
        if (profile.orchestrator()) {
            return chatAgentOrchestratorInstructionsTemplate.render(Collections.emptyMap());
        }
        List<String> skillBodies = new ArrayList<>();
        for (String skill : profile.skills()) {
            skillBodies.add(promptSupportService.loadSkill(skill));
        }
        return promptSupportService.buildPrompt(skillBodies, "Respond to the user's chat message.", Map.of());
    }

    private String buildUserPrompt(ChatAgentProfile profile, String content) {
        String routingHint = "";
        if (profile.orchestrator()) {
            routingHint = ChatAgentProfile.classifyIntent(content)
                    .map(hint -> "Routing hint — consider specialist: " + hint.agentId() + "\n\n")
                    .orElse("");
        }
        return chatUserMessageTemplate.render(Map.of(
                "caseToolHints", chatCasePromptSupport.buildCaseToolHints(content),
                "routingHint", routingHint,
                "historyBlock", "",
                "userMessage", content));
    }

    private String buildUserPrompt(ChatAgentProfile profile, String content, GoalClassification goal,
                                    String chatId, String userId) {
        String goalHint = "Goal identified: " + goal.goalType().name()
                + (goal.caseId().isPresent() ? " | case ID: " + goal.caseId().get() : "") + "\n\n";
        String routingHint = "";
        if (profile.orchestrator() && goal.goalType() != GoalType.GENERAL_QUESTION) {
            routingHint = "Use tools matching the identified goal (" + goal.goalType().name()
                    + "). Do NOT delegate via Task — call the appropriate tool directly.\n\n";
        }
        if (goal.goalType() == GoalType.ANALYZE_CASE && goal.caseId().isPresent()) {
            routingHint = routingHint
                    + "Call analyze_case with case ID " + goal.caseId().get()
                    + ". Do NOT ask the user for case text.\n\n";
        }
        String historyBlock = buildHistoryBlock(chatId, userId, content, 6, goal);
        return chatUserMessageTemplate.render(Map.of(
                "caseToolHints", chatCasePromptSupport.buildCaseToolHints(content, goal),
                "routingHint", goalHint + routingHint,
                "historyBlock", historyBlock,
                "userMessage", content));
    }

    private String buildHistoryBlock(String chatId, String userId, String currentContent, int limit) {
        return buildHistoryBlock(chatId, userId, currentContent, limit, null);
    }

    private String buildHistoryBlock(
            String chatId, String userId, String currentContent, int limit, GoalClassification goal) {
        boolean includeHistory = goal != null && goal.summary() != null
                && goal.summary().startsWith("follow-up:");
        if (goal != null && goal.hasCaseId()) {
            includeHistory = true;
        }
        String sessionId = userId + "-" + chatId;
        ConversationGoalContext.Entry ctx = ConversationGoalContext.get(sessionId);
        if (ctx != null && ctx.lastCaseId() != null && !ctx.lastCaseId().isBlank()) {
            includeHistory = true;
        }
        if (!includeHistory) {
            return "";
        }
        var history = chatService.getHistory(chatId, userId, limit, 0);
        if (history.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("Previous conversation:\n");
        for (var msg : history) {
            if (currentContent != null && msg.content().equals(currentContent)) {
                continue;
            }
            String snippet = msg.content();
            if (snippet.length() > 500) {
                snippet = snippet.substring(0, 500) + "...";
            }
            sb.append(msg.role()).append(": ").append(snippet).append("\n\n");
        }
        return sb.toString();
    }

    private static void sendAgentEvent(SseEmitter emitter, Map<String, Object> payload) {
        try {
            emitter.send(SseEmitter.event().name("agent").data(payload));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void sendPipelineStageEvents(SseEmitter emitter, String sessionId) {
        var stages = pipelineProgressCollector.drainStages(sessionId);
        for (var stage : stages) {
            try {
                emitter.send(SseEmitter.event().name("pipeline_stage").data(stage.toPayload()));
            } catch (IOException e) {
                log.debug("Failed to send pipeline stage event for session {}: {}", sessionId, e.getMessage());
                break;
            }
        }
    }

    private Map<String, ChatMessage> processViaHarnessEngine(
            String chatId, String userId, ChatLanguageTurn languageTurn, GoalClassification goal) {
        String caseId = goal.caseId().filter(id -> !id.isBlank())
                .orElseThrow(() -> new IllegalStateException(
                        "Harness routing requires a non-blank case ID for goal " + goal.goalType()));
        log.info("Routing to harness engine: goal={}, caseId={}", goal.goalType(), caseId);
        recordHarnessRoute(goal);

        ChatMessage userMessage = chatService.appendUserMessage(chatId, userId, languageTurn.originalText().trim());
        String sessionId = userId + "-" + chatId;
        logStreamService.setCurrentSessionId(sessionId);
        OrchestrationContextHolder.setSessionId(sessionId);

        try {
            logStreamService.sendLog(sessionId, "INFO", "HARNESS_GOAL",
                    "Goal identified: " + goal.goalType() + " — routing to workflow engine");
            sendHarnessProgress(sessionId, goal.goalType().name(), "PLANNING", "Starting workflow for case " + caseId);

            MedicalAgentService.AgentResponse engineResponse = switch (goal.goalType()) {
                case MATCH_DOCTORS -> medicalAgentService.matchDoctors(caseId,
                        buildHarnessRequest(sessionId, languageTurn.processingText()));
                case ROUTE_CASE -> medicalAgentService.routeCase(caseId, Map.of("sessionId", sessionId));
                default -> new MedicalAgentService.AgentResponse(
                        "Goal " + goal.goalType() + " is not supported by harness engines yet.",
                        Map.of("goalType", goal.goalType().name()));
            };

            if (harnessProperties.zeroResultFallbackEnabled() && isZeroResult(goal.goalType(), engineResponse)) {
                log.info("Engine returned zero results for case {}; falling back to LLM chat", caseId);
                sendHarnessProgress(sessionId, "LLM_FALLBACK", "PLANNING",
                        "GraphRAG found no exact matches — consulting LLM");
                ChatMessage fallbackMessage = fallbackToLlmChat(
                        chatId, userId, languageTurn.processingText(), goal, sessionId);
                OrchestrationContextHolder.clear();
                logStreamService.clearCurrentSessionId();
                return Map.of("userMessage", userMessage, "assistantMessage", fallbackMessage);
            }

            String engineName = goal.goalType() == GoalType.MATCH_DOCTORS ? "DoctorMatch" : "Routing";
            Object matchCount = engineResponse.metadata() != null
                    ? engineResponse.metadata().getOrDefault("doctorMatchCount",
                    engineResponse.metadata().getOrDefault("facilityMatchCount", 0))
                    : 0;
            boolean requiresClinicianReview = engineResponse.metadata() != null
                    && Boolean.TRUE.equals(engineResponse.metadata().get("requiresClinicianReview"));
            boolean lowConfidenceClarify = engineResponse.metadata() != null
                    && "CLARIFY".equals(engineResponse.metadata().get("policyAction"))
                    && matchCount instanceof Number n && n.intValue() > 0;
            String progressDetail = requiresClinicianReview
                    ? "Clinician review required — " + matchCount + " matches found"
                    : lowConfidenceClarify
                    ? "Low confidence — " + matchCount + " matches shown"
                    : "Completed — " + matchCount + " matches found";
            String progressState = requiresClinicianReview ? "ESCALATE" : lowConfidenceClarify ? "CLARIFY" : "DONE";
            sendHarnessProgress(sessionId, engineName, progressState, progressDetail);

            String responseText = formatHarnessReply(languageTurn, engineResponse.response());
            ChatMessage assistantMessage = chatService.appendAssistantMessage(chatId, userId, responseText);
            ConversationGoalContext.set(goal.goalType(), caseId, sessionId);
            try {
                sessionService.appendMessage(sessionId, new AssistantMessage(responseText));
            } catch (Exception ignored) {
                // best effort
            }
            return Map.of("userMessage", userMessage, "assistantMessage", assistantMessage);
        } catch (Exception e) {
            log.warn("Harness engine failed for chat {} goal {}: {}", chatId, goal.goalType(), e.getMessage());
            sendHarnessProgress(userId + "-" + chatId, goal.goalType().name(), "FAILED", "Error: " + e.getMessage());
            ChatMessage assistantMessage = chatService.appendAssistantMessage(
                    chatId, userId, "Sorry, the specialist matching engine encountered an error. Please try again.");
            return Map.of("userMessage", userMessage, "assistantMessage", assistantMessage);
        } finally {
            OrchestrationContextHolder.clear();
            logStreamService.clearCurrentSessionId();
        }
    }

    private static Map<String, Object> buildHarnessRequest(String sessionId, String userMessage) {
        Map<String, Object> request = new HashMap<>();
        request.put("sessionId", sessionId);
        if (GoalClassifier.requestsMoreDoctors(userMessage)) {
            request.put("excludePreviouslyMatched", true);
        }
        return request;
    }

    private boolean isZeroResult(GoalType goalType, MedicalAgentService.AgentResponse response) {
        if (response.metadata() == null) return true;
        String key = goalType == GoalType.MATCH_DOCTORS ? "doctorMatchCount" : "facilityMatchCount";
        Object count = response.metadata().get(key);
        if (count instanceof Number n) return n.intValue() == 0;
        if (count instanceof String s) {
            try { return Integer.parseInt(s) == 0; } catch (NumberFormatException ignored) {}
        }
        return count == null;
    }

    private ChatMessage fallbackToLlmChat(String chatId, String userId, String content,
                                           GoalClassification goal, String prevSessionId) {
        ChatAgentProfile autoProfile = ChatAgentProfile.AUTO;
        Chat chat = chatService.requireOwnedChat(chatId, userId);
        ChatAgentProfile profile = autoProfile;
        String sessionId = userId + "-" + chatId + "-fallback";
        logStreamService.setCurrentSessionId(sessionId);
        OrchestrationContextHolder.setSessionId(sessionId);
        ChatToolContextHolder.setProfile(profile);
        ChatToolContextHolder.setGoalType(goal.goalType());

        try {
            String noMatchHint = "GraphRAG found no exact matches for this case. "
                    + "Suggest alternative search criteria or broader specialties.";
            String augmentedContent = noMatchHint + "\n\nOriginal request: " + content;
            String userPrompt = chatUserMessageTemplate.render(Map.of(
                    "caseToolHints", chatCasePromptSupport.buildCaseToolHints(content, goal),
                    "routingHint", "Goal: " + goal.goalType().name() + " | No exact matches from GraphRAG.\n\n",
                    "historyBlock", "",
                    "userMessage", augmentedContent));
            String systemPrompt = buildSystemPrompt(profile);

            String reply = llmCallLimiter.execute(LlmClientType.TOOL_CALLING, () -> chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .advisors(a -> SessionAdvisorSupport.applyOrchestratorContext(a, sessionId))
                    .call()
                    .content());

            String finalReply = applyChatPolicyGate(
                    reply != null && !reply.isBlank() ? reply.trim() : "No alternative matches could be suggested.",
                    Map.of("agentId", profile.agentId()));
            return chatService.appendAssistantMessage(chatId, userId, finalReply);
        } finally {
            ChatToolContextHolder.clear();
            OrchestrationContextHolder.clear();
            logStreamService.clearCurrentSessionId();
        }
    }

    private Map<String, ChatMessage> processViaCaseAnalysisEngine(
            String chatId, String userId, ChatLanguageTurn languageTurn, GoalClassification goal) {
        String caseId = goal.caseId().filter(id -> !id.isBlank())
                .orElseThrow(() -> new IllegalStateException(
                        "Case analysis harness requires a non-blank case ID"));
        log.info("Routing to case analysis harness: caseId={}", caseId);
        recordHarnessRoute(goal);

        ChatMessage userMessage = chatService.appendUserMessage(chatId, userId, languageTurn.originalText().trim());
        String sessionId = userId + "-" + chatId;
        logStreamService.setCurrentSessionId(sessionId);
        OrchestrationContextHolder.setSessionId(sessionId);

        try {
            logStreamService.sendLog(sessionId, "INFO", "HARNESS_GOAL",
                    "Goal identified: ANALYZE_CASE — routing to case analysis workflow");
            sendHarnessProgress(sessionId, "CaseAnalysis", "PLANNING", "Starting analysis for case " + caseId);

            MedicalAgentService.AgentResponse engineResponse = medicalAgentService.analyzeCase(
                    caseId, Map.of("sessionId", sessionId, "userFocus", languageTurn.originalText().trim()));

            sendHarnessProgress(sessionId, "CaseAnalysis", "DONE", "Case analysis completed");

            String responseText = formatHarnessReply(languageTurn, engineResponse.response());
            ChatMessage assistantMessage = chatService.appendAssistantMessage(chatId, userId, responseText);
            ConversationGoalContext.set(goal.goalType(), caseId, sessionId);
            try {
                sessionService.appendMessage(sessionId, new AssistantMessage(responseText));
            } catch (Exception ignored) {
            }
            return Map.of("userMessage", userMessage, "assistantMessage", assistantMessage);
        } catch (Exception e) {
            log.warn("Case analysis harness failed for chat {}: {}", chatId, e.getMessage());
            sendHarnessProgress(sessionId, "CaseAnalysis", "FAILED", "Error: " + e.getMessage());
            ChatMessage assistantMessage = chatService.appendAssistantMessage(
                    chatId, userId, "Sorry, the case analysis engine encountered an error. Please try again.");
            return Map.of("userMessage", userMessage, "assistantMessage", assistantMessage);
        } finally {
            OrchestrationContextHolder.clear();
            logStreamService.clearCurrentSessionId();
        }
    }

    private SseEmitter streamViaCaseAnalysisEngine(
            String chatId, String userId, ChatLanguageTurn languageTurn, GoalClassification goal, RateLimitTier tier) {
        SseEmitter emitter = new SseEmitter(300_000L);
        String sessionId = userId + "-" + chatId;
        RateLimitTier metricsTier = tier != null ? tier : RateLimitTier.DEFAULT;
        CompletableFuture.runAsync(() -> {
            Timer.Sample turnSample = chatTurnMetrics.startTurn(metricsTier);
            chatStreamActivityPublisher.register(sessionId, emitter);
            try {
                sendAgentEvent(emitter, Map.of(
                        "type", "agent_start",
                        "agentId", "case-analysis-harness",
                        "orchestrator", false));
                chatStreamActivityPublisher.publishReasoning(sessionId, "Running case analysis workflow…");

                Map<String, ChatMessage> result = processViaCaseAnalysisEngine(chatId, userId, languageTurn, goal);
                ChatMessage assistant = result.get("assistantMessage");
                String reply = assistant.content();

                emitter.send(SseEmitter.event().name("token").data(Map.of("t", reply)));
                sendPipelineStageEvents(emitter, sessionId);
                sendAgentEvent(emitter, Map.of("type", "agent_done", "agentId", "case-analysis-harness"));
                emitter.send(SseEmitter.event().name("done").data(Map.of(
                        "id", assistant.id(),
                        "content", reply)));
                chatTurnMetrics.recordTurnSuccess(turnSample, metricsTier);
                emitter.complete();
            } catch (Exception e) {
                log.warn("Case analysis harness stream failed for chat {}: {}", chatId, e.getMessage());
                chatTurnMetrics.recordStreamError();
                try {
                    chatService.appendAssistantMessage(chatId, userId,
                            "Sorry, the case analysis engine encountered an error. Please try again.");
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            } finally {
                chatStreamActivityPublisher.unregister(sessionId);
            }
        });
        return emitter;
    }

    private void sendHarnessProgress(String sessionId, String engine, String state, String detail) {
        logStreamService.sendLog(sessionId, "INFO", "HARNESS_PROGRESS",
                "{\"engine\": \"" + engine + "\", \"state\": \"" + state + "\", \"detail\": \"" + detail + "\"}");
    }

    private record TurnContext(
            ChatMessage userMessage,
            String sessionId,
            ChatAgentProfile profile,
            GoalClassification goal,
            RoutingTier routingTier,
            String systemPrompt,
            String userPrompt,
            ChatLanguageTurn languageTurn) {}
}