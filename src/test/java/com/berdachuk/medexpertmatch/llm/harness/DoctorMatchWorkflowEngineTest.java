package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.llm.config.HarnessProperties;
import com.berdachuk.medexpertmatch.llm.harness.impl.AgentPlannerServiceImpl;
import com.berdachuk.medexpertmatch.llm.harness.impl.AgentResponseVerifierImpl;
import com.berdachuk.medexpertmatch.llm.harness.impl.CaseContextBundleServiceImpl;
import com.berdachuk.medexpertmatch.llm.harness.impl.InMemoryAgentPlanArtefactStore;
import com.berdachuk.medexpertmatch.llm.harness.impl.InMemoryHarnessWorkflowRunStore;
import com.berdachuk.medexpertmatch.llm.harness.impl.MedicalAgentCriticServiceImpl;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentLlmSupportService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.llm.tools.DoctorMatchingAgentTools;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DoctorMatchWorkflowEngineTest {

    @Test
    @DisplayName("returns safe response when verify fails with empty matches")
    void verifyFailureReturnsSafeResponse() throws Exception {
        MedicalAgentLlmSupportService llmSupport = mock(MedicalAgentLlmSupportService.class);
        MedicalCaseRepository caseRepository = mock(MedicalCaseRepository.class);
        LogStreamService logStream = mock(LogStreamService.class);
        DoctorMatchingAgentTools matchingTools = mock(DoctorMatchingAgentTools.class);
        when(llmSupport.analyzeCaseWithMedGemma(anyString())).thenReturn("{}");
        when(matchingTools.match_doctors_to_case(anyString(), anyInt(), any(), any(), any()))
                .thenReturn(List.of());

        CaseContextBundleService bundleService = new CaseContextBundleServiceImpl(caseRepository);
        AgentPlannerService planner = new AgentPlannerServiceImpl(
                bundleService, new InMemoryAgentPlanArtefactStore());
        HarnessMetrics metrics = new HarnessMetrics(new SimpleMeterRegistry());
        MedicalAgentCriticService critic = new MedicalAgentCriticServiceImpl(
                HarnessProperties.defaults(), metrics);

        DoctorMatchWorkflowEngine engine = new DoctorMatchWorkflowEngine(
                llmSupport,
                caseRepository,
                logStream,
                matchingTools,
                new ObjectMapper(),
                new AgentResponseVerifierImpl(),
                critic,
                bundleService,
                planner,
                HarnessProperties.defaults(),
                metrics,
                new InMemoryHarnessWorkflowRunStore(),
                mock(ApplicationEventPublisher.class));

        MedicalAgentService.AgentResponse response = engine.execute(
                "6a1c68963a08e800010de68e",
                Map.of("sessionId", "test-session"));

        assertTrue(response.response().contains("not a substitute"));
        Object reason = response.metadata().get("harnessFailureReason");
        assertTrue(HarnessFailureReason.TOOL_OUTPUT_INVALID.name().equals(reason)
                || HarnessFailureReason.ITERATION_LIMIT.name().equals(reason));
    }

    @Test
    @DisplayName("completes when matches pass verify and critic")
    void successPath() throws Exception {
        MedicalAgentLlmSupportService llmSupport = mock(MedicalAgentLlmSupportService.class);
        MedicalCaseRepository caseRepository = mock(MedicalCaseRepository.class);
        LogStreamService logStream = mock(LogStreamService.class);
        DoctorMatchingAgentTools matchingTools = mock(DoctorMatchingAgentTools.class);

        Doctor doctor = new Doctor("d1", "Dr. Lee", null, List.of("Cardiology"), List.of(), List.of(), false, null);
        DoctorMatch match = new DoctorMatch(doctor, 90.0, 1, "fit");
        when(llmSupport.analyzeCaseWithMedGemma(anyString())).thenReturn("{}");
        when(matchingTools.match_doctors_to_case(anyString(), anyInt(), any(), any(), any()))
                .thenReturn(List.of(match));
        when(caseRepository.findById(anyString())).thenReturn(Optional.empty());
        when(llmSupport.interpretResultsWithMedGemma(anyString(), anyString(), any()))
                .thenReturn("Matched specialists for research use only, not a substitute for professional medical advice.");

        CaseContextBundleService bundleService = new CaseContextBundleServiceImpl(caseRepository);
        AgentPlannerService planner = new AgentPlannerServiceImpl(
                bundleService, new InMemoryAgentPlanArtefactStore());
        HarnessMetrics metrics = new HarnessMetrics(new SimpleMeterRegistry());
        MedicalAgentCriticService critic = new MedicalAgentCriticServiceImpl(
                HarnessProperties.defaults(), metrics);

        DoctorMatchWorkflowEngine engine = new DoctorMatchWorkflowEngine(
                llmSupport,
                caseRepository,
                logStream,
                matchingTools,
                new ObjectMapper(),
                new AgentResponseVerifierImpl(),
                critic,
                bundleService,
                planner,
                HarnessProperties.defaults(),
                metrics,
                new InMemoryHarnessWorkflowRunStore(),
                mock(ApplicationEventPublisher.class));

        MedicalAgentService.AgentResponse response = engine.execute(
                "6a1c68963a08e800010de68e",
                Map.of("sessionId", "test-session-2"));

        assertEquals(1, response.metadata().get("matchCount"));
        assertTrue(response.response().contains("not a substitute"));
    }
}
