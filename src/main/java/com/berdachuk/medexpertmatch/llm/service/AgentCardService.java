package com.berdachuk.medexpertmatch.llm.service;

import java.util.List;
import java.util.Map;

/**
 * Builds an A2A-compatible agent discovery card (M15 Step 1 — no spring-ai-a2a pom yet).
 */
public interface AgentCardService {

    Map<String, Object> buildAgentCard(String baseUrl);
}
