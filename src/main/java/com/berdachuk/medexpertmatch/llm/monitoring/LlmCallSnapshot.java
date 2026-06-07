package com.berdachuk.medexpertmatch.llm.monitoring;

import com.berdachuk.medexpertmatch.core.util.LlmCacheSource;
import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import com.berdachuk.medexpertmatch.core.util.LlmOperation;
import com.berdachuk.medexpertmatch.core.util.LlmUsageContext;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.lang.Nullable;

/**
 * Immutable LLM call metadata (no prompt or response text).
 */
public record LlmCallSnapshot(
        @Nullable String sessionId,
        LlmClientType clientType,
        LlmOperation operation,
        @Nullable String routingTier,
        @Nullable String goalType,
        @Nullable String model,
        @Nullable Integer promptTokens,
        @Nullable Integer completionTokens,
        @Nullable Long cacheReadTokens,
        @Nullable Long cacheWriteTokens,
        @Nullable String finishReason,
        long latencyMs,
        int promptChars,
        int messageCount,
        @Nullable Integer maxTokensBudget,
        LlmCacheSource cacheSource,
        boolean cacheHit) {

    public static LlmCallSnapshot fromProvider(
            ChatClientResponse response,
            ChatClientRequest request,
            LlmUsageContext context,
            long latencyMs) {
        ChatResponse chatResponse = response != null ? response.chatResponse() : null;
        ChatResponseMetadata metadata = chatResponse != null ? chatResponse.getMetadata() : null;
        Usage usage = metadata != null ? metadata.getUsage() : null;
        String model = metadata != null ? metadata.getModel() : null;
        String finishReason = null;
        if (chatResponse != null && chatResponse.getResult() != null) {
            Generation generation = chatResponse.getResult();
            if (generation.getMetadata() != null) {
                finishReason = generation.getMetadata().getFinishReason();
            }
        }
        return new LlmCallSnapshot(
                context.sessionId(),
                context.clientType(),
                context.operation(),
                context.routingTier(),
                context.goalType(),
                model,
                usage != null ? usage.getPromptTokens() : null,
                usage != null ? usage.getCompletionTokens() : null,
                usage != null ? usage.getCacheReadInputTokens() : null,
                usage != null ? usage.getCacheWriteInputTokens() : null,
                finishReason,
                latencyMs,
                promptCharCount(request),
                messageCount(request),
                context.maxTokensBudget(),
                LlmCacheSource.NONE,
                false);
    }

    public static LlmCallSnapshot fromCacheHit(LlmUsageContext context, String cacheKey) {
        LlmUsageContext ctx = context != null ? context : defaultContext(cacheKey);
        return new LlmCallSnapshot(
                ctx.sessionId(),
                ctx.clientType(),
                ctx.operation(),
                ctx.routingTier(),
                ctx.goalType(),
                null,
                0,
                0,
                null,
                null,
                null,
                0L,
                0,
                0,
                ctx.maxTokensBudget(),
                LlmCacheSource.LLM_RESPONSES_CACHE,
                true);
    }

    public static LlmOperation operationFromCacheKey(String cacheKey) {
        if (cacheKey == null || cacheKey.isBlank()) {
            return LlmOperation.OTHER;
        }
        if (cacheKey.startsWith("analyze:")) {
            return LlmOperation.CASE_ANALYSIS;
        }
        if (cacheKey.startsWith("interpret:match:")) {
            return LlmOperation.MATCH_INTERPRET;
        }
        if (cacheKey.startsWith("interpret:case:")) {
            return LlmOperation.CASE_INTERPRET;
        }
        if (cacheKey.startsWith("routing:")) {
            return LlmOperation.ROUTING_SUMMARIZE;
        }
        if (cacheKey.startsWith("network:")) {
            return LlmOperation.NETWORK_SUMMARIZE;
        }
        return LlmOperation.OTHER;
    }

    public String formatCompactMessage() {
        StringBuilder sb = new StringBuilder("LLM · ")
                .append(clientType.name());
        if (cacheHit) {
            sb.append(" · (cached) · ").append(operation.uiLabel());
            sb.append(" · 0 tok · 0ms");
            return sb.toString();
        }
        if (model != null && !model.isBlank()) {
            sb.append(" · ").append(model);
        }
        sb.append(" · ").append(formatTokenPair());
        if (cacheReadTokens != null && cacheReadTokens > 0) {
            sb.append(" · cache ").append(formatCount(cacheReadTokens));
        }
        sb.append(" · ").append(formatLatency(latencyMs));
        return sb.toString();
    }

    public String compactMessage() {
        return formatCompactMessage();
    }

    public String formatHarnessDetails() {
        StringBuilder sb = new StringBuilder();
        if (promptTokens != null) {
            sb.append("in=").append(promptTokens);
        } else {
            sb.append("in=—");
        }
        if (completionTokens != null) {
            sb.append(" out=").append(completionTokens);
        } else {
            sb.append(" out=—");
        }
        if (cacheReadTokens != null && cacheReadTokens > 0) {
            sb.append(" cache_read=").append(cacheReadTokens);
        }
        sb.append(" latency=").append(latencyMs).append("ms");
        if (promptChars > 0) {
            sb.append(" prompt_chars=").append(promptChars);
        }
        if (cacheHit) {
            sb.append(" cache_hit=true");
        }
        return sb.toString();
    }

    private String formatTokenPair() {
        String in = promptTokens != null ? formatCount(promptTokens) : "—";
        String out = completionTokens != null ? formatCount(completionTokens) : "—";
        return in + "→" + out + " tok";
    }

    private static String formatCount(long value) {
        if (value >= 1000) {
            return String.format("%.1fk", value / 1000.0);
        }
        return Long.toString(value);
    }

    private static String formatLatency(long latencyMs) {
        if (latencyMs >= 1000) {
            return String.format("%.1fs", latencyMs / 1000.0);
        }
        return latencyMs + "ms";
    }

    private static int promptCharCount(ChatClientRequest request) {
        if (request == null || request.prompt() == null) {
            return 0;
        }
        int total = 0;
        for (Message message : request.prompt().getInstructions()) {
            if (message.getText() != null) {
                total += message.getText().length();
            }
        }
        return total;
    }

    private static int messageCount(ChatClientRequest request) {
        if (request == null || request.prompt() == null) {
            return 0;
        }
        return request.prompt().getInstructions().size();
    }

    private static LlmUsageContext defaultContext(String cacheKey) {
        return new LlmUsageContext(
                "default", LlmClientType.CLINICAL, operationFromCacheKey(cacheKey), null, null, null);
    }
}
