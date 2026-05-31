package com.berdachuk.medexpertmatch.llm.tool;

import com.berdachuk.medexpertmatch.llm.chat.ChatAgentProfile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Strips agent/skill tokens mistakenly passed as {@code preferredSpecialties} by tool-calling models.
 */
public final class MatchToolParameterSanitizer {

    private static final Set<String> KNOWN_AGENT_AND_SKILL_TOKENS = buildKnownTokens();

    private MatchToolParameterSanitizer() {
    }

    public static List<String> sanitizePreferredSpecialties(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        List<String> kept = new ArrayList<>();
        for (String specialty : raw) {
            if (isPlausibleMedicalSpecialty(specialty)) {
                kept.add(specialty.trim());
            }
        }
        return kept.isEmpty() ? null : List.copyOf(kept);
    }

    /**
     * When every preferred specialty was an agent/skill token, drop telehealth filtering too.
     */
    public static Boolean sanitizeRequireTelehealth(List<String> rawPreferred, Boolean requireTelehealth) {
        if (rawPreferred == null || rawPreferred.isEmpty()) {
            return requireTelehealth;
        }
        boolean anyInvalid = rawPreferred.stream().anyMatch(value -> !isPlausibleMedicalSpecialty(value));
        if (anyInvalid) {
            return null;
        }
        return requireTelehealth;
    }

    static boolean isPlausibleMedicalSpecialty(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (KNOWN_AGENT_AND_SKILL_TOKENS.contains(normalized)) {
            return false;
        }
        if (normalized.equals("ai specialist") || normalized.equals("clinical specialist")) {
            return false;
        }
        if (normalized.endsWith("-matcher")
                || normalized.endsWith("-analyzer")
                || normalized.endsWith("-scout")
                || normalized.endsWith("-planner")
                || normalized.endsWith("-intake")) {
            return false;
        }
        if (normalized.contains("-") && !normalized.contains(" ")) {
            return false;
        }
        return true;
    }

    private static Set<String> buildKnownTokens() {
        Set<String> tokens = new HashSet<>();
        for (ChatAgentProfile profile : ChatAgentProfile.values()) {
            tokens.add(profile.agentId().toLowerCase(Locale.ROOT));
            profile.skills().forEach(skill -> tokens.add(skill.toLowerCase(Locale.ROOT)));
        }
        tokens.add("specialty-matcher");
        tokens.add("clinical-guideline");
        tokens.add("evidence-retriever");
        tokens.add("recommendation-engine");
        tokens.add("network-analyzer");
        tokens.add("routing-planner");
        tokens.add("case-analyzer");
        tokens.add("clinical-advisor");
        tokens.add("triage");
        return Set.copyOf(tokens);
    }
}
