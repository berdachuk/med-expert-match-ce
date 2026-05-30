package com.berdachuk.medexpertmatch.llm.service;

import java.util.Map;

/**
 * Lightweight A2A-style message bridge pending full spring-ai-a2a-server integration (M15).
 */
public interface A2AMessageService {

    Map<String, Object> sendMessage(Map<String, Object> request);
}
