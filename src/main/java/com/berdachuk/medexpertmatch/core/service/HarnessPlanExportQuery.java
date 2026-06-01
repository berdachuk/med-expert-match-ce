package com.berdachuk.medexpertmatch.core.service;

import java.util.Map;
import java.util.Optional;

/**
 * Read-only query for harness planner artefacts to include in chat exports (no llm module dependency in chat).
 */
public interface HarnessPlanExportQuery {

    Optional<Map<String, Object>> findPlanBySessionId(String sessionId);
}
