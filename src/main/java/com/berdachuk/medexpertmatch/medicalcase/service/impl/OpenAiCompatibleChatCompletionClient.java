package com.berdachuk.medexpertmatch.medicalcase.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.berdachuk.medexpertmatch.medicalcase.service.ChatCompletionTextClient;
import com.berdachuk.medexpertmatch.medicalcase.service.support.OpenAiAssistantResponseParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Raw /v1/chat/completions client: merges {@code message.content}, array content parts,
 * and {@code reasoning_content} when Spring AI's ChatClient maps only empty {@code content}.
 */
@Slf4j
@Component
public class OpenAiCompatibleChatCompletionClient implements ChatCompletionTextClient {

    private final Environment environment;
    private final ObjectMapper objectMapper;
    private final int readTimeoutSeconds;
    private final AtomicReference<RestClient> restClientRef = new AtomicReference<>();

    public OpenAiCompatibleChatCompletionClient(
            Environment environment,
            ObjectMapper objectMapper,
            @Value("${medexpertmatch.synthetic-data.llm.timeout-seconds:300}") int readTimeoutSeconds) {
        this.environment = environment;
        this.objectMapper = objectMapper;
        this.readTimeoutSeconds = readTimeoutSeconds;
    }

    @Override
    public Optional<String> complete(String systemPrompt, String userPrompt) {
        String baseUrl = environment.getProperty("spring.ai.custom.chat.base-url");
        if (!StringUtils.hasText(baseUrl)) {
            return Optional.empty();
        }
        String model = environment.getProperty("spring.ai.custom.chat.model", "");
        String apiKey = environment.getProperty("spring.ai.custom.chat.api-key", "");
        double temperature = parseDouble(environment.getProperty("spring.ai.custom.chat.temperature"), 0.7);
        int maxTokens = parseInt(
                environment.getProperty("medexpertmatch.synthetic-data.description.llm.max-tokens"), 1024);

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("temperature", temperature);
            body.put("max_tokens", maxTokens);
            body.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)));

            String jsonRequest = objectMapper.writeValueAsString(body);
            RestClient client = restClient(baseUrl.trim());
            String jsonResponse = client.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> addAuth(h, apiKey))
                    .body(jsonRequest)
                    .retrieve()
                    .body(String.class);

            if (!StringUtils.hasText(jsonResponse)) {
                return Optional.empty();
            }
            String text = OpenAiAssistantResponseParser.assistantTextFromJson(jsonResponse, objectMapper);
            if (StringUtils.hasText(text)) {
                log.debug("OpenAI-compatible completion fallback returned {} chars", text.length());
                return Optional.of(text);
            }
        } catch (Exception e) {
            log.warn("OpenAI-compatible completion fallback failed: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private RestClient restClient(String baseUrl) {
        return restClientRef.updateAndGet(existing -> {
            if (existing != null) {
                return existing;
            }
            String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            int readSec = Math.max(60, readTimeoutSeconds);
            JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
            factory.setReadTimeout(Duration.ofSeconds(readSec));
            return RestClient.builder()
                    .baseUrl(normalized)
                    .requestFactory(factory)
                    .build();
        });
    }

    private static void addAuth(HttpHeaders headers, String apiKey) {
        if (StringUtils.hasText(apiKey)) {
            headers.setBearerAuth(apiKey);
        }
    }

    private static double parseDouble(String value, double defaultVal) {
        if (!StringUtils.hasText(value)) {
            return defaultVal;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private static int parseInt(String value, int defaultVal) {
        if (!StringUtils.hasText(value)) {
            return defaultVal;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
