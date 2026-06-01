package com.berdachuk.medexpertmatch.llm.evaluation;

import org.springframework.stereotype.Component;

/**
 * Offline structural integrity gate for eval datasets (no LLM required).
 */
@Component
public class EvalDatasetIntegrityService {

    private static final java.util.Set<String> ALLOWED_TYPES = java.util.Set.of(
            "case-analysis",
            "doctor-match",
            "facility-routing",
            "queue-priority");

    public double computeIntegrityPassRate(String classpathResource) {
        int total = 0;
        int valid = 0;
        try (var in = getClass().getClassLoader().getResourceAsStream(classpathResource)) {
            if (in == null) {
                return 0.0;
            }
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    total++;
                    if (isValidLine(line)) {
                        valid++;
                    }
                }
            }
        } catch (Exception e) {
            return 0.0;
        }
        return total == 0 ? 0.0 : (double) valid / total;
    }

    @SuppressWarnings("unchecked")
    private static boolean isValidLine(String line) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> row = mapper.readValue(line, java.util.Map.class);
            Object question = row.get("question");
            Object answer = row.get("answer");
            if (!(question instanceof String q) || q.isBlank()) {
                return false;
            }
            if (!(answer instanceof String a) || a.isBlank()) {
                return false;
            }
            Object metaObj = row.get("meta");
            if (!(metaObj instanceof java.util.Map<?, ?> meta)) {
                return false;
            }
            Object typeObj = meta.get("type");
            if (!(typeObj instanceof String type) || !ALLOWED_TYPES.contains(type)) {
                return false;
            }
            if ("doctor-match".equals(type) || "facility-routing".equals(type)) {
                Object caseId = meta.get("caseId");
                return caseId instanceof String cid && !cid.isBlank();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
