package com.berdachuk.medexpertmatch.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;

/**
 * Builds OpenAI-compatible {@link ChatModel} beans from resolved role endpoint settings.
 */
@Slf4j
public final class OpenAiChatModelFactory {

    private OpenAiChatModelFactory() {
    }

    public static ChatModel create(LlmRoleEndpointResolver.ResolvedEndpoint endpoint, String purpose) {
        if (!"openai".equalsIgnoreCase(endpoint.provider())) {
            throw new IllegalArgumentException(
                    "Only OpenAI-compatible providers are supported. Role: " + endpoint.role()
                            + ", provider: " + endpoint.provider());
        }

        String apiKey = endpoint.apiKey() != null && !endpoint.apiKey().isEmpty()
                ? endpoint.apiKey()
                : "dummy-key";

        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .baseUrl(endpoint.baseUrl())
                .apiKey(apiKey);

        if (endpoint.model() != null && !endpoint.model().isEmpty()) {
            optionsBuilder.model(endpoint.model());
        }
        applyDoubleOption(endpoint.temperature(), optionsBuilder::temperature, "temperature");
        applyIntOption(endpoint.maxTokens(), optionsBuilder::maxTokens, "max-tokens");

        log.info("Creating {} ChatModel from role '{}' at {}", purpose, endpoint.role(), endpoint.baseUrl());
        return OpenAiChatModel.builder().options(optionsBuilder.build()).build();
    }

    public static ChatModel createWithMaxTokens(
            LlmRoleEndpointResolver.ResolvedEndpoint endpoint,
            String purpose,
            int maxTokensOverride) {
        LlmRoleEndpointResolver.ResolvedEndpoint overridden = new LlmRoleEndpointResolver.ResolvedEndpoint(
                endpoint.role(),
                endpoint.provider(),
                endpoint.baseUrl(),
                endpoint.apiKey(),
                endpoint.model(),
                endpoint.temperature(),
                String.valueOf(maxTokensOverride));
        return create(overridden, purpose);
    }

    private static void applyDoubleOption(String raw, java.util.function.Consumer<Double> setter, String label) {
        if (raw == null || raw.isEmpty()) {
            return;
        }
        try {
            setter.accept(Double.parseDouble(raw));
        } catch (NumberFormatException e) {
            log.warn("Invalid {} for role '{}': {}", label, "endpoint", raw);
        }
    }

    private static void applyIntOption(String raw, java.util.function.IntConsumer setter, String label) {
        if (raw == null || raw.isEmpty()) {
            return;
        }
        try {
            setter.accept(Integer.parseInt(raw));
        } catch (NumberFormatException e) {
            log.warn("Invalid {} value: {}", label, raw);
        }
    }
}
