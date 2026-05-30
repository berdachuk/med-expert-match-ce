package com.berdachuk.medexpertmatch.llm.rest;

import com.berdachuk.medexpertmatch.llm.service.impl.AgentCardServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class A2AAgentCardTest {

    @Test
    @DisplayName("AgentCardServiceImpl builds expected skill ids")
    void serviceBuildsSkills() {
        var service = new AgentCardServiceImpl();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> skills = (List<Map<String, Object>>) service
                .buildAgentCard("http://test").get("skills");
        assertFalse(skills.isEmpty());
        assertTrue(skills.stream().anyMatch(s -> "evidence_search".equals(s.get("id"))));
        assertTrue(skills.stream().anyMatch(s -> "doctor_match".equals(s.get("id"))));
    }
}
