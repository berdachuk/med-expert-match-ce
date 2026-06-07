package com.berdachuk.medexpertmatch.llm.advisor;

import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import com.berdachuk.medexpertmatch.core.util.LlmOperation;
import com.berdachuk.medexpertmatch.core.util.LlmUsageContext;
import com.berdachuk.medexpertmatch.core.util.LlmUsageContextHolder;
import com.berdachuk.medexpertmatch.llm.monitoring.LlmUsageTelemetryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LlmUsageCaptureAdvisorTest {

    private final LlmUsageTelemetryService telemetryService = mock(LlmUsageTelemetryService.class);
    private final LlmUsageCaptureAdvisor advisor = new LlmUsageCaptureAdvisor(telemetryService);

    @AfterEach
    void clearContext() {
        LlmUsageContextHolder.clear();
    }

    @Test
    @DisplayName("records snapshot with usage tokens and prompt char count")
    void recordsProviderUsage() {
        LlmUsageContextHolder.set(new LlmUsageContext("sess-1", LlmClientType.CLINICAL,
                LlmOperation.CASE_ANALYSIS, "FULL", "MATCH_DOCTORS", 6000));

        ChatClientRequest request = ChatClientRequest.builder()
                .prompt(new Prompt(List.of(new UserMessage("analyze case"))))
                .build();

        ChatResponseMetadata metadata = ChatResponseMetadata.builder()
                .model("medgemma:1.5-4b")
                .usage(new DefaultUsage(100, 50, 150, null, 40L, 0L))
                .build();
        ChatResponse chatResponse = ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage("ok"))))
                .metadata(metadata)
                .build();
        ChatClientResponse response = new ChatClientResponse(chatResponse, Map.of());

        advisor.adviseCall(request, chainReturning(response));

        verify(telemetryService).record(argThat(snapshot ->
                snapshot.promptTokens() == 100
                        && snapshot.completionTokens() == 50
                        && snapshot.cacheReadTokens() == 40L
                        && snapshot.promptChars() == "analyze case".length()
                        && snapshot.clientType() == LlmClientType.CLINICAL
                        && snapshot.operation() == LlmOperation.CASE_ANALYSIS));
    }

    @Test
    @DisplayName("advisor order is lowest precedence")
    void runsClosestToProvider() {
        assertEquals(org.springframework.core.Ordered.LOWEST_PRECEDENCE, advisor.getOrder());
    }

    private CallAdvisorChain chainReturning(ChatClientResponse response) {
        return new CallAdvisorChain() {
            @Override
            public ChatClientResponse nextCall(ChatClientRequest augmented) {
                return response;
            }

            @Override
            public List<CallAdvisor> getCallAdvisors() {
                return Collections.emptyList();
            }

            @Override
            public CallAdvisorChain copy(CallAdvisor advisorToCopy) {
                return this;
            }
        };
    }
}
