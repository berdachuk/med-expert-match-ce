package com.berdachuk.medexpertmatch.documents.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class StructuredFileParser {

    private final ObjectMapper objectMapper;

    public StructuredFileParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    public List<ParsedDocument> parseJsonl(Path path) throws IOException {
        List<ParsedDocument> docs = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                Map<String, Object> entry = objectMapper.readValue(line,
                        new TypeReference<Map<String, Object>>() {});
                docs.add(mapToDocument(entry));
            }
        }
        return docs;
    }

    @SuppressWarnings("unchecked")
    public List<ParsedDocument> parseJson(Path path) throws IOException {
        List<ParsedDocument> docs = new ArrayList<>();
        String content = Files.readString(path, StandardCharsets.UTF_8);
        Object parsed = objectMapper.readValue(content, Object.class);
        if (parsed instanceof List) {
            for (Object item : (List<Object>) parsed) {
                if (item instanceof Map) {
                    docs.add(mapToDocument((Map<String, Object>) item));
                }
            }
        } else if (parsed instanceof Map) {
            docs.add(mapToDocument((Map<String, Object>) parsed));
        }
        return docs;
    }

    public List<ParsedDocument> parseCsv(Path path) throws IOException {
        List<ParsedDocument> docs = new ArrayList<>();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return docs;
        }
        String[] headers = lines.get(0).split(",");
        for (int i = 1; i < lines.size(); i++) {
            String[] values = lines.get(i).split(",");
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < Math.min(headers.length, values.length); j++) {
                sb.append(headers[j].trim()).append(": ").append(values[j].trim()).append("\n");
            }
            docs.add(new ParsedDocument(
                    path.getFileName().toString() + "#" + i,
                    null,
                    null,
                    path.getFileName().toString(),
                    null,
                    sb.toString(),
                    "csv"));
        }
        return docs;
    }

    private ParsedDocument mapToDocument(Map<String, Object> entry) {
        String content = entry.containsKey("text") ? (String) entry.get("text")
                : entry.containsKey("content") ? (String) entry.get("content")
                : objectMapper != null ? entry.toString() : "";

        return new ParsedDocument(
                (String) entry.getOrDefault("id", null),
                (String) entry.getOrDefault("title", null),
                (String) entry.getOrDefault("category", null),
                (String) entry.getOrDefault("source", null),
                (String) entry.getOrDefault("url", null),
                content,
                "jsonl");
    }

    public record ParsedDocument(
            String externalId,
            String title,
            String category,
            String sourceName,
            String sourceUrl,
            String content,
            String sourceFormat) {}
}
