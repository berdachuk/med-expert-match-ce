package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.core.service.HarnessPlanExportQuery;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class HarnessPlanExportQueryImpl implements HarnessPlanExportQuery {

    private final AgentPlanArtefactStore agentPlanArtefactStore;

    public HarnessPlanExportQueryImpl(AgentPlanArtefactStore agentPlanArtefactStore) {
        this.agentPlanArtefactStore = agentPlanArtefactStore;
    }

    @Override
    public Optional<Map<String, Object>> findPlanBySessionId(String sessionId) {
        return agentPlanArtefactStore.findBySessionId(sessionId).map(this::toExportMap);
    }

    private Map<String, Object> toExportMap(AgentPlanArtefact artefact) {
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("workflowType", artefact.workflowType().name());
        plan.put("caseId", artefact.caseId());
        plan.put("steps", artefact.steps());
        plan.put("acceptanceCriteria", artefact.acceptanceCriteria());
        return plan;
    }
}
