package com.berdachuk.medexpertmatch.core.util;

import com.berdachuk.medexpertmatch.core.config.LlmRoleEndpointResolver;
import com.berdachuk.medexpertmatch.core.config.LlmStructuredOutputProperties;
import com.berdachuk.medexpertmatch.core.monitoring.StructuredOutputValidationMetrics;
import com.berdachuk.medexpertmatch.core.monitoring.StructuredOutputValidationTracker;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.core.env.Environment;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Shared wrapper for structured {@code ChatClient.entity(..., validateSchema())} calls (M140).
 */
public final class StructuredOutputSupport {

    private StructuredOutputSupport() {
    }

    public static Consumer<ChatClient.EntityParamSpec> entitySpec(
            LlmStructuredOutputProperties properties,
            Environment environment,
            LlmClientType clientType) {
        boolean providerNative = providerNativeAllowed(properties, environment, clientType);
        return spec -> {
            if (providerNative) {
                spec.useProviderStructuredOutput();
            }
            spec.validateSchema();
        };
    }

    public static <T> T execute(
            LlmUsageContext usageContext,
            LlmCallLimiter limiter,
            StructuredOutputValidationMetrics metrics,
            Supplier<T> supplier) {
        return LlmUsageContextRunner.execute(usageContext, () ->
                limiter.execute(usageContext.clientType(), () -> {
                    StructuredOutputValidationTracker.begin(usageContext, metrics);
                    try {
                        return supplier.get();
                    } catch (RuntimeException e) {
                        if (metrics != null) {
                            metrics.recordFailure(usageContext.operation(), usageContext.clientType());
                        }
                        throw e;
                    } finally {
                        StructuredOutputValidationTracker.end();
                    }
                }));
    }

    public static <T> T callEntity(
            ChatClient chatClient,
            LlmUsageContext usageContext,
            LlmCallLimiter limiter,
            LlmStructuredOutputProperties properties,
            Environment environment,
            StructuredOutputValidationMetrics metrics,
            Consumer<ChatClient.ChatClientRequestSpec> promptSpec,
            Class<T> type) {
        return execute(usageContext, limiter, metrics, () -> {
            ChatClient.ChatClientRequestSpec spec = chatClient.prompt();
            promptSpec.accept(spec);
            return spec.call().entity(type, entitySpec(properties, environment, usageContext.clientType()));
        });
    }

    public static <T> T callEntity(
            ChatClient chatClient,
            LlmUsageContext usageContext,
            LlmCallLimiter limiter,
            LlmStructuredOutputProperties properties,
            Environment environment,
            StructuredOutputValidationMetrics metrics,
            Consumer<ChatClient.ChatClientRequestSpec> promptSpec,
            StructuredOutputConverter<T> converter) {
        return execute(usageContext, limiter, metrics, () -> {
            ChatClient.ChatClientRequestSpec spec = chatClient.prompt();
            promptSpec.accept(spec);
            return spec.call().entity(converter, entitySpec(properties, environment, usageContext.clientType()));
        });
    }

    static boolean providerNativeAllowed(
            LlmStructuredOutputProperties properties,
            Environment environment,
            LlmClientType clientType) {
        if (properties == null || !properties.providerNativeEnabled()) {
            return false;
        }
        LlmRoleEndpointResolver.ResolvedEndpoint endpoint = switch (clientType) {
            case CLINICAL -> LlmRoleEndpointResolver.resolveClinical(environment);
            case UTILITY -> LlmRoleEndpointResolver.resolveUtility(environment);
            default -> LlmRoleEndpointResolver.resolveClinical(environment);
        };
        String url = endpoint.baseUrl() != null ? endpoint.baseUrl().toLowerCase() : "";
        return !(url.contains("localhost") || url.contains("127.0.0.1") || url.contains(":11434"));
    }
}
