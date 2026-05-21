package com.berdachuk.medexpertmatch.chunking.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChunkerFactoryIT {

    private final SemanticChunker semanticChunker = new SemanticChunker();
    private final RecursiveCharacterChunker recursiveChunker = new RecursiveCharacterChunker();
    private final AdaptiveChunker adaptiveChunker = new AdaptiveChunker(semanticChunker, recursiveChunker);
    private final ChunkerFactory chunkerFactory = new ChunkerFactory(adaptiveChunker, semanticChunker, recursiveChunker);

    @Test
    void shouldReturnAdaptiveForNullStrategy() {
        var chunker = chunkerFactory.getChunker(null);
        assertTrue(chunker instanceof AdaptiveChunker);
    }

    @Test
    void shouldReturnAdaptiveForDefaultStrategy() {
        var chunker = chunkerFactory.getChunker("adaptive");
        assertTrue(chunker instanceof AdaptiveChunker);
    }

    @Test
    void shouldReturnSemanticForSemanticStrategy() {
        var chunker = chunkerFactory.getChunker("semantic");
        assertTrue(chunker instanceof SemanticChunker);
    }

    @Test
    void shouldReturnRecursiveForRecursiveStrategy() {
        var chunker = chunkerFactory.getChunker("recursive");
        assertTrue(chunker instanceof RecursiveCharacterChunker);
    }

    @Test
    void shouldReturnRecursiveForCharacterStrategy() {
        var chunker = chunkerFactory.getChunker("character");
        assertTrue(chunker instanceof RecursiveCharacterChunker);
    }

    @Test
    void shouldReturnAdaptiveForUnknownStrategy() {
        var chunker = chunkerFactory.getChunker("unknown");
        assertTrue(chunker instanceof AdaptiveChunker);
    }

    @Test
    void shouldReturnAdaptiveForUpperCaseStrategy() {
        var chunker = chunkerFactory.getChunker("SEMANTIC");
        assertTrue(chunker instanceof SemanticChunker);
    }

    @Test
    void returnedChunkerShouldWork() {
        var chunker = chunkerFactory.getChunker("recursive");
        var chunks = chunker.chunk("Hello world. This is a test.", 10, 2, 5);
        assertFalse(chunks.isEmpty());
    }
}
