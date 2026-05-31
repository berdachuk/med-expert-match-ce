package com.berdachuk.medexpertmatch.web.service;

/**
 * Server-side markdown rendering for chat history (defense-in-depth with client DOMPurify).
 */
public interface ChatMarkdownRenderer {

    String renderSafe(String markdown);
}
