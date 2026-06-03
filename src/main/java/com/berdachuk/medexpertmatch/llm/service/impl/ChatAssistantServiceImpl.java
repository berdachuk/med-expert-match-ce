package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.chat.domain.Chat;
import com.berdachuk.medexpertmatch.chat.domain.ChatMessage;
import com.berdachuk.medexpertmatch.chat.service.ChatAssistantService;
import com.berdachuk.medexpertmatch.chat.service.ChatTurnMetrics;
import com.berdachuk.medexpertmatch.chat.service.ChatService;
import com.berdachuk.medexpertmatch.core.domain.RateLimitTier;
import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.llm.chat.ChatCasePromptSupport;
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
import com.berdachuk.medexpertmatch.llm.harness.MedicalAgentCriticService;
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
    private final MedicalAgentCriticService medicalAgentCriticService;
    private final HarnessProperties harnessProperties;
    private final String functionGemmaModelName;
    private final GoalClassifier goalClassifier;
    private final MedicalAgentService medicalAgentService;
    private final SessionService sessionService;
    private final PipelineProgressCollector pipelineProgressCollector;

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
            MedicalAgentCriticService medicalAgentCriticService,
            HarnessProperties harnessProperties,
            @Value("${spring.ai.custom.tool-calling.model:functiongemma}") String functionGemmaModelName,
            GoalClassifier goalClassifier,
            MedicalAgentService medicalAgentService,
            SessionService sessionService,
            PipelineProgressCollector pipelineProgressCollector) {
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
        this.medicalAgentCriticService = medicalAgentCriticService;
        this.harnessProperties = harnessProperties;
        this.functionGemmaModelName = functionGemmaModelName;
        this.goalClassifier = goalClassifier;
        this.medicalAgentService = medicalAgentService;
        this.sessionService = sessionService;
        this.pipelineProgressCollector = pipelineProgressCollector;
    }

    @Override
    public Map<String, ChatMessage> processMessage(String chatId, String userId, String content, String agentIdOverride) {
        String sessionId = userId + "-" + chatId;
        OrchestrationContextHolder.setSessionId(sessionId);
        try {
            GoalClassification goal = goalClassifier.classify(content);
            log.info("Goal classified: {} (caseId={})", goal.goalType(), goal.caseId().orElse("none"));

            if (goal.isRoutableToEngine() && goal.hasCaseId()) {
                return processViaHarnessEngine(chatId, userId, content, goal);
            }

            TurnContext ctx = prepareTurn(chatId, userId, content, agentIdOverride, goal);
            try {
                String reply = invokeSync(ctx);
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
        String sessionId = userId + "-" + chatId;
        OrchestrationContextHolder.setSessionId(sessionId);
        GoalClassification goal;
        try {
            goal = goalClassifier.classify(content);
        } finally {
            OrchestrationContextHolder.clear();
        }
        log.info("Goal classified: {} (caseId={})", goal.goalType(), goal.caseId().orElse("none"));
        TurnContext ctx = prepareTurn(chatId, userId, content, agentIdOverride, goal);
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
                                String reply = resolveReplyAfterStream(ctx, full.toString());
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

    private String invokeSync(TurnContext ctx) {
        log.info("Chat LLM turn — agent: {}, session: {}, model: {}",
                ctx.profile().agentId(), ctx.sessionId(), functionGemmaModelName);
        logStreamService.sendLog(ctx.sessionId(), "INFO", "Chat Agent",
                "Invoking agent " + ctx.profile().agentId() + " via " + functionGemmaModelName);

        String reply = llmCallLimiter.execute(LlmClientType.TOOL_CALLING, () -> chatClient.prompt()
                .system(ctx.systemPrompt())
                .user(ctx.userPrompt())
                .advisors(a -> SessionAdvisorSupport.applyOrchestratorContext(a, ctx.sessionId()))
                .call()
                .content());

        if (reply == null || reply.isBlank()) {
            return "I could not generate a response. Please try again or rephrase your question.";
        }
        return applyChatCritic(reply.trim(), Map.of("agentId", ctx.profile().agentId()));
    }

    private String applyChatCritic(String reply, Map<String, Object> metadata) {
        if (!harnessProperties.criticChatEnabled()) {
            return reply;
        }
        MedicalAgentCriticService.CriticResult critic = medicalAgentCriticService.review(reply, metadata);
        return critic.sanitizedResponse();
    }

    private String resolveReplyAfterStream(TurnContext ctx, String streamedText) {
        if (streamedText != null && !streamedText.isBlank()) {
            return applyChatCritic(streamedText.trim(), Map.of("agentId", ctx.profile().agentId()));
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

    private TurnContext prepareTurn(String chatId, String userId, String content, String agentIdOverride) {
        return prepareTurn(chatId, userId, content, agentIdOverride, GoalClassification.general());
    }

    private TurnContext prepareTurn(String chatId, String userId, String content, String agentIdOverride,
                                     GoalClassification goal) {
        Chat chat = chatService.requireOwnedChat(chatId, userId);
        ChatAgentProfile profile = resolveProfile(chat, agentIdOverride);

        if (agentIdOverride != null && !agentIdOverride.isBlank()
                && !agentIdOverride.equals(chat.agentId())) {
            chatService.updateAgentId(chatId, userId, profile.agentId());
        }

        ChatMessage userMessage = chatService.appendUserMessage(chatId, userId, content.trim());
        String sessionId = userId + "-" + chatId;
        try {
            sessionService.appendMessage(sessionId, new UserMessage(content.trim()));
        } catch (Exception ignored) {
        }
        logStreamService.setCurrentSessionId(sessionId);
        OrchestrationContextHolder.setSessionId(sessionId);
        ChatToolContextHolder.setProfile(profile);

        String systemPrompt = buildSystemPrompt(profile);
        String userPrompt = buildUserPrompt(profile, content, goal, chatId, userId);
        return new TurnContext(userMessage, sessionId, profile, goal, systemPrompt, userPrompt);
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

    private String buildUserPrompt(ChatAgentProfile profile, String content, GoalClassification goal, String chatId, String userId) {
        String goalHint = "Goal identified: " + goal.goalType().name()
                + (goal.caseId().isPresent() ? " | case ID: " + goal.caseId().get() : "") + "\n\n";
        String routingHint = "";
        if (profile.orchestrator() && goal.goalType() != GoalType.GENERAL_QUESTION) {
            routingHint = "Use tools matching the identified goal (" + goal.goalType().name()
                    + "). Do NOT delegate via Task — call the appropriate tool directly.\n\n";
        }
        String historyBlock = "";
        if (goal.summary() != null && goal.summary().startsWith("follow-up:")) {
            var history = chatService.getHistory(chatId, userId, 6, 0);
            if (!history.isEmpty()) {
                StringBuilder sb = new StringBuilder("Previous conversation:\n");
                for (var msg : history) {
                    if (!msg.content().equals(content)) {
                        sb.append(msg.role()).append(": ").append(msg.content()).append("\n\n");
                    }
                }
                historyBlock = sb.toString();
            }
        }
        return chatUserMessageTemplate.render(Map.of(
                "caseToolHints", chatCasePromptSupport.buildCaseToolHints(content, goal),
                "routingHint", goalHint + routingHint,
                "historyBlock", historyBlock,
                "userMessage", content));
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
            String chatId, String userId, String content, GoalClassification goal) {
        String caseId = goal.caseId().orElseThrow();
        log.info("Routing to harness engine: goal={}, caseId={}", goal.goalType(), caseId);

        ChatMessage userMessage = chatService.appendUserMessage(chatId, userId, content.trim());
        String sessionId = userId + "-" + chatId;
        logStreamService.setCurrentSessionId(sessionId);
        OrchestrationContextHolder.setSessionId(sessionId);

        try {
            logStreamService.sendLog(sessionId, "INFO", "HARNESS_GOAL",
                    "Goal identified: " + goal.goalType() + " — routing to workflow engine");
            sendHarnessProgress(sessionId, goal.goalType().name(), "PLANNING", "Starting workflow for case " + caseId);

            MedicalAgentService.AgentResponse engineResponse = switch (goal.goalType()) {
                case MATCH_DOCTORS -> medicalAgentService.matchDoctors(caseId, Map.of("sessionId", sessionId));
                case ROUTE_CASE -> medicalAgentService.routeCase(caseId, Map.of("sessionId", sessionId));
                default -> new MedicalAgentService.AgentResponse(
                        "Goal " + goal.goalType() + " is not supported by harness engines yet.",
                        Map.of("goalType", goal.goalType().name()));
            };

            if (harnessProperties.zeroResultFallbackEnabled() && isZeroResult(goal.goalType(), engineResponse)) {
                log.info("Engine returned zero results for case {}; falling back to LLM chat", caseId);
                sendHarnessProgress(sessionId, "LLM_FALLBACK", "PLANNING",
                        "GraphRAG found no exact matches — consulting LLM");
                ChatMessage fallbackMessage = fallbackToLlmChat(chatId, userId, content, goal, sessionId);
                OrchestrationContextHolder.clear();
                logStreamService.clearCurrentSessionId();
                return Map.of("userMessage", userMessage, "assistantMessage", fallbackMessage);
            }

            String engineName = goal.goalType() == GoalType.MATCH_DOCTORS ? "DoctorMatch" : "Routing";
            Object matchCount = engineResponse.metadata() != null
                    ? engineResponse.metadata().getOrDefault("doctorMatchCount",
                    engineResponse.metadata().getOrDefault("facilityMatchCount", 0))
                    : 0;
            sendHarnessProgress(sessionId, engineName, "DONE", "Completed — " + matchCount + " matches found");

            ChatMessage assistantMessage = chatService.appendAssistantMessage(
                    chatId, userId, engineResponse.response());
            ConversationGoalContext.set(goal.goalType(), caseId, sessionId);
            try {
                sessionService.appendMessage(sessionId, new AssistantMessage(engineResponse.response()));
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

            String finalReply = applyChatCritic(
                    reply != null && !reply.isBlank() ? reply.trim() : "No alternative matches could be suggested.",
                    Map.of("agentId", profile.agentId()));
            return chatService.appendAssistantMessage(chatId, userId, finalReply);
        } finally {
            ChatToolContextHolder.clear();
            OrchestrationContextHolder.clear();
            logStreamService.clearCurrentSessionId();
        }
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
            String systemPrompt,
            String userPrompt) {}
}