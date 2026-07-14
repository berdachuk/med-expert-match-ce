package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.llm.agent.AgentSessionBranches;
import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionService;

import java.time.Instant;
import java.util.Map;

/**
 * Appends a compact non-PHI harness outcome line to {@link SessionService} for follow-up chat turns (M141).
 */
public final class HarnessSessionSummary {

    private HarnessSessionSummary() {}

    public static String format(GoalType goalType, String caseId, Map<String, Object> metadata) {
        String goal = goalType != null ? goalType.name() : "UNKNOWN";
        String safeCaseId = caseId != null && !caseId.isBlank() ? caseId : "n/a";
        String policy = metadataValue(metadata, "policyAction", "harnessState");
        int matchCount = matchCount(metadata, goalType);
        if (goalType == GoalType.ANALYZE_CASE) {
            return "[Harness] " + goal + " completed · caseId=" + safeCaseId + " · policy=" + policy;
        }
        return "[Harness] " + goal + " completed · caseId=" + safeCaseId
                + " · " + matchCount + " matches · policy=" + policy;
    }

    public static void append(SessionService sessionService, String sessionId,
                              GoalType goalType, String caseId, Map<String, Object> metadata) {
        if (sessionService == null || sessionId == null || sessionId.isBlank()) {
            return;
        }
        try {
            sessionService.appendEvent(SessionEvent.builder()
                    .sessionId(sessionId)
                    .timestamp(Instant.now())
                    .message(new AssistantMessage(format(goalType, caseId, metadata)))
                    .branch(AgentSessionBranches.ORCHESTRATOR)
                    .metadata(Map.of(SessionEvent.METADATA_SYNTHETIC, true))
                    .build());
        } catch (Exception ignored) {
            // best effort — chat persistence remains source of truth
        }
    }

    private static int matchCount(Map<String, Object> metadata, GoalType goalType) {
        if (metadata == null || metadata.isEmpty()) {
            return 0;
        }
        String key = goalType == GoalType.ROUTE_CASE ? "facilityMatchCount" : "doctorMatchCount";
        Object count = metadata.get(key);
        if (count instanceof Number number) {
            return number.intValue();
        }
        if (count instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static String metadataValue(Map<String, Object> metadata, String... keys) {
        if (metadata == null) {
            return "n/a";
        }
        for (String key : keys) {
            Object value = metadata.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value);
            }
        }
        return "n/a";
    }
}
