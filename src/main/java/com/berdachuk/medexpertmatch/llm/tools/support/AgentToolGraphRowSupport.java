package com.berdachuk.medexpertmatch.llm.tools.support;

import java.util.Map;

/**
 * Helpers for Apache AGE Cypher result rows with varying column aliases.
 */
public final class AgentToolGraphRowSupport {

    private AgentToolGraphRowSupport() {
    }

    public static Object firstPresent(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            if (row.containsKey(key)) {
                return row.get(key);
            }
        }
        return null;
    }
}
