package com.berdachuk.medexpertmatch.chunking.service.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "medexpertmatch.documents.enabled", havingValue = "true")
public class RecursiveCharacterChunker implements com.berdachuk.medexpertmatch.chunking.api.Chunker {

    @Override
    public List<String> chunk(String text, int chunkSize, int overlap, int minChars) {
        if (text == null || text.length() < minChars) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            if (end < text.length()) {
                int breakPoint = findBreakPoint(text, end, start);
                if (breakPoint > start) {
                    end = breakPoint;
                }
            }
            String chunk = text.substring(start, end).trim();
            if (chunk.length() >= minChars) {
                chunks.add(chunk);
            }
            start = Math.max(start + chunkSize - overlap, start + 1);
            if (start >= text.length()) {
                break;
            }
        }
        return chunks;
    }

    private int findBreakPoint(String text, int end, int start) {
        for (int i = end - 1; i > start; i--) {
            char c = text.charAt(i);
            if (c == '\n' || c == '.' || c == '!' || c == '?') {
                return i + 1;
            }
        }
        for (int i = end - 1; i > start; i--) {
            if (text.charAt(i) == ' ') {
                return i + 1;
            }
        }
        return start + 1;
    }
}
