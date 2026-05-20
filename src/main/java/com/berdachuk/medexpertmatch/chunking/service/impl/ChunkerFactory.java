package com.berdachuk.medexpertmatch.chunking.service.impl;

import com.berdachuk.medexpertmatch.chunking.api.Chunker;
import org.springframework.stereotype.Component;

@Component
public class ChunkerFactory {

    private final AdaptiveChunker adaptiveChunker;
    private final SemanticChunker semanticChunker;
    private final RecursiveCharacterChunker recursiveCharacterChunker;

    public ChunkerFactory(AdaptiveChunker adaptiveChunker,
                          SemanticChunker semanticChunker,
                          RecursiveCharacterChunker recursiveCharacterChunker) {
        this.adaptiveChunker = adaptiveChunker;
        this.semanticChunker = semanticChunker;
        this.recursiveCharacterChunker = recursiveCharacterChunker;
    }

    public Chunker getChunker(String strategy) {
        if (strategy == null) {
            return adaptiveChunker;
        }
        return switch (strategy.toLowerCase()) {
            case "semantic" -> semanticChunker;
            case "recursive", "character" -> recursiveCharacterChunker;
            default -> adaptiveChunker;
        };
    }
}
