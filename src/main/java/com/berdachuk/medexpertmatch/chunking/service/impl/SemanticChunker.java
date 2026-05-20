package com.berdachuk.medexpertmatch.chunking.service.impl;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class SemanticChunker implements com.berdachuk.medexpertmatch.chunking.api.Chunker {

    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[.!?])\\s+");

    @Override
    public List<String> chunk(String text, int chunkSize, int overlap, int minChars) {
        if (text == null || text.length() < minChars) {
            return List.of();
        }

        String[] sentences = SENTENCE_BOUNDARY.split(text);
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (current.length() + trimmed.length() > chunkSize && !current.isEmpty()) {
                chunks.add(current.toString().trim());
                if (overlap > 0 && current.length() > overlap) {
                    String overlapText = current.substring(Math.max(0, current.length() - overlap));
                    current = new StringBuilder(overlapText);
                } else {
                    current = new StringBuilder();
                }
            }
            if (!current.isEmpty()) {
                current.append(" ");
            }
            current.append(trimmed);
        }

        if (!current.isEmpty() && current.length() >= minChars) {
            chunks.add(current.toString().trim());
        }

        return chunks;
    }
}
