package com.berdachuk.medexpertmatch.chunking.service.impl;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChunkerIT {

    private final SemanticChunker semanticChunker = new SemanticChunker();
    private final RecursiveCharacterChunker recursiveChunker = new RecursiveCharacterChunker();
    private final AdaptiveChunker adaptiveChunker = new AdaptiveChunker(semanticChunker, recursiveChunker);

    @Test
    void semanticChunkerShouldSplitAtSentenceBoundaries() {
        String text = "First sentence. Second sentence. Third sentence.";
        List<String> chunks = semanticChunker.chunk(text, 100, 0, 10);
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.get(0).contains("First"));
    }

    @Test
    void semanticChunkerShouldReturnEmptyForShortText() {
        List<String> chunks = semanticChunker.chunk("Short.", 100, 0, 50);
        assertTrue(chunks.isEmpty());
    }

    @Test
    void semanticChunkerShouldReturnEmptyForNull() {
        assertTrue(semanticChunker.chunk(null, 100, 0, 10).isEmpty());
    }

    @Test
    void semanticChunkerShouldReturnEmptyForBlank() {
        assertTrue(semanticChunker.chunk("   ", 100, 0, 10).isEmpty());
    }

    @Test
    void semanticChunkerShouldChunkLongText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("Sentence number ").append(i).append(". ");
        }
        List<String> chunks = semanticChunker.chunk(sb.toString(), 200, 20, 10);
        assertTrue(chunks.size() > 1);
    }

    @Test
    void recursiveChunkerShouldSplitAtBreakpoints() {
        String text = "This is a test sentence. Here is another one. And a third.";
        List<String> chunks = recursiveChunker.chunk(text, 20, 5, 10);
        assertFalse(chunks.isEmpty());
    }

    @Test
    void recursiveChunkerShouldReturnEmptyForNull() {
        assertTrue(recursiveChunker.chunk(null, 100, 0, 10).isEmpty());
    }

    @Test
    void recursiveChunkerShouldReturnEmptyForShortText() {
        assertTrue(recursiveChunker.chunk("Hi.", 100, 0, 50).isEmpty());
    }

    @Test
    void adaptiveChunkerShouldUseSemanticForParagraphText() {
        String text = "Paragraph one with multiple sentences. Here is another sentence. And more text.\n\n"
                + "Paragraph two with content. This continues. More to read.\n\n"
                + "Paragraph three. With more content. And even more.";
        List<String> chunks = adaptiveChunker.chunk(text, 200, 20, 10);
        assertFalse(chunks.isEmpty());
    }

    @Test
    void adaptiveChunkerShouldUseRecursiveForUnstructuredText() {
        String text = "a b c d e f g h i j k l m n o p q r s t u v w x y z 1 2 3 4 5 6 7 8 9 0";
        List<String> chunks = adaptiveChunker.chunk(text, 20, 5, 5);
        assertFalse(chunks.isEmpty());
    }

    @Test
    void adaptiveChunkerShouldReturnEmptyForNull() {
        assertTrue(adaptiveChunker.chunk(null, 100, 0, 10).isEmpty());
    }

    @Test
    void adaptiveChunkerShouldReturnEmptyForShortText() {
        assertTrue(adaptiveChunker.chunk("Abc", 100, 0, 10).isEmpty());
    }
}
