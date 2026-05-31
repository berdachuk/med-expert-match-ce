package com.berdachuk.medexpertmatch.llm.domain;

import java.util.Map;

/**
 * A2A skill descriptor for federation registry (M22).
 */
public record A2aSkillDescriptor(
        String id,
        String description,
        Map<String, Object> inputSchema
) {
}
