package com.berdachuk.medexpertmatch.web.service;

import com.berdachuk.medexpertmatch.web.service.impl.ChatMarkdownRendererImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatMarkdownRendererTest {

    private ChatMarkdownRendererImpl renderer;

    @BeforeEach
    void setUp() {
        renderer = new ChatMarkdownRendererImpl();
    }

    @Test
    @DisplayName("Renders bold markdown to safe strong tag")
    void rendersBoldMarkdown() {
        String html = renderer.renderSafe("**Bold** clinical summary");
        assertTrue(html.contains("<strong>Bold</strong>"));
        assertFalse(html.contains("**"));
    }

    @Test
    @DisplayName("Strips javascript: links from rendered markdown")
    void stripsJavascriptLinks() {
        String html = renderer.renderSafe("**[click](javascript:alert(1))**");
        assertFalse(html.toLowerCase().contains("javascript:"));
        assertFalse(html.contains("href=\"javascript:"));
        assertTrue(html.contains("<strong>click</strong>"));
    }

    @Test
    @DisplayName("Allows https links in rendered markdown")
    void allowsHttpsLinks() {
        String html = renderer.renderSafe("[PubMed](https://pubmed.ncbi.nlm.nih.gov/)");
        assertTrue(html.contains("href=\"https://pubmed.ncbi.nlm.nih.gov/\""));
    }

    @Test
    @DisplayName("Escapes raw HTML in markdown input")
    void escapesRawHtml() {
        String html = renderer.renderSafe("<script>alert(1)</script>");
        assertFalse(html.contains("<script>"));
    }
}
