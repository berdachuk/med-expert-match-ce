package com.berdachuk.medexpertmatch.llm.eval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Optional live eval against a real FunctionGemma endpoint (Ollama OpenAI-compatible).
 *
 * <p>Run baseline:
 * {@code mvn test -Dtest=ToolSelectionLiveEvalIT -Dmedexpertmatch.eval.tool-selection.live=true -Dmedexpertmatch.eval.tool-selection.label=baseline}
 *
 * <p>Large generated dataset:
 * {@code -Dmedexpertmatch.eval.tool-selection.dataset=target/eval/tool-selection-large.jsonl}
 *
 * <p>Run after fine-tune (change model via env):
 * {@code set TOOL_CALLING_MODEL=functiongemma-medexpertmatch:270m && mvn test ... -Dmedexpertmatch.eval.tool-selection.label=finetuned}
 */
/**
 * REQ-018: integration coverage for registered requirement.
 * REQ-134: integration coverage for registered requirement.
 */
@EnabledIfSystemProperty(named = "medexpertmatch.eval.tool-selection.live", matches = "true")
class ToolSelectionLiveEvalIT {

    @Test
    @DisplayName("live FunctionGemma golden dataset eval writes JSON and Markdown report")
    void runLiveGoldenEval() throws Exception {
        String baseUrl = env("TOOL_CALLING_BASE_URL", "http://127.0.0.1:11434/v1");
        String apiKey = env("TOOL_CALLING_API_KEY", "none");
        String model = env("TOOL_CALLING_MODEL", "functiongemma:270m");
        String label = System.getProperty("medexpertmatch.eval.tool-selection.label", "baseline");

        OpenAiChatModel chatModel = buildToolCallingModel(baseUrl, apiKey, model);
        ToolSelectionLiveEvalService service = new ToolSelectionLiveEvalService(
                chatModel,
                new ToolSelectionPromptBuilder(),
                new FunctionGemmaToolCallParser());

        ToolSelectionLiveEvalReport report = service.evaluateGoldenDataset(model, label);
        assertNotNull(report);
        assertTrue(report.totalCases() >= 20);

        Path outputDir = Path.of("target", "eval");
        Path jsonPath = new ToolSelectionLiveEvalReportWriter().writeReport(report, outputDir);
        assertTrue(jsonPath.toFile().exists(), "Report JSON should exist: " + jsonPath);

        System.out.printf(Locale.ROOT,
                "FunctionGemma live eval [%s] model=%s accuracy=%.1f%% (%d/%d) -> %s%n",
                label, model, report.accuracy() * 100.0, report.passedCases(), report.totalCases(), jsonPath);
    }

    private static OpenAiChatModel buildToolCallingModel(String baseUrl, String apiKey, String model) {
        return OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .model(model)
                        .temperature(0.0)
                        .build())
                .build();
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
