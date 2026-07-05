package com.berdachuk.medexpertmatch.llm.advisor;

import com.berdachuk.medexpertmatch.core.util.LlmUsageContextHolder;
import com.berdachuk.medexpertmatch.llm.monitoring.LlmCallSnapshot;
import com.berdachuk.medexpertmatch.llm.monitoring.LlmUsageTelemetryService;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicReference;

public class LlmUsageCaptureAdvisor implements CallAdvisor, StreamAdvisor {

    private final LlmUsageTelemetryService telemetryService;

    public LlmUsageCaptureAdvisor(LlmUsageTelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @Override
    public String getName() {
        return "llmUsageCaptureAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        long start = System.nanoTime();
        ChatClientResponse response = chain.nextCall(request);
        recordSnapshot(request, response, start);
        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        long start = System.nanoTime();
        AtomicReference<ChatClientResponse> lastResponse = new AtomicReference<>();
        return chain.nextStream(request)
                .doOnNext(lastResponse::set)
                .doOnComplete(() -> recordSnapshot(request, lastResponse.get(), start));
    }

    private void recordSnapshot(ChatClientRequest request, ChatClientResponse response, long startNanos) {
        if (response == null) {
            return;
        }
        com.berdachuk.medexpertmatch.core.monitoring.StructuredOutputValidationTracker.onProviderCall();
        long latencyMs = Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
        LlmCallSnapshot snapshot = LlmCallSnapshot.fromProvider(
                response, request, LlmUsageContextHolder.getOrDefault(), latencyMs);
        telemetryService.record(snapshot);
    }
}
