package com.berdachuk.medexpertmatch.llm.service;

import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Focused integration tests for extracted workflow-oriented agent services.
 */
class MedicalAgentWorkflowServicesIT extends BaseIntegrationTest {

    @Autowired
    private MedicalAgentDoctorMatchingWorkflowService doctorMatchingWorkflowService;

    @Autowired
    private MedicalAgentCaseAnalysisWorkflowService caseAnalysisWorkflowService;

    @Autowired
    private MedicalAgentRoutingWorkflowService routingWorkflowService;

    @Autowired
    private MedicalAgentRecommendationWorkflowService recommendationWorkflowService;

    @Autowired
    private MedicalAgentQueuePrioritizationWorkflowService queuePrioritizationWorkflowService;

    @Autowired
    private MedicalAgentNetworkAnalyticsWorkflowService networkAnalyticsWorkflowService;

    @Autowired
    private MedicalAgentCaseIntakeWorkflowService caseIntakeWorkflowService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    private String testCaseId;

    @BeforeEach
    void setUp() {
        medicalCaseRepository.deleteAll();
        doctorRepository.deleteAll();

        Doctor doctor = new Doctor(
                IdGenerator.generateDoctorId(),
                "Dr. Cardiology Expert",
                "cardio@test.com",
                List.of("Cardiology"),
                List.of("Board Certified Cardiology"),
                List.of(),
                true,
                "AVAILABLE"
        );
        doctorRepository.insert(doctor);

        MedicalCase medicalCase = new MedicalCase(
                null,
                55,
                "Chest pain and shortness of breath",
                "Chest pain, Shortness of breath, Elevated troponin",
                "Acute myocardial infarction",
                List.of("I21.9"),
                List.of(),
                UrgencyLevel.HIGH,
                "Cardiology",
                CaseType.INPATIENT,
                "Patient presents with acute MI requiring specialist consultation",
                null
        );
        testCaseId = medicalCaseRepository.insert(medicalCase);
    }

    @Test
    void doctorMatchingWorkflow_ShouldReturnHybridMetadata() {
        MedicalAgentService.AgentResponse response = doctorMatchingWorkflowService.matchDoctors(
                testCaseId,
                Map.of("sessionId", "workflow-match")
        );

        assertNotNull(response);
        assertNotNull(response.response());
        assertFalse(response.response().isBlank());
        assertEquals(Boolean.TRUE, response.metadata().get("hybridApproach"));
        assertEquals(testCaseId, response.metadata().get("caseId"));
    }

    @Test
    void caseAnalysisWorkflow_ShouldReturnEvidenceDrivenResponse() {
        MedicalAgentService.AgentResponse response = caseAnalysisWorkflowService.analyzeCase(
                testCaseId,
                Map.of("sessionId", "workflow-analyze")
        );

        assertNotNull(response);
        assertNotNull(response.response());
        assertFalse(response.response().isBlank());
        assertEquals(Boolean.TRUE, response.metadata().get("hybridApproach"));
        assertEquals(Boolean.TRUE, response.metadata().get("medgemmaUsed"));
    }

    @Test
    void routingWorkflow_ShouldReturnRoutingMetadata() {
        MedicalAgentService.AgentResponse response = routingWorkflowService.routeCase(
                testCaseId,
                Map.of("sessionId", "workflow-route")
        );

        assertNotNull(response);
        assertNotNull(response.response());
        assertFalse(response.response().isBlank());
        assertEquals(testCaseId, response.metadata().get("caseId"));
        assertEquals(Boolean.TRUE, response.metadata().get("hybridApproach"));
        assertTrue(response.metadata().containsKey("facilityMatchCount"));
    }

    @Test
    void recommendationWorkflow_ShouldUseDirectServiceInterface() {
        MedicalAgentService.AgentResponse response = recommendationWorkflowService.generateRecommendations(
                "recommendation-" + testCaseId,
                Map.of("caseId", testCaseId, "sessionId", "workflow-recommend")
        );

        assertNotNull(response);
        assertNotNull(response.response());
        assertFalse(response.response().isBlank());
        assertEquals(Boolean.TRUE, response.metadata().get("hybridApproach"));
        assertEquals(Boolean.TRUE, response.metadata().get("medgemmaUsed"));
    }

    @Test
    void queuePrioritizationWorkflow_ShouldReturnDeterministicOrderMetadata() {
        MedicalCase lowerUrgencyCase = new MedicalCase(
                null,
                44,
                "Mild headache",
                "Headache",
                "Migraine",
                List.of(),
                List.of(),
                UrgencyLevel.LOW,
                "Neurology",
                CaseType.CONSULT_REQUEST,
                "Lower urgency follow-up case",
                null
        );
        String lowerUrgencyCaseId = medicalCaseRepository.insert(lowerUrgencyCase);

        MedicalAgentService.AgentResponse response = queuePrioritizationWorkflowService.prioritizeConsults(
                Map.of("caseIds", List.of(testCaseId, lowerUrgencyCaseId), "sessionId", "workflow-prioritize")
        );

        assertNotNull(response);
        assertNotNull(response.response());
        assertFalse(response.response().isBlank());
        assertEquals(Boolean.TRUE, response.metadata().get("deterministicOrder"));
        assertTrue(response.response().contains(testCaseId));
        assertTrue(response.response().contains(lowerUrgencyCaseId));
    }

    @Test
    void networkAnalyticsWorkflow_ShouldReturnConditionMetadata() {
        MedicalAgentService.AgentResponse response = networkAnalyticsWorkflowService.networkAnalytics(
                Map.of("conditionCode", "I21.9", "sessionId", "workflow-analytics")
        );

        assertNotNull(response);
        assertNotNull(response.response());
        assertFalse(response.response().isBlank());
        assertEquals(List.of("I21.9"), response.metadata().get("conditionCodes"));
    }

    @Test
    void caseIntakeWorkflow_ShouldCreateCaseAndReturnMatchingResponse() {
        MedicalAgentService.AgentResponse response = caseIntakeWorkflowService.matchFromText(
                "Acute chest pain with radiation to the left arm",
                Map.of(
                        "patientAge", 61,
                        "symptoms", "Chest pain, sweating",
                        "urgencyLevel", "HIGH",
                        "sessionId", "workflow-intake"
                )
        );

        assertNotNull(response);
        assertNotNull(response.response());
        assertFalse(response.response().isBlank());
        assertNotNull(response.metadata().get("caseId"));
    }
}
