package com.berdachuk.medexpertmatch.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Spring AI configuration for LLM and embedding models.
 * Supports OpenAI-compatible providers only (no Ollama native API).
 * Excluded in test profile to allow TestAIConfig to provide mocks.
 */
@Slf4j
@Configuration
@org.springframework.context.annotation.Profile("!test")
public class SpringAIConfig {

    private final Environment environment;
    private final LlmTierProperties tierProperties;

    public SpringAIConfig(Environment environment, LlmTierProperties tierProperties) {
        this.environment = environment;
        this.tierProperties = tierProperties;
        String[] activeProfiles = environment.getActiveProfiles();
        log.info("SpringAIConfig is being instantiated! Active profiles: {}", Arrays.toString(activeProfiles));
        if (Arrays.asList(activeProfiles).contains("test")) {
            throw new IllegalStateException("SpringAIConfig should NOT be active in test profile! This will create real LLM models!");
        }
    }

    @Bean
    @Primary
    @ConditionalOnProperty(
            name = "medexpertmatch.skills.enabled",
            havingValue = "false",
            matchIfMissing = false
    )
    public ChatClient chatClient(
            @Qualifier("clinicalChatModel") ChatModel clinicalChatModel,
            List<Advisor> advisors) {
        log.info("Configuring ChatClient with clinical ChatModel: {}", clinicalChatModel.getClass().getSimpleName());
        return chatClientBuilder(clinicalChatModel, advisors).build();
    }

    @Bean("caseAnalysisChatClient")
    public ChatClient caseAnalysisChatClient(
            @Qualifier("clinicalChatModel") ChatModel clinicalChatModel,
            List<Advisor> advisors) {
        log.info("Creating caseAnalysisChatClient with clinical LLM: {}", clinicalChatModel.getClass().getSimpleName());
        return chatClientBuilder(clinicalChatModel, advisors).build();
    }

    @Bean("utilityChatClient")
    public ChatClient utilityChatClient(
            @Qualifier("utilityChatModel") ChatModel utilityChatModel,
            List<Advisor> advisors) {
        log.info("Creating utilityChatClient");
        return chatClientBuilder(utilityChatModel, advisors).build();
    }

    @Bean("rerankingChatClient")
    @org.springframework.lang.Nullable
    public ChatClient rerankingChatClient(
            @Qualifier("rerankingChatModel") @org.springframework.lang.Nullable ChatModel rerankingChatModel,
            List<Advisor> advisors) {
        if (rerankingChatModel == null) {
            return null;
        }
        log.info("Creating rerankingChatClient");
        return chatClientBuilder(rerankingChatModel, advisors).build();
    }

    @Bean("clinicalChatModel")
    @Lazy
    public ChatModel clinicalChatModel() {
        LlmRoleEndpointResolver.ResolvedEndpoint endpoint = LlmRoleEndpointResolver.resolveClinical(environment);
        return OpenAiChatModelFactory.createWithMaxTokens(
                endpoint, "clinical", tierProperties.full().maxTokens());
    }

    @Bean("utilityChatModel")
    @Lazy
    public ChatModel utilityChatModel() {
        LlmRoleEndpointResolver.ResolvedEndpoint endpoint = LlmRoleEndpointResolver.resolveUtility(environment);
        return OpenAiChatModelFactory.createWithMaxTokens(
                endpoint, "utility", tierProperties.standard().maxTokens());
    }

    /**
     * Clinical model — used for case analysis, interpretation, and high-quality clinical reasoning.
     */
    @Bean("descriptionGenerationChatModel")
    @Lazy
    public ChatModel descriptionGenerationChatModel(@Qualifier("utilityChatModel") ChatModel utilityChatModel) {
        String descriptionMaxTokens = environment.getProperty(
                "medexpertmatch.synthetic-data.description.llm.max-tokens", "1024");
        try {
            int maxTokens = Integer.parseInt(descriptionMaxTokens);
            LlmRoleEndpointResolver.ResolvedEndpoint endpoint = LlmRoleEndpointResolver.resolveUtility(environment);
            return OpenAiChatModelFactory.createWithMaxTokens(endpoint, "description-generation", maxTokens);
        } catch (NumberFormatException e) {
            log.warn("Invalid description max-tokens: {}. Using utilityChatModel bean.", descriptionMaxTokens);
            return utilityChatModel;
        }
    }

    @Bean("descriptionGenerationChatClient")
    @Lazy
    public ChatClient descriptionGenerationChatClient(
            @Qualifier("descriptionGenerationChatModel") ChatModel descriptionGenerationChatModel) {
        log.info("Creating descriptionGenerationChatClient");
        return ChatClient.builder(descriptionGenerationChatModel).build();
    }

    private static ChatClient.Builder chatClientBuilder(ChatModel model, List<Advisor> advisors) {
        ChatClient.Builder builder = ChatClient.builder(model);
        advisors.stream()
                .sorted(Comparator.comparingInt(Advisor::getOrder))
                .forEach(builder::defaultAdvisors);
        return builder;
    }

