package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.core.util.LlmCallLimiter;
import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import com.berdachuk.medexpertmatch.llm.chat.ChatLanguageDetector;
import com.berdachuk.medexpertmatch.llm.chat.ChatLanguageService;
import com.berdachuk.medexpertmatch.llm.chat.ChatLanguageTurn;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class ChatLanguageServiceImpl implements ChatLanguageService {

    private final ChatClient chatClient;
    private final PromptTemplate translateToEnglishTemplate;
    private final PromptTemplate translateFromEnglishTemplate;
    private final LlmCallLimiter llmCallLimiter;

    public ChatLanguageServiceImpl(
            @Qualifier("utilityChatModel") ChatModel utilityChatModel,
            @Qualifier("chatTranslateToEnglishPromptTemplate") PromptTemplate translateToEnglishTemplate,
            @Qualifier("chatTranslateFromEnglishPromptTemplate") PromptTemplate translateFromEnglishTemplate,
            LlmCallLimiter llmCallLimiter) {
        this.chatClient = ChatClient.builder(utilityChatModel).build();
        this.translateToEnglishTemplate = translateToEnglishTemplate;
        this.translateFromEnglishTemplate = translateFromEnglishTemplate;
        this.llmCallLimiter = llmCallLimiter;
    }

    @Override
    public ChatLanguageTurn prepareTurn(String originalText) {
        if (originalText == null || originalText.isBlank()) {
            return ChatLanguageTurn.english("");
        }
        String languageTag = ChatLanguageDetector.detectLanguageTag(originalText);
        if (!ChatLanguageDetector.requiresEnglishProcessing(languageTag)) {
            return ChatLanguageTurn.english(originalText.trim());
        }
        String english = translateToEnglish(originalText.trim(), languageTag);
        return new ChatLanguageTurn(originalText.trim(), english, languageTag, true);
    }

    @Override
    public String localizeReply(ChatLanguageTurn turn, String englishReply) {
        if (englishReply == null || englishReply.isBlank()) {
            return englishReply;
        }
        if (turn == null || turn.isEnglish()) {
            return englishReply.trim();
        }
        return translateFromEnglish(englishReply.trim(), turn.userLanguageTag());
    }

    private String translateToEnglish(String text, String sourceLanguage) {
        try {
            String systemPrompt = translateToEnglishTemplate.render(Map.of());
            String userPrompt = "Source language: " + sourceLanguage + "\n\nMessage:\n" + text;
            String translated = llmCallLimiter.execute(LlmClientType.UTILITY, () ->
                    chatClient.prompt()
                            .system(systemPrompt)
                            .user(userPrompt)
                            .call()
                            .content());
            if (translated == null || translated.isBlank()) {
                log.warn("Empty EN translation for language {}; using original text", sourceLanguage);
                return text;
            }
            log.debug("Translated user message from {} to English (length {} -> {})",
                    sourceLanguage, text.length(), translated.trim().length());
            return translated.trim();
        } catch (Exception e) {
            log.warn("Failed to translate to English ({}): {}", sourceLanguage, e.getMessage());
            return text;
        }
    }

    private String translateFromEnglish(String englishReply, String targetLanguage) {
        try {
            String systemPrompt = translateFromEnglishTemplate.render(Map.of("targetLanguage", targetLanguage));
            String userPrompt = "English reply:\n" + englishReply;
            String translated = llmCallLimiter.execute(LlmClientType.UTILITY, () ->
                    chatClient.prompt()
                            .system(systemPrompt)
                            .user(userPrompt)
                            .call()
                            .content());
            if (translated == null || translated.isBlank()) {
                log.warn("Empty translation to {}; returning English reply", targetLanguage);
                return englishReply;
            }
            log.debug("Translated assistant reply to {} (length {} -> {})",
                    targetLanguage, englishReply.length(), translated.trim().length());
            return translated.trim();
        } catch (Exception e) {
            log.warn("Failed to translate reply to {}: {}", targetLanguage, e.getMessage());
            return englishReply;
        }
    }
}
