package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.chat.domain.Chat;
import com.berdachuk.medexpertmatch.chat.domain.ChatMessage;
import com.berdachuk.medexpertmatch.chat.service.ChatAssistantService;
import com.berdachuk.medexpertmatch.chat.service.ChatService;
import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.core.util.LlmCallLimiter;
import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import com.berdachuk.medexpertmatch.llm.chat.ChatAgentProfile;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentPromptSupportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ChatAssistantServiceImpl implements ChatAssistantService {

    private final ChatService chatService;
    private final ChatClient chatClient;
    private final MedicalAgentPromptSupportService promptSupportService;
    private final LogStreamService logStreamService;
    private final LlmCallLimiter llmCallLimiter;
    private final PromptTemplate chatAgentSystemTemplate;
    private final String functionGemmaModelName;

    public ChatAssistantServiceImpl(
            ChatService chatService,
            @Qualifier("medicalAgentChatClient") ChatClient chatClient,
            MedicalAgentPromptSupportService promptSupportService,
            LogStreamService logStreamService,
            LlmCallLimiter llmCallLimiter,
            @Value("${spring.ai.custom.tool-calling.model:functiongemma}") String functionGemmaModelName) {
        this.chatService = chatService;
        this.chatClient = chatClient;
        this.promptSupportService = promptSupportService;
        this.logStreamService = logStreamService;
        this.llmCallLimiter = llmCallLimiter;
        this.functionGemmaModelName = functionGemmaModelName;
        this.chatAgentSystemTemplate = new PromptTemplate(new ClassPathResource("prompts/chat-agent-system.st"));
    }

    @Override
    public Map<String, ChatMessage> processMessage(String chatId, String userId, String content, String agentIdOverride) {
        Chat chat = chatService.requireOwnedChat(chatId, userId);
        ChatAgentProfile profile = resolveProfile(chat, agentIdOverride, content);

        if (agentIdOverride != null && !agentIdOverride.isBlank()
                && !agentIdOverride.equals(chat.agentId())) {
            chatService.updateAgentId(chatId, userId, profile.agentId());
        }

        ChatMessage userMessage = chatService.appendUserMessage(chatId, userId, content.trim());
        String sessionId = userId + "-" + chatId;

        logStreamService.setCurrentSessionId(sessionId);
        OrchestrationContextHolder.setSessionId(sessionId);

        try {
            String systemPrompt = buildSystemPrompt(profile);
            String userPrompt = buildUserPrompt(profile, content);

            log.info("Chat LLM turn — agent: {}, session: {}, model: {}", profile.agentId(), sessionId, functionGemmaModelName);
            logStreamService.sendLog(sessionId, "INFO", "Chat Agent",
                    "Invoking agent " + profile.agentId() + " via " + functionGemmaModelName);

            String reply = llmCallLimiter.execute(LlmClientType.TOOL_CALLING, () -> chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .advisors(a -> a.param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, sessionId))
                    .call()
                    .content());

            if (reply == null || reply.isBlank()) {
                reply = "I could not generate a response. Please try again or rephrase your question.";
            }

            ChatMessage assistantMessage = chatService.appendAssistantMessage(chatId, userId, reply.trim());
            return Map.of("userMessage", userMessage, "assistantMessage", assistantMessage);
        } catch (Exception e) {
            log.warn("Chat LLM turn failed for chat {}: {}", chatId, e.getMessage());
            ChatMessage assistantMessage = chatService.appendAssistantMessage(
                    chatId,
                    userId,
                    "Sorry, the assistant encountered an error. Please try again.");
            return Map.of("userMessage", userMessage, "assistantMessage", assistantMessage);
        } finally {
            OrchestrationContextHolder.clear();
            logStreamService.clearCurrentSessionId();
        }
    }

    private ChatAgentProfile resolveProfile(Chat chat, String agentIdOverride, String content) {
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
            return """
                    You are in Auto orchestrator mode. Answer directly when possible.
                    For specialized domains, use TodoWrite to plan and Task to delegate to subagents:
                    triage-intake, case-analyzer, evidence-scout, specialist-matcher, routing-planner, network-analyst.
                    Merge subagent results into one cohesive reply.""";
        }
        List<String> skillBodies = new ArrayList<>();
        for (String skill : profile.skills()) {
            skillBodies.add(promptSupportService.loadSkill(skill));
        }
        return promptSupportService.buildPrompt(skillBodies, "Respond to the user's chat message.", Map.of());
    }

    private String buildUserPrompt(ChatAgentProfile profile, String content) {
        if (profile.orchestrator()) {
            return ChatAgentProfile.classifyIntent(content)
                    .map(hint -> "Routing hint — consider specialist: " + hint.agentId() + "\n\nUser message:\n" + content)
                    .orElse(content);
        }
        return "User message:\n" + content;
    }
}
