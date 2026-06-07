package com.berdachuk.medexpertmatch.llm.eval;

import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import com.berdachuk.medexpertmatch.llm.tool.ToolSelectionPolicy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ToolSelectionEvalRunner {

    private static final String DATASET = "/eval/tool-selection-cases.jsonl";
    private static final long ESTIMATED_TOKENS = 2048;

    private ToolSelectionEvalRunner() {
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
                GoalType goalType = GoalType.valueOf(node.get("goalType").asText());
                boolean caseIdInHints = node.path("caseIdInHints").asBoolean(false);
                String caseId = node.hasNonNull("caseId") ? node.get("caseId").asText() : null;
                String userMessage = node.get("userMessage").asText();

                Optional<ToolSelectionPolicy.ToolChoice> actual =
                        ToolSelectionPolicy.resolve(goalType, caseIdInHints, caseId, userMessage);

                if (matchesExpected(node, actual)) {
                    passed++;
                }
            }
            return new EvalFamilyResult("tool_selection", "LIGHT", passed, total, ESTIMATED_TOKENS, true);
        } catch (Exception e) {
            throw new IllegalStateException("Tool selection eval failed", e);
        }
    }

    private static boolean matchesExpected(JsonNode node, Optional<ToolSelectionPolicy.ToolChoice> actual) {
        JsonNode expectedToolNode = node.get("expectedTool");
        if (expectedToolNode == null || expectedToolNode.isNull()) {
            return actual.isEmpty();
        }
        if (actual.isEmpty()) {
            return false;
        }
        String expectedTool = expectedToolNode.asText();
        if (!expectedTool.equals(actual.get().toolName())) {
            return false;
        }
        Map<String, String> expectedArgs = readStringMap(node.get("expectedArgs"));
        for (Map.Entry<String, String> entry : expectedArgs.entrySet()) {
            if (!entry.getValue().equals(actual.get().args().get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, String> readStringMap(JsonNode node) {
        Map<String, String> map = new HashMap<>();
        if (node == null || node.isNull()) {
            return map;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            map.put(field.getKey(), field.getValue().asText());
        }
        return map;
    }

    private static InputStream resourceStream(String path) {
        return Objects.requireNonNull(ToolSelectionEvalRunner.class.getResourceAsStream(path), path);
    }
}
