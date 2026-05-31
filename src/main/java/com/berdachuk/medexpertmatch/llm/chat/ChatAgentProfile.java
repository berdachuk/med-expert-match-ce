package com.berdachuk.medexpertmatch.llm.chat;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Maps AI Chat picker values to skill subsets and orchestration behavior (M14).
 */
public enum ChatAgentProfile {

    AUTO("auto", List.of(), true),
    TRIAGE_INTAKE("triage-intake", List.of("triage"), false),
    CASE_ANALYZER("case-analyzer", List.of("case-analyzer", "clinical-advisor"), false),
    EVIDENCE_SCOUT("evidence-scout", List.of("evidence-retriever", "clinical-guideline"), false),
    SPECIALIST_MATCHER("specialist-matcher", List.of("doctor-matcher", "recommendation-engine"), false),
    ROUTING_PLANNER("routing-planner", List.of("routing-planner"), false),
    NETWORK_ANALYST("network-analyst", List.of("network-analyzer"), false);

    private final String agentId;
    private final List<String> skills;
    private final boolean orchestrator;

    ChatAgentProfile(String agentId, List<String> skills, boolean orchestrator) {
        this.agentId = agentId;
        this.skills = List.copyOf(skills);
        this.orchestrator = orchestrator;
    }

    public String agentId() {
        return agentId;
    }

    public List<String> skills() {
        return skills;
    }

    public boolean orchestrator() {
        return orchestrator;
    }

    public static Optional<ChatAgentProfile> fromAgentId(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.of(AUTO);
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(p -> p.agentId.equals(normalized))
                .findFirst();
    }

    /**
     * Lightweight keyword routing for Auto mode when the user message strongly signals one domain.
     */
    public static Optional<ChatAgentProfile> classifyIntent(String message) {
        if (message == null || message.isBlank()) {
            return Optional.empty();
        }
        String lower = message.toLowerCase(Locale.ROOT);
        if (containsAny(lower, "pubmed", "evidence", "guideline", "literature", "grade")) {
            return Optional.of(EVIDENCE_SCOUT);
        }
        if (containsAny(lower, "match doctor", "find specialist", "recommend doctor", "expert match")) {
            return Optional.of(SPECIALIST_MATCHER);
        }
        if (containsAny(lower, "route", "facility", "referral", "geographic")) {
            return Optional.of(ROUTING_PLANNER);
        }
        if (containsAny(lower, "network", "graph", "collaboration", "centrality")) {
            return Optional.of(NETWORK_ANALYST);
        }
        if (containsAny(lower, "urgency", "triage", "intake", "red flag", "priority")) {
            return Optional.of(TRIAGE_INTAKE);
        }
        if (containsAny(lower, "analyze case", "icd", "diagnosis hint", "complexity", "symptom")) {
            return Optional.of(CASE_ANALYZER);
        }
        return Optional.empty();
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
