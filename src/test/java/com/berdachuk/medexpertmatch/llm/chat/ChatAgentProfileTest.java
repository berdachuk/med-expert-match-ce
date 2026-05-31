package com.berdachuk.medexpertmatch.llm.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatAgentProfileTest {

    @Test
    @DisplayName("All picker values resolve to a profile")
    void resolvesPickerValues() {
        List.of("auto", "triage-intake", "case-analyzer", "evidence-scout",
                        "specialist-matcher", "routing-planner", "network-analyst")
                .forEach(id -> assertTrue(ChatAgentProfile.fromAgentId(id).isPresent(), id));
    }

    @Test
    @DisplayName("Auto orchestrator exposes no direct skill subset")
    void autoIsOrchestrator() {
        ChatAgentProfile auto = ChatAgentProfile.fromAgentId("auto").orElseThrow();
        assertTrue(auto.orchestrator());
        assertTrue(auto.skills().isEmpty());
    }

    @Test
    @DisplayName("Keyword routing maps evidence questions to evidence-scout")
    void classifiesEvidenceIntent() {
        ChatAgentProfile profile = ChatAgentProfile.classifyIntent("Find PubMed evidence for hypertension treatment")
                .orElseThrow();
        assertEquals(ChatAgentProfile.EVIDENCE_SCOUT, profile);
        assertFalse(profile.orchestrator());
    }

    @Test
    @DisplayName("Specialist matcher skills match M14 plan")
    void specialistMatcherSkills() {
        ChatAgentProfile profile = ChatAgentProfile.SPECIALIST_MATCHER;
        assertEquals(List.of("doctor-matcher", "recommendation-engine"), profile.skills());
    }
}
