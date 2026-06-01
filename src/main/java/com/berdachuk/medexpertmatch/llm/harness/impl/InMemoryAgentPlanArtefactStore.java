package com.berdachuk.medexpertmatch.llm.harness.impl;

import com.berdachuk.medexpertmatch.llm.harness.AgentPlanArtefact;
import com.berdachuk.medexpertmatch.llm.harness.AgentPlanArtefactStore;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Non-Spring test double; production uses {@link JdbcAgentPlanArtefactStore}.
 */
public class InMemoryAgentPlanArtefactStore implements AgentPlanArtefactStore {

    private final ConcurrentHashMap<String, AgentPlanArtefact> bySession = new ConcurrentHashMap<>();

    @Override
    public void save(AgentPlanArtefact artefact) {
        bySession.put(artefact.sessionId(), artefact);
    }

    @Override
    public Optional<AgentPlanArtefact> findBySessionId(String sessionId) {
        return Optional.ofNullable(bySession.get(sessionId));
    }
}
