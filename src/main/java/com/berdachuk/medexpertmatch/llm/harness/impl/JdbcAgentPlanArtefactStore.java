package com.berdachuk.medexpertmatch.llm.harness.impl;

import com.berdachuk.medexpertmatch.llm.harness.AgentPlanArtefact;
import com.berdachuk.medexpertmatch.llm.harness.AgentPlanArtefactJdbcRepository;
import com.berdachuk.medexpertmatch.llm.harness.AgentPlanArtefactStore;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JdbcAgentPlanArtefactStore implements AgentPlanArtefactStore {

    private final AgentPlanArtefactJdbcRepository repository;

    public JdbcAgentPlanArtefactStore(AgentPlanArtefactJdbcRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(AgentPlanArtefact artefact) {
        repository.upsert(artefact);
    }

    @Override
    public Optional<AgentPlanArtefact> findBySessionId(String sessionId) {
        return repository.findBySessionId(sessionId);
    }
}