    @Bean
    @Primary
    public EmbeddingModel primaryEmbeddingModel(List<EmbeddingModel> models) {
        String embeddingBaseUrl = environment.getProperty("spring.ai.custom.embedding.base-url");
        String embeddingProvider = environment.getProperty("spring.ai.custom.embedding.provider", "openai");
        String embeddingApiKey = environment.getProperty("spring.ai.custom.embedding.api-key");
        String embeddingModel = environment.getProperty("spring.ai.custom.embedding.model");
        String embeddingDimensions = environment.getProperty("spring.ai.custom.embedding.dimensions");

        if (embeddingBaseUrl != null && !embeddingBaseUrl.isEmpty()) {
            log.info("Creating custom EmbeddingModel with provider: {}, base URL: {}", embeddingProvider, embeddingBaseUrl);

            if (!"openai".equalsIgnoreCase(embeddingProvider)) {
                throw new IllegalArgumentException("Only OpenAI-compatible providers are supported. Provider: " + embeddingProvider);
            }

            String apiKey = embeddingApiKey != null && !embeddingApiKey.isEmpty() ? embeddingApiKey : "dummy-key";
            OpenAiEmbeddingOptions.Builder optionsBuilder = OpenAiEmbeddingOptions.builder()
                    .baseUrl(embeddingBaseUrl)
                    .apiKey(apiKey);
            if (embeddingModel != null && !embeddingModel.isEmpty()) {
                optionsBuilder.model(embeddingModel);
            }
            if (embeddingDimensions != null && !embeddingDimensions.isEmpty()) {
                try {
                    optionsBuilder.dimensions(Integer.parseInt(embeddingDimensions));
                } catch (NumberFormatException e) {
                    log.warn("Invalid embedding dimensions: {}. Using default.", embeddingDimensions);
                }
            }

            log.info("Creating OpenAiEmbeddingModel!");
            OpenAiEmbeddingModel model = new OpenAiEmbeddingModel(MetadataMode.EMBED, optionsBuilder.build());
            log.info("OpenAiEmbeddingModel created! Type: {}", model.getClass().getName());
            return model;
        }

        if (models.isEmpty()) {
            throw new IllegalStateException("No EmbeddingModel bean found. Please configure 'spring.ai.custom.embedding.*' properties.");
        }

        if (models.size() == 1) {
            return models.get(0);
        }

        EmbeddingModel selected = models.stream()
                .filter(m -> m.getClass().getSimpleName().toLowerCase().contains("openai"))
                .findFirst()
                .orElse(models.get(0));

        log.info("Multiple EmbeddingModel beans found: {}. Selected primary: {}",
                models.stream().map(m -> m.getClass().getSimpleName()).toList(),
                selected.getClass().getSimpleName());

        return selected;
    }

    @Bean
    @Qualifier("rerankingChatModel")
    public ChatModel rerankingChatModel() {
        try {
            LlmRoleEndpointResolver.ResolvedEndpoint endpoint = LlmRoleEndpointResolver.resolveReranking(environment);
            if (endpoint.model() == null || endpoint.model().isEmpty()) {
                log.info("Reranking model name not configured for role '{}'; semantic reranking may use placeholder.",
                        endpoint.role());
                return null;
            }
            return OpenAiChatModelFactory.create(endpoint, "reranking");
        } catch (IllegalStateException e) {
            log.info("Reranking ChatModel not configured. Semantic reranking will use placeholder implementation.");
            return null;
        }
    }

    @Bean
    @Qualifier("toolCallingChatModel")
    public ChatModel toolCallingChatModel(@Qualifier("clinicalChatModel") ChatModel clinicalChatModel) {
        String toolCallingBaseUrl = environment.getProperty("spring.ai.custom.tool-calling.base-url");
        String toolCallingProvider = environment.getProperty("spring.ai.custom.tool-calling.provider", "openai");
        String toolCallingModel = environment.getProperty("spring.ai.custom.tool-calling.model", "functiongemma");
        String toolCallingApiKey = environment.getProperty("spring.ai.custom.tool-calling.api-key");
        String toolCallingTemperature = environment.getProperty("spring.ai.custom.tool-calling.temperature", "0.7");
        String toolCallingMaxTokens = environment.getProperty("spring.ai.custom.tool-calling.max-tokens", "4096");

        if (toolCallingBaseUrl == null || toolCallingBaseUrl.isEmpty()) {
            toolCallingBaseUrl = environment.getProperty("spring.ai.custom.chat.base-url");
        }
        if (toolCallingApiKey == null || toolCallingApiKey.isEmpty()) {
            toolCallingApiKey = environment.getProperty("spring.ai.custom.chat.api-key");
        }

        if (toolCallingBaseUrl != null && !toolCallingBaseUrl.isEmpty()) {
            LlmRoleEndpointResolver.ResolvedEndpoint endpoint = new LlmRoleEndpointResolver.ResolvedEndpoint(
                    "tool-calling",
                    toolCallingProvider,
                    toolCallingBaseUrl,
                    toolCallingApiKey,
                    toolCallingModel,
                    toolCallingTemperature,
                    toolCallingMaxTokens);
            return OpenAiChatModelFactory.createWithMaxTokens(
                    endpoint, "tool-calling", tierProperties.light().maxTokens());
        }

        log.warn("Tool calling ChatModel not configured. Falling back to clinical ChatModel (may not support tools).");
        return clinicalChatModel;
    }
}
