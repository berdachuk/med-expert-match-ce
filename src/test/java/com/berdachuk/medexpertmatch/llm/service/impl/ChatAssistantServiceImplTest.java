package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.chat.domain.Chat;
import com.berdachuk.medexpertmatch.chat.domain.ChatMessage;
import com.berdachuk.medexpertmatch.chat.service.ChatService;
import com.berdachuk.medexpertmatch.chat.service.ChatTurnMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.core.util.LlmCallLimiter;
import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import com.berdachuk.medexpertmatch.llm.chat.ChatCasePromptSupport;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.session.SessionService;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatAssistantServiceImplTest {

    private final ChatService chatService = mock(ChatService.class);
    private final ChatClient chatClient = mock(ChatClient.class);
    private final ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
    private final ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);
    private final MedicalAgentPromptSupportService promptSupport = mock(MedicalAgentPromptSupportService.class);
    private final LogStreamService logStreamService = mock(LogStreamService.class);
    private final ChatStreamActivityPublisher chatStreamActivityPublisher = mock(ChatStreamActivityPublisher.class);
    private final LlmCallLimiter llmCallLimiter = new LlmCallLimiter(1, 1, 1, 1);
    private final PromptTemplate chatAgentSystemTemplate = mock(PromptTemplate.class);
    private final PromptTemplate chatAgentOrchestratorInstructionsTemplate = mock(PromptTemplate.class);
    private final PromptTemplate chatUserMessageTemplate = mock(PromptTemplate.class);
    private final ChatCasePromptSupport chatCasePromptSupport = mock(ChatCasePromptSupport.class);
    private final MedicalAgentCriticService medicalAgentCriticService = mock(MedicalAgentCriticService.class);
    private final GoalClassifier goalClassifier = mock(GoalClassifier.class);
    private final MedicalAgentService medicalAgentService = mock(MedicalAgentService.class);
    private final SessionService sessionService = mock(SessionService.class);
    private final PipelineProgressCollector pipelineProgressCollector = mock(PipelineProgressCollector.class);

    private final ChatAssistantServiceImpl service = new ChatAssistantServiceImpl(
            chatService, chatClient, promptSupport, logStreamService, chatStreamActivityPublisher, llmCallLimiter,
            new ChatTurnMetrics(new SimpleMeterRegistry()), chatAgentSystemTemplate,
            chatAgentOrchestratorInstructionsTemplate, chatUserMessageTemplate, chatCasePromptSupport,
            medicalAgentCriticService, HarnessProperties.defaults(),
            "functiongemma", goalClassifier, medicalAgentService, sessionService, pipelineProgressCollector);

    @AfterEach
    void clearContext() {
        OrchestrationContextHolder.clear();
    }

    @Test
    @DisplayName("processMessage threads session id and persists assistant reply")
    void processMessageInvokesLlmWithSession() {
        Chat chat = new Chat("c1", "user-a", "Test", "auto", false,
                Instant.now(), Instant.now(), Instant.now(), 0);
        ChatMessage userMsg = new ChatMessage("m1", "c1", "user", "Find evidence", 1, null, Instant.now());
        ChatMessage assistantMsg = new ChatMessage("m2", "c1", "assistant", "Here is evidence", 2, null, Instant.now());

        when(chatService.requireOwnedChat("c1", "user-a")).thenReturn(chat);
        when(chatService.appendUserMessage("c1", "user-a", "Find evidence")).thenReturn(userMsg);
        when(chatService.appendAssistantMessage("c1", "user-a", "Here is evidence")).thenReturn(assistantMsg);
        when(goalClassifier.classify(any())).thenReturn(GoalClassification.general());
        when(promptSupport.loadSkill(any())).thenReturn("skill body");
        when(promptSupport.buildPrompt(any(), any(), any())).thenReturn("prompt");
        when(chatAgentSystemTemplate.render(any())).thenReturn("system");
        when(chatUserMessageTemplate.render(any())).thenReturn("user prompt");
        when(chatCasePromptSupport.buildCaseToolHints(any(), any())).thenReturn("");
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.advisors(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("Here is evidence");
        when(medicalAgentCriticService.review(any(), any()))
                .thenReturn(new MedicalAgentCriticService.CriticResult(true, "Here is evidence", null, null));

        Map<String, ChatMessage> result = service.processMessage("c1", "user-a", "Find evidence", "auto");

        assertNotNull(result.get("userMessage"));
        assertEquals("Here is evidence", result.get("assistantMessage").content());
        verify(logStreamService).setCurrentSessionId("user-a-c1");
        verify(logStreamService).clearCurrentSessionId();
    }

    @Test
    @DisplayName("shouldNotClearConversationGoalContextOnTurnEnd — context survives across turn boundaries")
    void shouldNotClearConversationGoalContextOnTurnEnd() {
        Chat chat = new Chat("c1", "user-a", "Test", "auto", false,
                Instant.now(), Instant.now(), Instant.now(), 0);
        ChatMessage userMsg = new ChatMessage("m1", "c1", "user", "Find evidence", 1, null, Instant.now());
        ChatMessage assistantMsg = new ChatMessage("m2", "c1", "assistant", "Here is evidence", 2, null, Instant.now());

        when(chatService.requireOwnedChat("c1", "user-a")).thenReturn(chat);
        when(chatService.appendUserMessage("c1", "user-a", "Find evidence")).thenReturn(userMsg);
        when(chatService.appendAssistantMessage("c1", "user-a", "Here is evidence")).thenReturn(assistantMsg);
        when(goalClassifier.classify(any())).thenReturn(
                new GoalClassification(GoalType.SEARCH_EVIDENCE, Optional.empty(), Optional.empty(), "keyword: evidence search"));
        when(promptSupport.loadSkill(any())).thenReturn("skill body");
        when(promptSupport.buildPrompt(any(), any(), any())).thenReturn("prompt");
        when(chatAgentSystemTemplate.render(any())).thenReturn("system");
        when(chatUserMessageTemplate.render(any())).thenReturn("user prompt");
        when(chatCasePromptSupport.buildCaseToolHints(any(), any())).thenReturn("");
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.advisors(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("Here is evidence");
        when(medicalAgentCriticService.review(any(), any()))
                .thenReturn(new MedicalAgentCriticService.CriticResult(true, "Here is evidence", null, null));

        service.processMessage("c1", "user-a", "Find evidence", "auto");

        ConversationGoalContext.Entry entry = ConversationGoalContext.get("user-a-c1");
        assertNotNull(entry);
        assertEquals(GoalType.SEARCH_EVIDENCE, entry.lastGoal());
    }

    @Test
    @DisplayName("shouldInjectHistoryIntoFollowUpPrompt — history appears in prompt when follow-up detected")
    void shouldInjectHistoryIntoFollowUpPrompt() {
        Chat chat = new Chat("c1", "user-a", "Test", "auto", false,
                Instant.now(), Instant.now(), Instant.now(), 0);
        ChatMessage userMsg = new ChatMessage("m1", "c1", "user", "more", 2, null, Instant.now());
        ChatMessage assistantMsg = new ChatMessage("m2", "c1", "assistant", "Here is more info", 3, null, Instant.now());
        ChatMessage prevAssistant = new ChatMessage("m0", "c1", "assistant", "Dr. Smith is a cardiologist", 1, null, Instant.now());

        when(chatService.requireOwnedChat("c1", "user-a")).thenReturn(chat);
        when(chatService.appendUserMessage("c1", "user-a", "more")).thenReturn(userMsg);
        when(chatService.appendAssistantMessage("c1", "user-a", "Here is more info")).thenReturn(assistantMsg);
        when(chatService.getHistory("c1", "user-a", 6, 0))
                .thenReturn(List.of(prevAssistant, userMsg));
        when(goalClassifier.classify(any())).thenReturn(
                new GoalClassification(GoalType.ANALYZE_CASE, Optional.of("case123"),
                        Optional.empty(), "follow-up: ANALYZE_CASE"));
        when(promptSupport.loadSkill(any())).thenReturn("skill body");
        when(promptSupport.buildPrompt(any(), any(), any())).thenReturn("prompt");
        when(chatAgentSystemTemplate.render(any())).thenReturn("system");
        when(chatUserMessageTemplate.render(any())).thenReturn("user prompt with history");
        when(chatCasePromptSupport.buildCaseToolHints(any(), any())).thenReturn("");
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.advisors(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("Here is more info");
        when(medicalAgentCriticService.review(any(), any()))
                .thenReturn(new MedicalAgentCriticService.CriticResult(true, "Here is more info", null, null));

        service.processMessage("c1", "user-a", "more", "auto");

        verify(chatService).getHistory("c1", "user-a", 6, 0);
    }
}
