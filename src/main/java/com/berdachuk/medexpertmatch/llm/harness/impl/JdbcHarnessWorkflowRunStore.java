package com.berdachuk.medexpertmatch.llm.harness.impl;

import com.berdachuk.medexpertmatch.llm.harness.DoctorMatchWorkflowState;
import com.berdachuk.medexpertmatch.llm.harness.HarnessWorkflowRun;
import com.berdachuk.medexpertmatch.llm.harness.HarnessWorkflowRunJdbcRepository;
import com.berdachuk.medexpertmatch.llm.harness.HarnessWorkflowRunStore;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JdbcHarnessWorkflowRunStore implements HarnessWorkflowRunStore {

    private final HarnessWorkflowRunJdbcRepository repository;

    public JdbcHarnessWorkflowRunStore(HarnessWorkflowRunJdbcRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(HarnessWorkflowRun run) {
        repository.upsert(run);
    }

    @Override
    public Optional<HarnessWorkflowRun> findById(String runId) {
        return repository.findById(runId);
    }

    @Override
    public void updateState(String runId, DoctorMatchWorkflowState state) {
        repository.updateState(runId, state);
    }
}
