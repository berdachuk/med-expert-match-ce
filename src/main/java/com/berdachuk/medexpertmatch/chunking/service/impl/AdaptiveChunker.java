package com.berdachuk.medexpertmatch.chunking.service.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "medexpertmatch.documents.enabled", havingValue = "true")
public class AdaptiveChunker implements com.berdachuk.medexpertmatch.chunking.api.Chunker {

    private final SemanticChunker semanticChunker;
    private final RecursiveCharacterChunker recursiveCharacterChunker;

    public AdaptiveChunker(SemanticChunker semanticChunker, RecursiveCharacterChunker recursiveCharacterChunker) {
        this.semanticChunker = semanticChunker;
        this.recursiveCharacterChunker = recursiveCharacterChunker;
    }

    @Override
    public List<String> chunk(String text, int chunkSize, int overlap, int minChars) {
        if (text == null || text.length() < minChars) {
            return List.of();
        }

        int paragraphCount = countOccurrences(text, "\n\n");
        int sentenceCount = countSentenceBoundaries(text);

        if ((paragraphCount > 2 && sentenceCount > 2)
                || sentenceCount > 10
                || (paragraphCount > 0 && sentenceCount > 2 && sentenceCount > paragraphCount)) {
            return semanticChunker.chunk(text, chunkSize, overlap, minChars);
        }

        return recursiveCharacterChunker.chunk(text, chunkSize, overlap, minChars);
    }

    private static int countOccurrences(String text, String pattern) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(pattern, idx)) != -1) {
            count++;
            idx += pattern.length();
        }
        return count;
    }

    private static int countSentenceBoundaries(String text) {
        int count = 0;
        for (int i = 0; i < text.length() - 1; i++) {
            char c = text.charAt(i);
            if ((c == '.' || c == '!' || c == '?') && text.charAt(i + 1) == ' ') {
                count++;
            }
        }
        return count;
    }
}
