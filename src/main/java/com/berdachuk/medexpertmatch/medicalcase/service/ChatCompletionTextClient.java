package com.berdachuk.medexpertmatch.medicalcase.service;

import java.util.Optional;

/**
 * Minimal OpenAI-compatible chat completion (non-streaming) for text extraction.
 */
public interface ChatCompletionTextClient {

    /**
     * Calls POST /v1/chat/completions and returns assistant-visible text.
     *
     * @param systemPrompt system message text
     * @param userPrompt   user message text
     * @return non-blank text when the HTTP call succeeds and a usable assistant payload exists
     */
    Optional<String> complete(String systemPrompt, String userPrompt);
}
