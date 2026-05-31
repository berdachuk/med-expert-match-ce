package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.core.util.LlmCallLimiter;
import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentLlmSupportService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentPromptSupportService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that a conversation-holding medical agent workflow (recommendation) threads the
 * session id into the {@link SessionMemoryAdvisor} at call time via
 * {@link SessionMemoryAdvisor#SESSION_ID_CONTEXT_KEY}.
 * <p>
 * Pure unit test: the {@link ChatClient} fluent chain is mocked (mirroring the project's
 * AI-provider mocking convention) so no LLM / database is touched. Patient data is anonymized.
 */
class MedicalAgentRecommendationWorkflowSessionTest {

    @Test
    @DisplayName("Recommendation workflow sets SESSION_ID_CONTEXT_KEY = request sessionId on the advisor")
    void threadsSessionIdThroughAdvisor() {
        // --- mock the ChatClient fluent chain and capture the advisor consumer ---
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        ChatClient.AdvisorSpec advisorSpec = mock(ChatClient.AdvisorSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("tool results");

        // --- collaborators ---
        LlmCallLimiter llmCallLimiter = mock(LlmCallLimiter.class);
        // execute(Supplier) must actually run the supplier so the ChatClient chain (and the
        // advisor consumer) is exercised.
        when(llmCallLimiter.execute(eq(LlmClientType.TOOL_CALLING), any(Supplier.class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(1)).get());

        MedicalAgentPromptSupportService promptSupport = mock(MedicalAgentPromptSupportService.class);
        when(promptSupport.loadSkill(anyString())).thenReturn("doctor-matcher-skill");
        when(promptSupport.buildPrompt(any(), anyString(), any())).thenReturn("PROMPT");

        MedicalAgentLlmSupportService llmSupport = mock(MedicalAgentLlmSupportService.class);
        when(llmSupport.analyzeCaseWithMedGemma(anyString())).thenReturn("{\"urgencyLevel\":\"HIGH\"}");
        when(llmSupport.interpretResultsWithMedGemma(anyString(), anyString(), any()))
                .thenReturn("Final anonymized recommendation");

        MedicalCaseRepository medicalCaseRepository = mock(MedicalCaseRepository.class);
        when(medicalCaseRepository.findById(anyString())).thenReturn(Optional.empty());

        LogStreamService logStreamService = mock(LogStreamService.class);

        MedicalAgentRecommendationWorkflowServiceImpl service = new MedicalAgentRecommendationWorkflowServiceImpl(
                chatClient,
                "functiongemma",
                medicalCaseRepository,
                llmSupport,
                promptSupport,
                logStreamService,
                llmCallLimiter);

        String sessionId = "sess-123";
        MedicalAgentService.AgentResponse response = service.generateRecommendations(
                "match-anon-1",
                Map.of("caseId", "case-anon-1", "sessionId", sessionId));

        assertNotNull(response);

        // --- capture and replay the advisor consumer, asserting the session id param ---
        ArgumentCaptor<Consumer<ChatClient.AdvisorSpec>> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(requestSpec, times(1)).advisors(captor.capture());

        when(advisorSpec.param(anyString(), any())).thenReturn(advisorSpec);
        captor.getValue().accept(advisorSpec);

        verify(advisorSpec).param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, sessionId);
        assertEquals("Final anonymized recommendation", response.response());
    }

    @Test
    @DisplayName("Absent sessionId auto-defaults so a session can be created downstream")
    void autoDefaultsSessionIdWhenAbsent() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        ChatClient.AdvisorSpec advisorSpec = mock(ChatClient.AdvisorSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("tool results");

        LlmCallLimiter llmCallLimiter = mock(LlmCallLimiter.class);
        when(llmCallLimiter.execute(eq(LlmClientType.TOOL_CALLING), any(Supplier.class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(1)).get());

        MedicalAgentPromptSupportService promptSupport = mock(MedicalAgentPromptSupportService.class);
        when(promptSupport.loadSkill(anyString())).thenReturn("doctor-matcher-skill");
        when(promptSupport.buildPrompt(any(), anyString(), any())).thenReturn("PROMPT");

        MedicalAgentLlmSupportService llmSupport = mock(MedicalAgentLlmSupportService.class);
        when(llmSupport.analyzeCaseWithMedGemma(anyString())).thenReturn("{}");
        when(llmSupport.interpretResultsWithMedGemma(anyString(), anyString(), any())).thenReturn("ok");

        MedicalCaseRepository medicalCaseRepository = mock(MedicalCaseRepository.class);
        when(medicalCaseRepository.findById(anyString())).thenReturn(Optional.empty());

        MedicalAgentRecommendationWorkflowServiceImpl service = new MedicalAgentRecommendationWorkflowServiceImpl(
                chatClient,
                "functiongemma",
                medicalCaseRepository,
                llmSupport,
                promptSupport,
                mock(LogStreamService.class),
                llmCallLimiter);

        service.generateRecommendations("match-anon-2",
                Map.of("caseId", "case-anon-2")); // no sessionId

        ArgumentCaptor<Consumer<ChatClient.AdvisorSpec>> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(requestSpec).advisors(captor.capture());

        when(advisorSpec.param(anyString(), any())).thenReturn(advisorSpec);
        captor.getValue().accept(advisorSpec);

        // A non-null session id must be threaded so the advisor auto-creates a session.
        verify(advisorSpec).param(eq(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY),
                org.mockito.ArgumentMatchers.argThat(v -> v != null && !v.toString().isBlank()));
    }
}
