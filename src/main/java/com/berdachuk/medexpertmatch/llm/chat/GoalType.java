package com.berdachuk.medexpertmatch.llm.chat;

/**
 * High-level goals the AI chat should identify from user requests before routing.
 * Maps to harness workflow engines for execute-verify-critic pipelines.
 */
public enum GoalType {

    /** Find and rank doctors for a clinical case. Routes to {@code DoctorMatchWorkflowEngine}. */
    MATCH_DOCTORS,

    /** Analyze a case (ICD-10, urgency, clinical findings). Routes to LLM case analysis directly. */
    ANALYZE_CASE,

    /** Route a case to appropriate facilities. Routes to {@code RoutingWorkflowEngine}. */
    ROUTE_CASE,

    /** Assess urgency / triage intake. */
    TRIAGE_INTAKE,

    /** Search clinical evidence or literature. */
    SEARCH_EVIDENCE,

    /** Generate clinical recommendations for a matched doctor-case pair. */
    GENERATE_RECOMMENDATIONS,

    /** General informational question — no workflow engine needed. */
    GENERAL_QUESTION
}
