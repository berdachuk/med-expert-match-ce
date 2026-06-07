package com.berdachuk.medexpertmatch.llm.eval;

import com.berdachuk.medexpertmatch.llm.harness.HarnessContextKind;
import com.berdachuk.medexpertmatch.llm.harness.HarnessContextSummarizer;
import com.berdachuk.medexpertmatch.llm.harness.HarnessContextSummarizerImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Objects;

public final class ContextSummarizerEvalRunner {

    private static final String DATASET = "/eval/context-summarizer-cases.jsonl";
    private static final long ESTIMATED_TOKENS = 6000;

    private ContextSummarizerEvalRunner() {
    }

    public static EvalFamilyResult run() {
        ObjectMapper objectMapper = new ObjectMapper();
        HarnessContextSummarizer summarizer = new HarnessContextSummarizerImpl(objectMapper);
        int passed = 0;
        int total = 0;
        try (InputStream stream = resourceStream(DATASET);
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                total++;
                JsonNode node = objectMapper.readTree(line);
                HarnessContextKind kind = HarnessContextKind.valueOf(node.get("kind").asText());
                String raw = node.get("raw").asText();
                String summary = summarizer.summarizeToolResults(raw, kind);
                if (preservesWhitelist(node, summary) && dropsNoise(node, summary)) {
                    passed++;
                }
            }
            return new EvalFamilyResult("context_summarizer", "FULL", passed, total, ESTIMATED_TOKENS, false);
        } catch (Exception e) {
            throw new IllegalStateException("Context summarizer eval failed", e);
        }
    }

    private static boolean preservesWhitelist(JsonNode node, String summary) {
        if (!node.has("preserve")) {
            return true;
        }
        for (Iterator<JsonNode> it = node.get("preserve").elements(); it.hasNext(); ) {
            if (!summary.contains(it.next().asText())) {
                return false;
            }
        }
        return true;
    }

    private static boolean dropsNoise(JsonNode node, String summary) {
        if (!node.has("drop")) {
            return true;
        }
        for (Iterator<JsonNode> it = node.get("drop").elements(); it.hasNext(); ) {
            if (summary.contains(it.next().asText())) {
                return false;
            }
        }
        return true;
    }

    private static InputStream resourceStream(String path) {
        return Objects.requireNonNull(ContextSummarizerEvalRunner.class.getResourceAsStream(path), path);
    }
}
