package com.berdachuk.medexpertmatch.llm.harness;

/**
 * Taxonomy of harness failures for metrics and workflow metadata (no PHI).
 */
public enum HarnessFailureReason {
    TOOL_OUTPUT_INVALID,
    POLICY_GATE_REJECTED,
    POLICY_VIOLATION,
    ITERATION_LIMIT,
    TOOL_SCOPE_VIOLATION,
    CONTEXT_BUILD_FAILED,
    WORKFLOW_FAILED
}
