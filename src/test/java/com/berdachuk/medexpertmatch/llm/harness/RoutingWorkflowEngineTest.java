package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.facility.domain.Facility;
import com.berdachuk.medexpertmatch.llm.config.HarnessProperties;
import com.berdachuk.medexpertmatch.llm.harness.impl.AgentPlannerServiceImpl;
import com.berdachuk.medexpertmatch.llm.harness.impl.AgentResponseVerifierImpl;
import com.berdachuk.medexpertmatch.llm.harness.impl.CaseContextBundleServiceImpl;
import com.berdachuk.medexpertmatch.llm.harness.impl.InMemoryAgentPlanArtefactStore;
import com.berdachuk.medexpertmatch.llm.harness.impl.InMemoryHarnessWorkflowRunStore;
import com.berdachuk.medexpertmatch.llm.harness.impl.MedicalAgentCriticServiceImpl;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentLlmSupportService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.llm.tools.RoutingAgentTools;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.retrieval.domain.FacilityMatch;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoutingWorkflowEngineTest {

    @Test
    @DisplayName("routing succeeds with facility matches and critic pass")
    void successPath() {
        MedicalAgentLlmSupportService llmSupport = mock(MedicalAgentLlmSupportService.class);
        LogStreamService logStream = mock(LogStreamService.class);
        RoutingAgentTools routingTools = mock(RoutingAgentTools.class);
        MedicalCaseRepository caseRepository = mock(MedicalCaseRepository.class);

        Facility facility = new Facility(
                "f1", "City Hospital", "HOSPITAL", "Boston", "MA", "US",
                null, null, List.of("ICU"), 100, 50);
        FacilityMatch match = new FacilityMatch(facility, 80.0, 1, "capacity fit");
        when(llmSupport.analyzeCaseWithMedGemma(anyString())).thenReturn("{}");
        when(routingTools.match_facilities_for_case(anyString(), anyInt(), any(), any(), any(), any()))
                .thenReturn(List.of(match));
        when(llmSupport.summarizeRoutingResults(anyString(), anyString()))
                .thenReturn("Route to City Hospital for research use only, not a substitute for professional medical advice.");

        CaseContextBundleService bundleService = new CaseContextBundleServiceImpl(caseRepository);
        AgentPlannerService planner = new AgentPlannerServiceImpl(bundleService, new InMemoryAgentPlanArtefactStore());
        HarnessMetrics metrics = new HarnessMetrics(new SimpleMeterRegistry());
        MedicalAgentCriticService critic = new MedicalAgentCriticServiceImpl(HarnessProperties.defaults(), metrics);

        RoutingWorkflowEngine engine = new RoutingWorkflowEngine(
                llmSupport, logStream, routingTools, new AgentResponseVerifierImpl(), critic,
                bundleService, planner, HarnessProperties.defaults(), metrics,
                new HarnessCheckpointSupport(new InMemoryHarnessWorkflowRunStore(), new com.fasterxml.jackson.databind.ObjectMapper()));

        MedicalAgentService.AgentResponse response = engine.execute(
                "6a1c68963a08e800010de68e",
                Map.of("sessionId", "route-test"));

        assertEquals(1, response.metadata().get("facilityMatchCount"));
        assertTrue(response.response().contains("not a substitute"));
    }
}
