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
import com.berdachuk.medexpertmatch.llm.service.ChatStreamActivityPublisher;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentPromptSupportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
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
    private final String functionGemmaModelName;

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
            @Value("${spring.ai.custom.tool-calling.model:functiongemma}") String functionGemmaModelName) {
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
        this.functionGemmaModelName = functionGemmaModelName;
    }

    @Override
    public Map<String, ChatMessage> processMessage(String chatId, String userId, String content, String agentIdOverride) {
        TurnContext ctx = prepareTurn(chatId, userId, content, agentIdOverride);
        try {
            String reply = invokeSync(ctx);
            ChatMessage assistantMessage = chatService.appendAssistantMessage(chatId, userId, reply);
            return Map.of("userMessage", ctx.userMessage(), "assistantMessage", assistantMessage);
        } catch (Exception e) {
            log.warn("Chat LLM turn failed for chat {}: {}", chatId, e.getMessage());
            ChatMessage assistantMessage = chatService.appendAssistantMessage(
                    chatId, userId, "Sorry, the assistant encountered an error. Please try again.");
            return Map.of("userMessage", ctx.userMessage(), "assistantMessage", assistantMessage);
        } finally {
            clearTurnContext(ctx.sessionId());
        }
    }

    @Override
    public SseEmitter streamMessage(String chatId, String userId, String content, String agentIdOverride,
                                      RateLimitTier tier) {
        TurnContext ctx = prepareTurn(chatId, userId, content, agentIdOverride);
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
                                sendAgentEvent(emitter, Map.of("type", "agent_done", "agentId", ctx.profile().agentId()));
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
        return reply.trim();
    }

    /**
     * Tool-calling models often finish with no streamable text tokens; fall back to a sync call
     * so tool results still reach the user.
     */
    private String resolveReplyAfterStream(TurnContext ctx, String streamedText) {
        if (streamedText != null && !streamedText.isBlank()) {
            return streamedText.trim();
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
        Chat chat = chatService.requireOwnedChat(chatId, userId);
        ChatAgentProfile profile = resolveProfile(chat, agentIdOverride);

        if (agentIdOverride != null && !agentIdOverride.isBlank()
                && !agentIdOverride.equals(chat.agentId())) {
            chatService.updateAgentId(chatId, userId, profile.agentId());
        }

        ChatMessage userMessage = chatService.appendUserMessage(chatId, userId, content.trim());
        String sessionId = userId + "-" + chatId;
        logStreamService.setCurrentSessionId(sessionId);
        OrchestrationContextHolder.setSessionId(sessionId);

        String systemPrompt = buildSystemPrompt(profile);
        String userPrompt = buildUserPrompt(profile, content);
        return new TurnContext(userMessage, sessionId, profile, systemPrompt, userPrompt);
    }

    private void clearTurnContext(String sessionId) {
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
                "userMessage", content));
    }

    private static void sendAgentEvent(SseEmitter emitter, Map<String, Object> payload) {
        try {
            emitter.send(SseEmitter.event().name("agent").data(payload));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private record TurnContext(
            ChatMessage userMessage,
            String sessionId,
            ChatAgentProfile profile,
            String systemPrompt,
            String userPrompt) {}
}
