package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.llm.config.HarnessProperties;
import com.berdachuk.medexpertmatch.llm.harness.impl.AgentPlannerServiceImpl;
import com.berdachuk.medexpertmatch.llm.harness.impl.AgentResponseVerifierImpl;
import com.berdachuk.medexpertmatch.llm.harness.impl.CaseContextBundleServiceImpl;
import com.berdachuk.medexpertmatch.llm.harness.impl.InMemoryAgentPlanArtefactStore;
import com.berdachuk.medexpertmatch.llm.harness.impl.InMemoryHarnessWorkflowRunStore;
import com.berdachuk.medexpertmatch.llm.config.MedicalConfidencePolicyProperties;
import com.berdachuk.medexpertmatch.llm.harness.impl.MedicalConfidencePolicyServiceImpl;
import com.berdachuk.medexpertmatch.llm.harness.impl.MedicalAgentPolicyGateServiceImpl;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentLlmSupportService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.llm.tools.DoctorMatchingAgentTools;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;
import com.berdachuk.medexpertmatch.retrieval.repository.ConsultationMatchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DoctorMatchWorkflowEngineFollowUpTest {

    private static final String CASE_ID = "6a23f05200155d711484cf69";

    @Test
    @DisplayName("excludePreviouslyMatched loads prior doctor IDs and passes them to match tool")
    void excludePreviouslyMatchedPassesExcludedDoctorIds() throws Exception {
        MedicalAgentLlmSupportService llmSupport = mock(MedicalAgentLlmSupportService.class);
        MedicalCaseRepository caseRepository = mock(MedicalCaseRepository.class);
        LogStreamService logStream = mock(LogStreamService.class);
        DoctorMatchingAgentTools matchingTools = mock(DoctorMatchingAgentTools.class);
        ConsultationMatchRepository consultationMatchRepository = mock(ConsultationMatchRepository.class);

        Doctor doctor = new Doctor("doc-2", "Dr. Two", null, List.of("Cardiology"), List.of(), List.of(), false, null);
        DoctorMatch match = new DoctorMatch(doctor, 75.0, 1, "alternate fit");

        when(consultationMatchRepository.findDoctorIdsByCaseId(CASE_ID)).thenReturn(List.of("doc-1"));
        when(llmSupport.analyzeCaseWithMedGemma(anyString())).thenReturn("{}");
        when(matchingTools.match_doctors_to_case(
                eq(CASE_ID), anyInt(), isNull(), isNull(), isNull(), eq(List.of("doc-1"))))
                .thenReturn(List.of(match));
        when(caseRepository.findById(anyString())).thenReturn(Optional.empty());
        when(llmSupport.interpretResultsWithMedGemma(anyString(), anyString(), any()))
                .thenReturn("Additional specialists for research use only, not a substitute for professional medical advice.");

        DoctorMatchWorkflowEngine engine = buildEngine(
                llmSupport, caseRepository, logStream, matchingTools, consultationMatchRepository);

        engine.execute(CASE_ID, Map.of(
                "sessionId", "test-session",
                "excludePreviouslyMatched", true));

        verify(consultationMatchRepository).findDoctorIdsByCaseId(CASE_ID);
        verify(matchingTools).match_doctors_to_case(
                eq(CASE_ID), anyInt(), isNull(), isNull(), isNull(), eq(List.of("doc-1")));
    }

    @Test
    @DisplayName("success metadata exposes doctorMatchCount for harness progress UI")
    void successMetadataIncludesDoctorMatchCount() throws Exception {
        MedicalAgentLlmSupportService llmSupport = mock(MedicalAgentLlmSupportService.class);
        MedicalCaseRepository caseRepository = mock(MedicalCaseRepository.class);
        LogStreamService logStream = mock(LogStreamService.class);
        DoctorMatchingAgentTools matchingTools = mock(DoctorMatchingAgentTools.class);
        ConsultationMatchRepository consultationMatchRepository = mock(ConsultationMatchRepository.class);

        Doctor doctor = new Doctor("doc-1", "Dr. One", null, List.of("Cardiology"), List.of(), List.of(), false, null);
        DoctorMatch match = new DoctorMatch(doctor, 90.0, 1, "fit");

        when(llmSupport.analyzeCaseWithMedGemma(anyString())).thenReturn("{}");
        when(matchingTools.match_doctors_to_case(anyString(), anyInt(), any(), any(), any(), anyList()))
                .thenReturn(List.of(match));
        when(caseRepository.findById(anyString())).thenReturn(Optional.empty());
        when(llmSupport.interpretResultsWithMedGemma(anyString(), anyString(), any()))
                .thenReturn("Matched specialists for research use only, not a substitute for professional medical advice.");

        DoctorMatchWorkflowEngine engine = buildEngine(
                llmSupport, caseRepository, logStream, matchingTools, consultationMatchRepository);

        MedicalAgentService.AgentResponse response = engine.execute(
                CASE_ID, Map.of("sessionId", "test-session-2"));

        assertEquals(1, response.metadata().get("doctorMatchCount"));
        assertEquals(1, response.metadata().get("matchCount"));
    }

    private static DoctorMatchWorkflowEngine buildEngine(
            MedicalAgentLlmSupportService llmSupport,
            MedicalCaseRepository caseRepository,
            LogStreamService logStream,
            DoctorMatchingAgentTools matchingTools,
            ConsultationMatchRepository consultationMatchRepository) {
        CaseContextBundleService bundleService = new CaseContextBundleServiceImpl(caseRepository);
        AgentPlannerService planner = new AgentPlannerServiceImpl(
                bundleService, new InMemoryAgentPlanArtefactStore());
        HarnessMetrics metrics = new HarnessMetrics(new SimpleMeterRegistry());
        MedicalAgentPolicyGateService policyGate = new MedicalAgentPolicyGateServiceImpl(
                HarnessProperties.defaults(), metrics);
        MedicalConfidencePolicyService confidencePolicy = new MedicalConfidencePolicyServiceImpl(
                MedicalConfidencePolicyProperties.defaults());

        return new DoctorMatchWorkflowEngine(
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
                consultationMatchRepository);
    }
}
