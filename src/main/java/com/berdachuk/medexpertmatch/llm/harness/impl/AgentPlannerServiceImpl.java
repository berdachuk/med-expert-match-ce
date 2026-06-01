package com.berdachuk.medexpertmatch.llm.harness.impl;

import com.berdachuk.medexpertmatch.llm.harness.AgentPlanArtefact;
import com.berdachuk.medexpertmatch.llm.harness.AgentPlanArtefactStore;
import com.berdachuk.medexpertmatch.llm.harness.AgentPlannerService;
import com.berdachuk.medexpertmatch.llm.harness.CaseContextBundle;
import com.berdachuk.medexpertmatch.llm.harness.CaseContextBundleService;
import com.berdachuk.medexpertmatch.llm.harness.CaseContextIntent;
import com.berdachuk.medexpertmatch.llm.harness.HarnessWorkflowType;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AgentPlannerServiceImpl implements AgentPlannerService {

    private final CaseContextBundleService caseContextBundleService;
    private final AgentPlanArtefactStore planArtefactStore;

    public AgentPlannerServiceImpl(
            CaseContextBundleService caseContextBundleService,
            AgentPlanArtefactStore planArtefactStore) {
        this.caseContextBundleService = caseContextBundleService;
        this.planArtefactStore = planArtefactStore;
    }

    @Override
    public AgentPlanArtefact buildPlan(String sessionId, String caseId, HarnessWorkflowType workflowType) {
        CaseContextIntent intent = switch (workflowType) {
            case ROUTING -> CaseContextIntent.ROUTE;
            case CASE_INTAKE -> CaseContextIntent.MATCH;
            default -> CaseContextIntent.MATCH;
        };
        CaseContextBundle bundle = caseContextBundleService.build(caseId, intent);
        List<String> steps = switch (workflowType) {
            case ROUTING -> List.of(
                    "Build context bundle (" + bundle.coreSections().size() + " core sections)",
                    "Analyze case for routing",
                    "Execute match_facilities_for_case",
                    "Verify facility matches",
                    "Summarize routing and run critic");
            case CASE_INTAKE -> List.of(
                    "Validate intake text",
                    "Persist anonymized case",
                    "Generate abstract and embedding",
                    "Delegate to doctor match harness");
            default -> List.of(
                    "Build context bundle (" + bundle.coreSections().size() + " core sections)",
                    "Analyze case with LLM",
                    "Execute match_doctors_to_case",
                    "Verify tool output",
                    "Interpret results and run critic");
        };
        List<String> acceptance = List.of(
                "At least one valid doctor match with name and score",
                "Response includes medical disclaimer",
                "No PHI in stored output");
        AgentPlanArtefact plan = new AgentPlanArtefact(
                sessionId,
                workflowType,
                caseId,
                steps,
                acceptance,
                Instant.now());
        planArtefactStore.save(plan);
        return plan;
    }
}
