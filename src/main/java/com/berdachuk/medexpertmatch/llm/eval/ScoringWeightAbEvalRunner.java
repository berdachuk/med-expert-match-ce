package com.berdachuk.medexpertmatch.llm.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ScoringWeightAbEvalRunner {

    private static final String DATASET = "/eval/scoring-weight-ab-cases.jsonl";

    private ScoringWeightAbEvalRunner() {
    }

    public static EvalFamilyResult run() {
        ObjectMapper objectMapper = new ObjectMapper();
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
                WeightSet defaultWeights = readWeights(node.get("defaultWeights"));
                WeightSet altWeights = readWeights(node.get("altWeights"));
                Map<String, ChannelScores> doctors = readDoctors(node.get("doctors"));

                String defaultTop = topDoctor(doctors, defaultWeights);
                String altTop = topDoctor(doctors, altWeights);
                if (defaultTop.equals(node.get("expectedDefaultTop").asText())
                        && altTop.equals(node.get("expectedAltTop").asText())) {
                    passed++;
                }
            }
            return new EvalFamilyResult("scoring_weight_ab", "RETRIEVAL", passed, total, 0, true);
        } catch (Exception e) {
            throw new IllegalStateException("Scoring weight A/B eval failed", e);
        }
    }

    static double weightedScore(ChannelScores scores, WeightSet weights) {
        return scores.vector() * weights.vector()
                + scores.graph() * weights.graph()
                + scores.historical() * weights.historical();
    }

    private static String topDoctor(Map<String, ChannelScores> doctors, WeightSet weights) {
        return doctors.entrySet().stream()
                .max(Comparator.comparingDouble(entry -> weightedScore(entry.getValue(), weights)))
                .map(Map.Entry::getKey)
                .orElseThrow();
    }

    private static Map<String, ChannelScores> readDoctors(JsonNode doctorsNode) {
        Map<String, ChannelScores> doctors = new LinkedHashMap<>();
        for (Iterator<JsonNode> it = doctorsNode.elements(); it.hasNext(); ) {
            JsonNode doctor = it.next();
            doctors.put(
                    doctor.get("id").asText(),
                    new ChannelScores(
                            doctor.get("vector").asDouble(),
                            doctor.get("graph").asDouble(),
                            doctor.get("historical").asDouble()));
        }
        return doctors;
    }

    private static WeightSet readWeights(JsonNode node) {
        return new WeightSet(
                node.get("vector").asDouble(),
                node.get("graph").asDouble(),
                node.get("historical").asDouble());
    }

    private static InputStream resourceStream(String path) {
        return Objects.requireNonNull(ScoringWeightAbEvalRunner.class.getResourceAsStream(path), path);
    }

    record ChannelScores(double vector, double graph, double historical) {
    }

    record WeightSet(double vector, double graph, double historical) {
    }
}
