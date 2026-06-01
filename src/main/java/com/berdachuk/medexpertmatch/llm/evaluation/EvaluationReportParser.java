package com.berdachuk.medexpertmatch.llm.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class EvaluationReportParser {

    private final ObjectMapper objectMapper;

    public EvaluationReportParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public double extractNormalizedAccuracy(String reportJson) {
        try {
            JsonNode root = objectMapper.readTree(reportJson);
            if (root.has("normalized_accuracy")) {
                return root.get("normalized_accuracy").asDouble();
            }
            return 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }
}
