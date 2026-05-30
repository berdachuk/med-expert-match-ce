package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.llm.service.AgentCardService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AgentCardServiceImpl implements AgentCardService {

    @Override
    public Map<String, Object> buildAgentCard(String baseUrl) {
        return Map.of(
                "name", "MedExpertMatch",
                "description", "Medical expert recommendation and clinical evidence agent (PHI-safe, research use only)",
                "url", baseUrl,
                "version", "1.0.0",
                "protocolVersion", "0.2.0",
                "skills", List.of(
                        Map.of(
                                "id", "doctor_match",
                                "name", "Doctor Match",
                                "description", "Rank specialists for anonymized clinical cases using GraphRAG hybrid retrieval"),
                        Map.of(
                                "id", "evidence_search",
                                "name", "Evidence Search",
                                "description", "Retrieve PubMed literature and clinical guideline summaries")),
                "capabilities", Map.of(
                        "streaming", true,
                        "pushNotifications", false));
    }
}
