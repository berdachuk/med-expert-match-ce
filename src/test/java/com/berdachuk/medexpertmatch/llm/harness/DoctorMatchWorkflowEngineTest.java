package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.llm.config.HarnessProperties;
import com.berdachuk.medexpertmatch.llm.harness.impl.AgentPlannerServiceImpl;
import com.berdachuk.medexpertmatch.llm.harness.impl.AgentResponseVerifierImpl;
import com.berdachuk.medexpertmatch.llm.harness.impl.CaseContextBundleServiceImpl;
import com.berdachuk.medexpertmatch.llm.harness.impl.InMemoryAgentPlanArtefactStore;
import com.berdachuk.medexpertmatch.llm.harness.impl.InMemoryHarnessWorkflowRunStore;
import com.berdachuk.medexpertmatch.llm.harness.impl.MedicalAgentPolicyGateServiceImpl;
import com.berdachuk.medexpertmatch.llm.harness.impl.MedicalConfidencePolicyServiceImpl;
import com.berdachuk.medexpertmatch.llm.config.MedicalConfidencePolicyProperties;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentLlmSupportService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.llm.tools.DoctorMatchingAgentTools;
import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;
import com.berdachuk.medexpertmatch.retrieval.repository.ConsultationMatchRepository;
import com.berdachuk.medexpertmatch.retrieval.service.MatchExplainabilityService;
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
        when(matchingTools.match_doctors_to_case(anyString(), anyInt(), any(), any(), any(), any()))
                .thenReturn(List.of());

        CaseContextBundleService bundleService = new CaseContextBundleServiceImpl(caseRepository);
        AgentPlannerService planner = new AgentPlannerServiceImpl(
                bundleService, new InMemoryAgentPlanArtefactStore());
        HarnessMetrics metrics = new HarnessMetrics(new SimpleMeterRegistry());
        MedicalAgentPolicyGateService policyGate = new MedicalAgentPolicyGateServiceImpl(
                HarnessProperties.defaults(), metrics);
        MedicalConfidencePolicyService confidencePolicy = new MedicalConfidencePolicyServiceImpl(
                MedicalConfidencePolicyProperties.defaults());

        DoctorMatchWorkflowEngine engine = new DoctorMatchWorkflowEngine(
                llmSupport,
                caseRepository,
                logStream,
                matchingTools,
                new ObjectMapper(),
                new AgentResponseVerifierImpl(),
                policyGate,
                confidencePolicy,
                bundleService,
                planner,
                HarnessProperties.defaults(),
                metrics,
                new InMemoryHarnessWorkflowRunStore(),
                mock(ApplicationEventPublisher.class),
                mock(ConsultationMatchRepository.class),
                mock(MatchExplainabilityService.class));

        MedicalAgentService.AgentResponse response = engine.execute(
                "6a1c68963a08e800010de68e",
                Map.of("sessionId", "test-session"));

        assertTrue(response.response().contains("not a substitute"));
        assertEquals("CLARIFY", response.metadata().get("policyAction"));
    }

    @Test
    @DisplayName("completes when matches pass verify and policyGate")
    void successPath() throws Exception {
        MedicalAgentLlmSupportService llmSupport = mock(MedicalAgentLlmSupportService.class);
        MedicalCaseRepository caseRepository = mock(MedicalCaseRepository.class);
        LogStreamService logStream = mock(LogStreamService.class);
        DoctorMatchingAgentTools matchingTools = mock(DoctorMatchingAgentTools.class);

        Doctor doctor = new Doctor("d1", "Dr. Lee", null, List.of("Cardiology"), List.of(), List.of(), false, null);
        DoctorMatch match = new DoctorMatch(doctor, 90.0, 1, "fit");
        when(llmSupport.analyzeCaseWithMedGemma(anyString())).thenReturn("{}");
        when(matchingTools.match_doctors_to_case(anyString(), anyInt(), any(), any(), any(), any()))
                .thenReturn(List.of(match));
        when(caseRepository.findById(anyString())).thenReturn(Optional.empty());
        when(llmSupport.interpretResultsWithMedGemma(anyString(), anyString(), any()))
                .thenReturn("Matched specialists for research use only, not a substitute for professional medical advice.");

        CaseContextBundleService bundleService = new CaseContextBundleServiceImpl(caseRepository);
        AgentPlannerService planner = new AgentPlannerServiceImpl(
                bundleService, new InMemoryAgentPlanArtefactStore());
        HarnessMetrics metrics = new HarnessMetrics(new SimpleMeterRegistry());
        MedicalAgentPolicyGateService policyGate = new MedicalAgentPolicyGateServiceImpl(
                HarnessProperties.defaults(), metrics);
        MedicalConfidencePolicyService confidencePolicy = new MedicalConfidencePolicyServiceImpl(
                MedicalConfidencePolicyProperties.defaults());

        DoctorMatchWorkflowEngine engine = new DoctorMatchWorkflowEngine(
                llmSupport,
                caseRepository,
                logStream,
                matchingTools,
                new ObjectMapper(),
                new AgentResponseVerifierImpl(),
                policyGate,
                confidencePolicy,
                bundleService,
                planner,
                HarnessProperties.defaults(),
                metrics,
                new InMemoryHarnessWorkflowRunStore(),
                mock(ApplicationEventPublisher.class),
                mock(ConsultationMatchRepository.class),
                mock(MatchExplainabilityService.class));

        MedicalAgentService.AgentResponse response = engine.execute(
                "6a1c68963a08e800010de68e",
                Map.of("sessionId", "test-session-2"));

        assertEquals(1, response.metadata().get("matchCount"));
        assertEquals(1, response.metadata().get("doctorMatchCount"));
        assertTrue(response.response().contains("not a substitute"));
    }

    @Test
    @DisplayName("CLARIFY with verified matches still returns interpreted doctor list and caveat")
    void clarifyWithMatchesReturnsInterpretedList() throws Exception {
        MedicalAgentLlmSupportService llmSupport = mock(MedicalAgentLlmSupportService.class);
        MedicalCaseRepository caseRepository = mock(MedicalCaseRepository.class);
        LogStreamService logStream = mock(LogStreamService.class);
        DoctorMatchingAgentTools matchingTools = mock(DoctorMatchingAgentTools.class);

        Doctor doctor = new Doctor("d1", "Dr. Lee", null, List.of("Neurology"), List.of(), List.of(), false, null);
        DoctorMatch match = new DoctorMatch(doctor, 46.0, 1, "borderline fit");
        when(llmSupport.analyzeCaseWithMedGemma(anyString())).thenReturn("{}");
        when(matchingTools.match_doctors_to_case(anyString(), anyInt(), any(), any(), any(), any()))
                .thenReturn(List.of(match));
        when(caseRepository.findById(anyString())).thenReturn(Optional.empty());
        when(llmSupport.interpretResultsWithMedGemma(anyString(), anyString(), any()))
                .thenReturn("1. Dr. Lee — Neurology (score 46). Not a substitute for professional medical advice.");

        CaseContextBundleService bundleService = new CaseContextBundleServiceImpl(caseRepository);
        AgentPlannerService planner = new AgentPlannerServiceImpl(
                bundleService, new InMemoryAgentPlanArtefactStore());
        HarnessMetrics metrics = new HarnessMetrics(new SimpleMeterRegistry());
        MedicalAgentPolicyGateService policyGate = new MedicalAgentPolicyGateServiceImpl(
                HarnessProperties.defaults(), metrics);
        MedicalConfidencePolicyService confidencePolicy = new MedicalConfidencePolicyServiceImpl(
                MedicalConfidencePolicyProperties.defaults());

        DoctorMatchWorkflowEngine engine = new DoctorMatchWorkflowEngine(
                llmSupport,
                caseRepository,
                logStream,
                matchingTools,
                new ObjectMapper(),
                new AgentResponseVerifierImpl(),
                policyGate,
                confidencePolicy,
                bundleService,
                planner,
                HarnessProperties.defaults(),
                metrics,
                new InMemoryHarnessWorkflowRunStore(),
                mock(ApplicationEventPublisher.class),
                mock(ConsultationMatchRepository.class),
                mock(MatchExplainabilityService.class));

        MedicalAgentService.AgentResponse response = engine.execute(
                "6a1c68963a08e800010de68e",
                Map.of("sessionId", "test-session-clarify"));

        assertEquals("CLARIFY", response.metadata().get("policyAction"));
        assertEquals("low_score", response.metadata().get("policyReason"));
        assertEquals(1, response.metadata().get("matchCount"));
        assertEquals("DONE", response.metadata().get("harnessState"));
        assertTrue(response.response().contains("below the recommended threshold"));
        assertTrue(response.response().contains("Dr. Lee"));
        assertTrue(response.response().contains("Matched Doctors (GraphRAG)"));
    }

    @Test
    @DisplayName("ESCALATE policy pauses at NEEDS_HUMAN when human adjudication enabled")
    void escalatePausesForHumanAdjudication() throws Exception {
        MedicalAgentLlmSupportService llmSupport = mock(MedicalAgentLlmSupportService.class);
        MedicalCaseRepository caseRepository = mock(MedicalCaseRepository.class);
        LogStreamService logStream = mock(LogStreamService.class);
        DoctorMatchingAgentTools matchingTools = mock(DoctorMatchingAgentTools.class);

        String caseId = "6a1c68963a08e800010de68e";
        Doctor doctor = new Doctor("d1", "Dr. Urgent", null, List.of("Neurology"), List.of(), List.of(), false, null);
        DoctorMatch match = new DoctorMatch(doctor, 46.0, 1, "urgent low");
        when(llmSupport.analyzeCaseWithMedGemma(anyString())).thenReturn("{}");
        when(matchingTools.match_doctors_to_case(anyString(), anyInt(), any(), any(), any(), any()))
                .thenReturn(List.of(match));
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(
                new MedicalCase(caseId, 40, "Seizure", "Seizure", "Epilepsy", List.of("G40.9"),
                        List.of(), UrgencyLevel.HIGH, "Neurology", CaseType.CONSULT_REQUEST, "Notes", null)));

        CaseContextBundleService bundleService = new CaseContextBundleServiceImpl(caseRepository);
        AgentPlannerService planner = new AgentPlannerServiceImpl(
                bundleService, new InMemoryAgentPlanArtefactStore());
        HarnessMetrics metrics = new HarnessMetrics(new SimpleMeterRegistry());
        MedicalAgentPolicyGateService policyGate = new MedicalAgentPolicyGateServiceImpl(
                HarnessProperties.defaults(), metrics);
        MedicalConfidencePolicyService confidencePolicy = new MedicalConfidencePolicyServiceImpl(
                MedicalConfidencePolicyProperties.defaults());
        HarnessProperties adjudicationProps = new HarnessProperties(
                true, true, 2, true, 1, 0, false, true, false, false, false, true);

        DoctorMatchWorkflowEngine engine = new DoctorMatchWorkflowEngine(
                llmSupport,
                caseRepository,
                logStream,
                matchingTools,
                new ObjectMapper(),
                new AgentResponseVerifierImpl(),
                policyGate,
                confidencePolicy,
                bundleService,
                planner,
                adjudicationProps,
                metrics,
                new InMemoryHarnessWorkflowRunStore(),
                mock(ApplicationEventPublisher.class),
                mock(ConsultationMatchRepository.class),
                mock(MatchExplainabilityService.class));

        MedicalAgentService.AgentResponse response = engine.execute(
                caseId,
                Map.of("sessionId", "test-session-escalate"));

        assertEquals("NEEDS_HUMAN", response.metadata().get("harnessState"));
        assertEquals("ESCALATE", response.metadata().get("policyAction"));
        assertEquals(true, response.metadata().get("pendingClinicianReview"));
        assertTrue(response.metadata().containsKey("harnessRunId"));
    }
}
