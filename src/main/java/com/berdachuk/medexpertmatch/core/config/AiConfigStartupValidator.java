package com.berdachuk.medexpertmatch.core.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
public class AiConfigStartupValidator {

    private final Environment environment;

    public AiConfigStartupValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validate() {
        validateRequiredProperty("spring.ai.custom.chat.base-url", "${CHAT_BASE_URL}");
        validateRequiredProperty("spring.ai.custom.embedding.base-url", "${EMBEDDING_BASE_URL}");

        logRequiredProperty("spring.ai.custom.chat.model", "${CHAT_MODEL}");
        logRequiredProperty("spring.ai.custom.embedding.model", "${EMBEDDING_MODEL}");

        log.info("AI configuration startup validation passed");
    }

    private void validateRequiredProperty(String key, String envVar) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) {
            String error = String.format(
                    "Required AI configuration property '%s' is not set. " +
                    "Set environment variable %s or configure it in application.yml", key, envVar);
            log.error(error);
            throw new IllegalStateException(error);
        }
        log.info("AI config property '{}' = {}", key, maskSensitive(value));
    }

    private void logRequiredProperty(String key, String envVar) {
        String value = environment.getProperty(key);
        if (value != null && !value.isBlank()) {
            log.info("AI config property '{}' = {}", key, value);
        } else {
            log.warn("AI config property '{}' is not set. Default will apply. Set {} to override.", key, envVar);
        }
    }

    private static String maskSensitive(String value) {
        if (value == null) return null;
        if (value.length() <= 8) return "***";
        return value.substring(0, 4) + "..." + value.substring(value.length() - 4);
    }
}
