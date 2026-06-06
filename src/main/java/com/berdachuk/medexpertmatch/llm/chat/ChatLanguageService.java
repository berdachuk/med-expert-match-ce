package com.berdachuk.medexpertmatch.llm.chat;

/**
 * Detects user language and translates chat text to/from English for processing.
 */
public interface ChatLanguageService {

    ChatLanguageTurn prepareTurn(String originalText);

    String localizeReply(ChatLanguageTurn turn, String englishReply);
}
