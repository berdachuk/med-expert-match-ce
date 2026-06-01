package com.berdachuk.medexpertmatch.llm.harness;

import java.util.Optional;

public interface HarnessWorkflowRunStore {

    void save(HarnessWorkflowRun run);

    Optional<HarnessWorkflowRun> findById(String runId);

    void updateState(String runId, DoctorMatchWorkflowState state);
}
