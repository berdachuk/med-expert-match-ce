package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.core.util.IdentifierHasher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class HarnessWorkflowRunQueryService {

    private final HarnessWorkflowRunStore runStore;

    public HarnessWorkflowRunQueryService(HarnessWorkflowRunStore runStore) {
        this.runStore = runStore;
    }

    public List<Map<String, Object>> listAwaitingHumanReview(int limit) {
        return runStore.findByState(DoctorMatchWorkflowState.NEEDS_HUMAN, limit).stream()
                .map(this::toView)
                .toList();
    }

    private Map<String, Object> toView(HarnessWorkflowRun run) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("runId", run.runId());
        view.put("sessionIdHash", IdentifierHasher.sha256Hex(run.sessionId()));
        view.put("caseId", run.caseId());
        view.put("workflowType", run.workflowType().name());
        view.put("state", run.state().name());
        view.put("updatedAt", run.updatedAt() != null ? run.updatedAt().toString() : Instant.now().toString());
        return view;
    }
}
