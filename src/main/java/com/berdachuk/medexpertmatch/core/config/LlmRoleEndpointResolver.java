package com.berdachuk.medexpertmatch.core.config;

import org.springframework.core.env.Environment;

/**
 * Resolves OpenAI-compatible LLM endpoint settings per role with explicit fallback chains (M67).
 */
public final class LlmRoleEndpointResolver {

    private static final String CUSTOM_PREFIX = "spring.ai.custom.";

    private LlmRoleEndpointResolver() {
    }

    public static ResolvedEndpoint resolveClinical(Environment environment) {
        return firstResolvable(environment, "clinical", "chat");
    }

    public static ResolvedEndpoint resolveUtility(Environment environment) {
        return firstResolvable(environment, "utility", "reranking", "chat");
    }

    public static ResolvedEndpoint resolveReranking(Environment environment) {
        return firstResolvable(environment, "reranking", "utility", "chat");
    }

    private static ResolvedEndpoint firstResolvable(Environment environment, String... roles) {
        for (String role : roles) {
            String baseUrl = environment.getProperty(CUSTOM_PREFIX + role + ".base-url");
            if (isPresent(baseUrl)) {
                return load(environment, role, baseUrl);
            }
        }
        throw new IllegalStateException(
                "No LLM base URL configured for roles " + String.join(" → ", roles)
                        + ". Set CHAT_BASE_URL or role-specific CLINICAL_/UTILITY_/RERANKING_ variables.");
    }

    private static ResolvedEndpoint load(Environment environment, String role, String baseUrl) {
        String provider = environment.getProperty(CUSTOM_PREFIX + role + ".provider", "openai");
        String apiKey = environment.getProperty(CUSTOM_PREFIX + role + ".api-key");
        String model = environment.getProperty(CUSTOM_PREFIX + role + ".model");
        String temperature = environment.getProperty(CUSTOM_PREFIX + role + ".temperature");
        String maxTokens = environment.getProperty(CUSTOM_PREFIX + role + ".max-tokens");
        return new ResolvedEndpoint(role, provider, baseUrl, apiKey, model, temperature, maxTokens);
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    public record ResolvedEndpoint(
            String role,
            String provider,
            String baseUrl,
            String apiKey,
            String model,
            String temperature,
            String maxTokens) {
    }
}
