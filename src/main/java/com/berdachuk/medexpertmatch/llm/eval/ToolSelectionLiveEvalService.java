package com.berdachuk.medexpertmatch.llm.eval;

import com.berdachuk.medexpertmatch.core.util.LlmDateTimeContext;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/**
 * Runs live FunctionGemma tool-selection eval against a golden dataset.
 */
public class ToolSelectionLiveEvalService {

    private static final String ORCHESTRATOR_INSTRUCTIONS_RESOURCE =
            "/prompts/chat-agent-orchestrator-instructions.st";

    private final ChatModel chatModel;
    private final ToolSelectionPromptBuilder promptBuilder;
    private final FunctionGemmaToolCallParser parser;
    private final List<ToolCallback> evalToolCallbacks;

    public ToolSelectionLiveEvalService(
            ChatModel chatModel,
            ToolSelectionPromptBuilder promptBuilder,
            FunctionGemmaToolCallParser parser) {
        this.chatModel = chatModel;
        this.promptBuilder = promptBuilder;
        this.parser = parser;
        this.evalToolCallbacks = Arrays.asList(MethodToolCallbackProvider.builder()
                .toolObjects(new EvalToolStubs())
                .build()
                .getToolCallbacks());
    }

    public ToolSelectionLiveEvalReport evaluateGoldenDataset(String modelName, String label) throws IOException {
        return evaluateResolvedDataset(modelName, label, ToolSelectionDatasetPaths.customDatasetPathOrNull());
    }

    public ToolSelectionLiveEvalReport evaluateResolvedDataset(
            String modelName, String label, Path datasetPath) throws IOException {
        ToolSelectionGoldenDataset dataset = new ToolSelectionGoldenDataset();
        List<ToolSelectionGoldenCase> cases = datasetPath != null
                ? dataset.loadFile(datasetPath)
                : dataset.loadClasspathGolden();
        ToolSelectionGoldenDataset.requireMinimumSize(cases, 1);
        return evaluate(cases, modelName, label);
    }

    public ToolSelectionLiveEvalReport evaluate(
            List<ToolSelectionGoldenCase> cases, String modelName, String label) {
        String systemPrompt = LlmDateTimeContext.contextBlock() + "\n\n" + loadOrchestratorInstructions();
        List<ToolSelectionLiveEvalReport.CaseResult> results = new ArrayList<>();
        int passed = 0;

        for (ToolSelectionGoldenCase goldenCase : cases) {
            ToolSelectionLiveEvalReport.CaseResult caseResult = evaluateCase(systemPrompt, goldenCase);
            results.add(caseResult);
            if (caseResult.passed()) {
                passed++;
            }
        }

        double accuracy = cases.isEmpty() ? 0.0 : (double) passed / cases.size();
        return new ToolSelectionLiveEvalReport(
                label,
                modelName,
                Instant.now(),
                cases.size(),
                passed,
                accuracy,
                List.copyOf(results));
    }

    private ToolSelectionLiveEvalReport.CaseResult evaluateCase(
            String systemPrompt, ToolSelectionGoldenCase goldenCase) {
        String userPrompt = promptBuilder.buildUserPrompt(goldenCase);
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .toolCallbacks(evalToolCallbacks)
                .build();
        Prompt prompt = new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt)), options);

        ChatResponse response = chatModel.call(prompt);
        Optional<FunctionGemmaToolCallParser.ParsedToolCall> parsed = parser.parse(response);
        if (parsed.isEmpty() && response.getResult() != null && response.getResult().getOutput() != null) {
            parsed = parser.parseText(response.getResult().getOutput().getText());
        }

        String actualTool = parsed.map(FunctionGemmaToolCallParser.ParsedToolCall::toolName).orElse(null);
        Map<String, String> actualArgs = parsed.map(FunctionGemmaToolCallParser.ParsedToolCall::args).orElse(Map.of());
        boolean passed = matches(goldenCase, actualTool, actualArgs);

        return new ToolSelectionLiveEvalReport.CaseResult(
                goldenCase.scenario(),
                goldenCase.locale(),
                goldenCase.userMessage(),
                goldenCase.expectedTool(),
                actualTool,
                passed,
                actualArgs);
    }

    static boolean matches(ToolSelectionGoldenCase goldenCase, String actualTool, Map<String, String> actualArgs) {
        if (!goldenCase.expectsTool()) {
            return actualTool == null || actualTool.isBlank();
        }
        if (actualTool == null || !goldenCase.expectedTool().equals(actualTool)) {
            return false;
        }
        return FunctionGemmaToolCallParser.argsMatch(goldenCase.expectedArgs(), actualArgs);
    }

    private static String loadOrchestratorInstructions() {
        try (InputStream stream = ToolSelectionLiveEvalService.class.getResourceAsStream(ORCHESTRATOR_INSTRUCTIONS_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing resource: " + ORCHESTRATOR_INSTRUCTIONS_RESOURCE);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load orchestrator instructions", e);
        }
    }
}
