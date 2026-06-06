package com.berdachuk.medexpertmatch.llm.harness;

import org.springframework.stereotype.Component;

@Component
public class HarnessFailureBacklogSupport {

    private static final String TEMPLATE_PATH = ".agents/templates/harness-backlog-item.md";

    public static String buildBacklogMarkdown(
            String failureReason,
            String runId,
            HarnessWorkflowType workflowType) {
        String failureClass = mapFailureClass(failureReason);
        return """
                ## Symptom

                Harness run `%s` (%s) failed during automated workflow execution.

                ## Failure class

                - [x] %s

                ## Harness action

                Review `%s` and add a verification test.

                ## Verification

                `mvn test -Dtest=HarnessWorkflow*`
                """.formatted(runId, workflowType.name(), failureClass, TEMPLATE_PATH);
    }

    private static String mapFailureClass(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            return "Other";
        }
        return switch (failureReason) {
            case "TOOL_OUTPUT_INVALID" -> "Tool output invalid";
            case "ITERATION_LIMIT" -> "Iteration limit";
            case "POLICY_VIOLATION", "PHI_DETECTED", "DISCLAIMER_MISSING", "POLICY_GATE_REJECTED" ->
                    "Policy gate (disclaimer, PHI)";
            default -> "Other: " + failureReason;
        };
    }
}
