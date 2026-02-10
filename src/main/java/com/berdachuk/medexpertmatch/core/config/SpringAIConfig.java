package com.berdachuk.medexpertmatch.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import java.util.Arrays;
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
    private final ListableBeanFactory beanFactory;

    public SpringAIConfig(Environment environment, ListableBeanFactory beanFactory) {
        this.environment = environment;
        this.beanFactory = beanFactory;
        String[] activeProfiles = environment.getActiveProfiles();
        log.info("SpringAIConfig is being instantiated! Active profiles: {}", Arrays.toString(activeProfiles));
        if (Arrays.asList(activeProfiles).contains("test")) {
            throw new IllegalStateException("SpringAIConfig should NOT be active in test profile! This will create real LLM models!");
        }
    }

    /**
     * Chat client configuration.
     * Uses the primary ChatModel for chat operations.
     * Only created when skills are disabled (when skills are enabled, MedicalAgentConfiguration creates the ChatClient).
     */
    @Bean
    @Primary
    @ConditionalOnProperty(
            name = "medexpertmatch.skills.enabled",
            havingValue = "false",
            matchIfMissing = false
    )
    public ChatClient chatClient(@Qualifier("primaryChatModel") ChatModel primaryChatModel) {
        log.info("Configuring ChatClient with @Primary ChatModel: {}", primaryChatModel.getClass().getSimpleName());
        return ChatClient.builder(primaryChatModel).build();
    }

    /**
     * Dedicated ChatClient for CaseAnalysisService.
     * Always uses MedGemma (primaryChatModel) regardless of skills configuration.
     * This ensures medical case analysis always uses MedGemma's medical domain expertise.
     */
    @Bean("caseAnalysisChatClient")
    public ChatClient caseAnalysisChatClient(@Qualifier("primaryChatModel") ChatModel primaryChatModel) {
        log.info("Creating caseAnalysisChatClient with MedGemma (primaryChatModel): {}", primaryChatModel.getClass().getSimpleName());
        return ChatClient.builder(primaryChatModel).build();
    }

    /**
     * ChatModel for synthetic data description generation.
     * Uses same endpoint as primary chat but with lower max_tokens (default 1024) for short case abstracts.
     */
    @Bean("descriptionGenerationChatModel")
    @Lazy
    public ChatModel descriptionGenerationChatModel(@Qualifier("primaryChatModel") ChatModel primaryChatModel) {
        String chatBaseUrl = environment.getProperty("spring.ai.custom.chat.base-url");
        if (chatBaseUrl == null || chatBaseUrl.isEmpty()) {
            log.info("No custom chat base URL; description generation uses primaryChatModel");
            return primaryChatModel;
        }
        String chatProvider = environment.getProperty("spring.ai.custom.chat.provider", "openai");
        String chatApiKey = environment.getProperty("spring.ai.custom.chat.api-key");
        String chatModelName = environment.getProperty("spring.ai.custom.chat.model");
        String chatTemperature = environment.getProperty("spring.ai.custom.chat.temperature");
        String descriptionMaxTokens = environment.getProperty("medexpertmatch.synthetic-data.description.llm.max-tokens", "1024");

        if (!"openai".equalsIgnoreCase(chatProvider)) {
            return primaryChatModel;
        }
        OpenAiApi chatApi = OpenAiApi.builder()
                .baseUrl(chatBaseUrl)
                .apiKey(chatApiKey != null && !chatApiKey.isEmpty() ? chatApiKey : "dummy-key")
                .build();
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder();
        if (chatModelName != null && !chatModelName.isEmpty()) {
            optionsBuilder.model(chatModelName);
        }
        if (chatTemperature != null && !chatTemperature.isEmpty()) {
            try {
                optionsBuilder.temperature(Double.parseDouble(chatTemperature));
            } catch (NumberFormatException e) {
                log.warn("Invalid chat temperature for description model. Using default.");
            }
        }
        try {
            optionsBuilder.maxTokens(Integer.parseInt(descriptionMaxTokens));
        } catch (NumberFormatException e) {
            log.warn("Invalid description max-tokens: {}. Using 1024.", descriptionMaxTokens);
            optionsBuilder.maxTokens(1024);
        }
        log.info("Creating descriptionGenerationChatModel with max_tokens: {}", descriptionMaxTokens);
        return OpenAiChatModel.builder()
                .openAiApi(chatApi)
                .defaultOptions(optionsBuilder.build())
                .build();
    }

    /**
     * ChatClient for medical case description generation (synthetic data).
     * Uses descriptionGenerationChatModel with lower max_tokens than primary chat.
     */
    @Bean("descriptionGenerationChatClient")
    public ChatClient descriptionGenerationChatClient(@Qualifier("descriptionGenerationChatModel") ChatModel descriptionGenerationChatModel) {
        log.info("Creating descriptionGenerationChatClient");
        return ChatClient.builder(descriptionGenerationChatModel).build();
    }

    /**
     * Primary EmbeddingModel configuration.
     * Supports separate base URLs for embedding service via spring.ai.custom.embedding.* properties.
     * Only supports OpenAI-compatible providers.
     */
    @Bean
    @Primary
    public EmbeddingModel primaryEmbeddingModel(List<EmbeddingModel> models) {
        // Check if custom embedding configuration is provided (separate base URL and provider)
        String embeddingBaseUrl = environment.getProperty("spring.ai.custom.embedding.base-url");
        String embeddingProvider = environment.getProperty("spring.ai.custom.embedding.provider", "openai");
        String embeddingApiKey = environment.getProperty("spring.ai.custom.embedding.api-key");
        String embeddingModel = environment.getProperty("spring.ai.custom.embedding.model");
        String embeddingDimensions = environment.getProperty("spring.ai.custom.embedding.dimensions");

        if (embeddingBaseUrl != null && !embeddingBaseUrl.isEmpty()) {
            // Create custom EmbeddingModel with separate base URL
            log.info("Creating custom EmbeddingModel with provider: {}, base URL: {}", embeddingProvider, embeddingBaseUrl);

            // Only support OpenAI-compatible providers
            if (!"openai".equalsIgnoreCase(embeddingProvider)) {
                throw new IllegalArgumentException("Only OpenAI-compatible providers are supported. Provider: " + embeddingProvider);
            }

            // Create OpenAI-compatible EmbeddingModel
            log.info("Creating OpenAiApi for EmbeddingModel! Base URL: {}", embeddingBaseUrl);
            OpenAiApi embeddingApi = OpenAiApi.builder()
                    .baseUrl(embeddingBaseUrl)
                    .apiKey(embeddingApiKey != null && !embeddingApiKey.isEmpty() ? embeddingApiKey : "dummy-key")
                    .build();
            log.info("OpenAiApi created!");

            OpenAiEmbeddingOptions.Builder optionsBuilder = OpenAiEmbeddingOptions.builder();
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
            OpenAiEmbeddingModel model = new OpenAiEmbeddingModel(
                    embeddingApi,
                    MetadataMode.EMBED,
                    optionsBuilder.build());
            log.info("OpenAiEmbeddingModel created! Type: {}", model.getClass().getName());
            return model;
        }

        // Fall back to auto-configured models (should not happen as auto-config is disabled)
        if (models.isEmpty()) {
            throw new IllegalStateException("No EmbeddingModel bean found. Please configure 'spring.ai.custom.embedding.*' properties.");
        }

        if (models.size() == 1) {
            return models.get(0);
        }

        // Select OpenAI-compatible model if multiple are present
        EmbeddingModel selected = models.stream()
                .filter(m -> m.getClass().getSimpleName().toLowerCase().contains("openai"))
                .findFirst()
                .orElse(models.get(0));

        log.info("Multiple EmbeddingModel beans found: {}. Selected primary: {}",
                models.stream().map(m -> m.getClass().getSimpleName()).toList(),
                selected.getClass().getSimpleName());

        return selected;
    }

    /**
     * Primary ChatModel configuration.
     * Supports separate base URLs for chat service via spring.ai.custom.chat.* properties.
     * Only supports OpenAI-compatible providers.
     */
    @Bean
    @Primary
    @Lazy
    public ChatModel primaryChatModel() {
        // Check if custom chat configuration is provided (separate base URL and provider)
        String chatBaseUrl = environment.getProperty("spring.ai.custom.chat.base-url");
        String chatProvider = environment.getProperty("spring.ai.custom.chat.provider", "openai");
        String chatApiKey = environment.getProperty("spring.ai.custom.chat.api-key");
        String chatModel = environment.getProperty("spring.ai.custom.chat.model");
        String chatTemperature = environment.getProperty("spring.ai.custom.chat.temperature");
        // Read max-tokens from custom config, fall back to openai.chat.options.max-tokens, then default to 4096
        String chatMaxTokens = environment.getProperty("spring.ai.custom.chat.max-tokens",
                environment.getProperty("spring.ai.openai.chat.options.max-tokens", "4096"));

        if (chatBaseUrl != null && !chatBaseUrl.isEmpty()) {
            // Create custom ChatModel with separate base URL
            log.info("Creating custom ChatModel with provider: {}, base URL: {}", chatProvider, chatBaseUrl);

            // Only support OpenAI-compatible providers
            if (!"openai".equalsIgnoreCase(chatProvider)) {
                throw new IllegalArgumentException("Only OpenAI-compatible providers are supported. Provider: " + chatProvider);
            }

            // Create OpenAI-compatible ChatModel
            log.info("Creating OpenAiApi for ChatModel! Base URL: {}", chatBaseUrl);
            OpenAiApi chatApi = OpenAiApi.builder()
                    .baseUrl(chatBaseUrl)
                    .apiKey(chatApiKey != null && !chatApiKey.isEmpty() ? chatApiKey : "dummy-key")
                    .build();
            log.info("OpenAiApi created!");

            OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder();
            if (chatModel != null && !chatModel.isEmpty()) {
                optionsBuilder.model(chatModel);
            }
            if (chatTemperature != null && !chatTemperature.isEmpty()) {
                try {
                    optionsBuilder.temperature(Double.parseDouble(chatTemperature));
                } catch (NumberFormatException e) {
                    log.warn("Invalid chat temperature: {}. Using default.", chatTemperature);
                }
            }
            if (chatMaxTokens != null && !chatMaxTokens.isEmpty()) {
                try {
                    optionsBuilder.maxTokens(Integer.parseInt(chatMaxTokens));
                } catch (NumberFormatException e) {
                    log.warn("Invalid chat max-tokens: {}. Using default.", chatMaxTokens);
                }
            }

            log.info("Creating OpenAiChatModel!");
            OpenAiChatModel model = OpenAiChatModel.builder()
                    .openAiApi(chatApi)
                    .defaultOptions(optionsBuilder.build())
                    .build();
            log.info("OpenAiChatModel created! Type: {}", model.getClass().getName());
            return model;
        }

        // Fall back to auto-configured models (should not happen as auto-config is disabled)
        String[] beanNames = beanFactory.getBeanNamesForType(ChatModel.class);
        List<ChatModel> models = Arrays.stream(beanNames)
                .filter(name -> !name.equals("primaryChatModel") && !name.equals("rerankingChatModel") && !name.equals("toolCallingChatModel"))
                .map(name -> beanFactory.getBean(name, ChatModel.class))
                .toList();

        if (models.isEmpty()) {
            throw new IllegalStateException("No ChatModel bean found. Please configure 'spring.ai.custom.chat.*' properties.");
        }

        ChatModel primary = models.get(0);
        if (models.size() > 1) {
            // Select OpenAI-compatible model if multiple are present
            primary = models.stream()
                    .filter(m -> m.getClass().getSimpleName().toLowerCase().contains("openai"))
                    .findFirst()
                    .orElse(models.get(0));
        }

        log.info("Primary ChatModel: {}", primary.getClass().getSimpleName());
        return primary;
    }

    /**
     * Reranking ChatModel configuration.
     * Creates a dedicated ChatModel for semantic reranking using the configured reranking model.
     * Only supports OpenAI-compatible providers.
     */
    @Bean
    @Qualifier("rerankingChatModel")
    public ChatModel rerankingChatModel() {
        // Check if custom reranking configuration is provided (separate base URL and provider)
        String rerankingBaseUrl = environment.getProperty("spring.ai.custom.reranking.base-url");
        String rerankingProvider = environment.getProperty("spring.ai.custom.reranking.provider", "openai");
        String rerankingModel = environment.getProperty("spring.ai.custom.reranking.model");
        String rerankingApiKey = environment.getProperty("spring.ai.custom.reranking.api-key");
        String rerankingTemperature = environment.getProperty("spring.ai.custom.reranking.temperature", "0.1");

        if (rerankingBaseUrl != null && !rerankingBaseUrl.isEmpty() && rerankingModel != null && !rerankingModel.isEmpty()) {
            // Create custom reranking ChatModel with separate base URL
            log.info("Creating custom reranking ChatModel with provider: {}, base URL: {}, model: {}",
                    rerankingProvider, rerankingBaseUrl, rerankingModel);

            // Only support OpenAI-compatible providers
            if (!"openai".equalsIgnoreCase(rerankingProvider)) {
                throw new IllegalArgumentException("Only OpenAI-compatible providers are supported. Provider: " + rerankingProvider);
            }

            // Create OpenAI-compatible ChatModel for reranking
            log.info("Creating OpenAiApi for reranking ChatModel! Base URL: {}", rerankingBaseUrl);
            OpenAiApi rerankingApi = OpenAiApi.builder()
                    .baseUrl(rerankingBaseUrl)
                    .apiKey(rerankingApiKey != null && !rerankingApiKey.isEmpty() ? rerankingApiKey : "dummy-key")
                    .build();
            log.info("OpenAiApi created!");

            OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                    .model(rerankingModel);
            try {
                optionsBuilder.temperature(Double.parseDouble(rerankingTemperature));
            } catch (NumberFormatException e) {
                log.warn("Invalid reranking temperature: {}. Using default 0.1.", rerankingTemperature);
                optionsBuilder.temperature(0.1);
            }

            log.info("Creating OpenAiChatModel for reranking!");
            OpenAiChatModel rerankingModelInstance = OpenAiChatModel.builder()
                    .openAiApi(rerankingApi)
                    .defaultOptions(optionsBuilder.build())
                    .build();
            log.info("OpenAiChatModel created! Type: {}", rerankingModelInstance.getClass().getName());
            return rerankingModelInstance;
        }

        // If reranking is not configured, return null (semantic reranker will handle gracefully)
        log.info("Reranking ChatModel not configured. Semantic reranking will use placeholder implementation.");
        return null;
    }

    /**
     * Tool Calling ChatModel configuration.
     * Creates a dedicated ChatModel for tool/function calling operations using FunctionGemma.
     * This is separate from the primary ChatModel because medgemma:1.5-4b doesn't support tools,
     * while functiongemma does support tool calling.
     * Only supports OpenAI-compatible providers.
     */
    @Bean
    @Qualifier("toolCallingChatModel")
    public ChatModel toolCallingChatModel() {
        // Check if custom tool calling configuration is provided (separate base URL and provider)
        String toolCallingBaseUrl = environment.getProperty("spring.ai.custom.tool-calling.base-url");
        String toolCallingProvider = environment.getProperty("spring.ai.custom.tool-calling.provider", "openai");
        String toolCallingModel = environment.getProperty("spring.ai.custom.tool-calling.model", "functiongemma");
        String toolCallingApiKey = environment.getProperty("spring.ai.custom.tool-calling.api-key");
        String toolCallingTemperature = environment.getProperty("spring.ai.custom.tool-calling.temperature", "0.7");
        String toolCallingMaxTokens = environment.getProperty("spring.ai.custom.tool-calling.max-tokens", "4096");

        // If base URL is not explicitly set, fall back to chat base URL
        if (toolCallingBaseUrl == null || toolCallingBaseUrl.isEmpty()) {
            toolCallingBaseUrl = environment.getProperty("spring.ai.custom.chat.base-url");
        }
        // If API key is not explicitly set, fall back to chat API key
        if (toolCallingApiKey == null || toolCallingApiKey.isEmpty()) {
            toolCallingApiKey = environment.getProperty("spring.ai.custom.chat.api-key");
        }

        if (toolCallingBaseUrl != null && !toolCallingBaseUrl.isEmpty()) {
            // Create custom tool calling ChatModel with separate base URL
            log.info("Creating custom tool calling ChatModel with provider: {}, base URL: {}, model: {}",
                    toolCallingProvider, toolCallingBaseUrl, toolCallingModel);

            // Only support OpenAI-compatible providers
            if (!"openai".equalsIgnoreCase(toolCallingProvider)) {
                throw new IllegalArgumentException("Only OpenAI-compatible providers are supported. Provider: " + toolCallingProvider);
            }

            // Create OpenAI-compatible ChatModel for tool calling
            log.info("Creating OpenAiApi for tool calling ChatModel! Base URL: {}", toolCallingBaseUrl);
            OpenAiApi toolCallingApi = OpenAiApi.builder()
                    .baseUrl(toolCallingBaseUrl)
                    .apiKey(toolCallingApiKey != null && !toolCallingApiKey.isEmpty() ? toolCallingApiKey : "dummy-key")
                    .build();
            log.info("OpenAiApi created!");

            OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                    .model(toolCallingModel);
            try {
                optionsBuilder.temperature(Double.parseDouble(toolCallingTemperature));
            } catch (NumberFormatException e) {
                log.warn("Invalid tool calling temperature: {}. Using default 0.7.", toolCallingTemperature);
                optionsBuilder.temperature(0.7);
            }
            if (toolCallingMaxTokens != null && !toolCallingMaxTokens.isEmpty()) {
                try {
                    optionsBuilder.maxTokens(Integer.parseInt(toolCallingMaxTokens));
                } catch (NumberFormatException e) {
                    log.warn("Invalid tool calling max-tokens: {}. Using default.", toolCallingMaxTokens);
                }
            }

            log.info("Creating OpenAiChatModel for tool calling!");
            OpenAiChatModel toolCallingModelInstance = OpenAiChatModel.builder()
                    .openAiApi(toolCallingApi)
                    .defaultOptions(optionsBuilder.build())
                    .build();
            log.info("OpenAiChatModel created! Type: {}", toolCallingModelInstance.getClass().getName());
            return toolCallingModelInstance;
        }

        // If tool calling is not configured, fall back to primary chat model (may not support tools)
        log.warn("Tool calling ChatModel not configured. Falling back to primary ChatModel (may not support tools).");
        return primaryChatModel();
    }
}
