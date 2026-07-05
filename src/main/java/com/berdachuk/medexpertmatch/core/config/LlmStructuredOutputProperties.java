package com.berdachuk.medexpertmatch.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Structured LLM output settings (M140).
 */
@Validated
@ConfigurationProperties(prefix = "medexpertmatch.llm.structured-output")
public record LlmStructuredOutputProperties(boolean providerNativeEnabled) {

    public LlmStructuredOutputProperties {
        // default false when property absent
    }
}
