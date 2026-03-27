package com.berdachuk.medexpertmatch.embedding.config;

import com.berdachuk.medexpertmatch.embedding.multiendpoint.EmbeddingEndpointPool;
import com.berdachuk.medexpertmatch.embedding.multiendpoint.EndpointState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Multi-endpoint embedding pool. Active when the first endpoint URL is set in configuration.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "medexpertmatch.embedding.multi-endpoint.endpoints[0].url")
public class EmbeddingEndpointPoolConfig {

    @Bean
    public EmbeddingEndpointPool embeddingEndpointPool(
            MultiEndpointEmbeddingProperties properties,
            Environment environment) {

        String embeddingApiKey = environment.getProperty("spring.ai.custom.embedding.api-key", "");
        String embeddingDimensions = environment.getProperty("spring.ai.custom.embedding.dimensions", "768");

        List<MultiEndpointEmbeddingProperties.EndpointConfig> sortedEndpoints = new ArrayList<>(properties.getEndpoints());
        sortedEndpoints.sort(Comparator.comparingInt(MultiEndpointEmbeddingProperties.EndpointConfig::getPriority));

        List<EndpointState> endpointStates = new ArrayList<>();
        List<Integer> workersPerEndpoint = new ArrayList<>();
        for (MultiEndpointEmbeddingProperties.EndpointConfig ep : sortedEndpoints) {
            if (ep.getUrl() == null || ep.getUrl().isBlank()) {
                log.warn("Skipping endpoint with empty URL");
                continue;
            }

            OpenAiApi api = OpenAiApi.builder()
                    .baseUrl(ep.getUrl())
                    .apiKey(embeddingApiKey != null ? embeddingApiKey : "")
                    .build();

            OpenAiEmbeddingOptions.Builder optionsBuilder = OpenAiEmbeddingOptions.builder();
            if (ep.getModel() != null && !ep.getModel().isBlank()) {
                optionsBuilder.model(ep.getModel());
            }
            if (embeddingDimensions != null && !embeddingDimensions.isBlank()) {
                try {
                    optionsBuilder.dimensions(Integer.parseInt(embeddingDimensions));
                } catch (NumberFormatException e) {
                    log.warn("Invalid embedding dimensions: {}. Using default.", embeddingDimensions);
                }
            }

            OpenAiEmbeddingModel model = new OpenAiEmbeddingModel(
                    api,
                    MetadataMode.EMBED,
                    optionsBuilder.build());

            EndpointState state = new EndpointState(ep.getUrl(), ep.getModel(), model);
            endpointStates.add(state);
            int workers = ep.getWorkers() != null ? ep.getWorkers() : properties.getWorkerPerEndpoint();
            workersPerEndpoint.add(Math.max(1, workers));
            log.info("Multi-endpoint: added embedding endpoint {} with model {} (priority={}, workers={})",
                    ep.getUrl(), ep.getModel(), ep.getPriority(), workers);
        }

        if (endpointStates.isEmpty()) {
            throw new IllegalStateException(
                    "Multi-endpoint embedding enabled but no valid endpoints configured. "
                            + "Configure at least one endpoint in medexpertmatch.embedding.multi-endpoint.endpoints");
        }

        return new EmbeddingEndpointPool(
                endpointStates,
                workersPerEndpoint,
                properties.getSkipDurationMin(),
                properties.getApiBatchSize());
    }
}
