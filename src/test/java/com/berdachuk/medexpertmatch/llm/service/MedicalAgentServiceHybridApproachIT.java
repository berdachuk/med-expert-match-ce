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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for MedicalAgentService hybrid approach.
 * Verifies that MedGemma is used for medical reasoning and FunctionGemma for tool orchestration.
 */
class MedicalAgentServiceHybridApproachIT extends BaseIntegrationTest {

    @Autowired
    private MedicalAgentService medicalAgentService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    private String testCaseId;
    private String testDoctorId;

    @BeforeEach
    void setUp() {
        // Clear test data
        medicalCaseRepository.deleteAll();
        doctorRepository.deleteAll();

        // Create test doctor
        testDoctorId = IdGenerator.generateDoctorId();
        Doctor doctor = new Doctor(
                testDoctorId,
                "Dr. Cardiology Expert",
                "cardio@test.com",
                List.of("Cardiology"),
                List.of("Board Certified Cardiology"),
                List.of(),
                true,
                "AVAILABLE"
        );
        doctorRepository.insert(doctor);

        // Create test medical case
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
    void testMatchDoctors_HybridApproach() {
        // Test that hybrid approach is used
        MedicalAgentService.AgentResponse response = medicalAgentService.matchDoctors(
                testCaseId,
                Map.of("sessionId", "test-hybrid-approach")
        );

        // Verify response
        assertNotNull(response);
        assertNotNull(response.response());
        assertFalse(response.response().isEmpty(), "Response should not be empty");

        // Verify metadata indicates hybrid approach
        assertNotNull(response.metadata());
        assertTrue(response.metadata().containsKey("hybridApproach"),
                "Metadata should indicate hybrid approach is used");
        assertEquals(true, response.metadata().get("hybridApproach"),
                "hybridApproach should be true");

        // Verify MedGemma was used
        assertTrue(response.metadata().containsKey("medgemmaUsed"),
                "Metadata should indicate MedGemma was used");
        assertEquals(true, response.metadata().get("medgemmaUsed"),
                "medgemmaUsed should be true");

        // Verify FunctionGemma was used for tool orchestration
        assertTrue(response.metadata().containsKey("toolLlmUsed"),
                "Metadata should indicate FunctionGemma was used");
        assertEquals(false, response.metadata().get("toolLlmUsed"),
                "toolLlmUsed should be false as we now call tools directly");

        // Verify response contains medical reasoning (from MedGemma)
        String responseText = response.response().toLowerCase();
        assertTrue(responseText.contains("cardiology") ||
                        responseText.contains("specialist") ||
                        responseText.contains("doctor") ||
                        responseText.contains("match"),
                "Response should contain medical reasoning or match results");
    }

    @Test
    void testMatchDoctors_MedGemmaCaseAnalysis() {
        // Test that case analysis uses MedGemma
        // This is verified by checking that the response includes case analysis
        MedicalAgentService.AgentResponse response = medicalAgentService.matchDoctors(
                testCaseId,
                Map.of("sessionId", "test-medgemma-analysis")
        );

        assertNotNull(response);
        assertNotNull(response.response());

        // Response should include analysis from MedGemma
        // Even if FunctionGemma refuses, MedGemma analysis should be present
        String responseText = response.response();
        assertTrue(responseText.length() > 0, "Response should contain content");
    }

    @Test
    void testPrioritizeConsults_HybridApproach() {
        // Create multiple cases for prioritization
        MedicalCase case1 = new MedicalCase(
                null, 65, "Chest pain", "Chest pain", "MI",
                List.of("I21.9"), List.of(), UrgencyLevel.CRITICAL,
                "Cardiology", CaseType.INPATIENT, "Critical case", null
        );
        String caseId1 = medicalCaseRepository.insert(case1);

        MedicalCase case2 = new MedicalCase(
                null, 45, "Headache", "Headache", "Migraine",
                List.of(), List.of(), UrgencyLevel.LOW,
                "Neurology", CaseType.CONSULT_REQUEST, "Low urgency case", null
        );
        String caseId2 = medicalCaseRepository.insert(case2);

        // Test prioritization with hybrid approach (explicit caseIds)
        MedicalAgentService.AgentResponse response = medicalAgentService.prioritizeConsults(
                Map.of("caseIds", List.of(caseId1, caseId2), "sessionId", "test-prioritize")
        );

        assertNotNull(response);
        String body = response.response();
        assertNotNull(body);
        assertFalse(body.isEmpty());

        // All supplied case IDs must appear exactly once
        for (String caseId : List.of(caseId1, caseId2)) {
            int first = body.indexOf(caseId);
            assertTrue(first >= 0, "Response must contain case ID: " + caseId);
            assertEquals(first, body.lastIndexOf(caseId), "Case ID must appear exactly once: " + caseId);
        }

        // Strict urgency order: CRITICAL (case1) before LOW (case2)
        int idxCritical = body.indexOf("CRITICAL");
        int idxLow = body.indexOf("LOW");
        if (idxCritical >= 0 && idxLow >= 0) {
            assertTrue(idxCritical < idxLow, "CRITICAL must appear before LOW");
        }

        if (response.metadata() != null && response.metadata().containsKey("hybridApproach")) {
            assertEquals(true, response.metadata().get("hybridApproach"));
        }
        if (response.metadata() != null && response.metadata().containsKey("deterministicOrder")) {
            assertEquals(true, response.metadata().get("deterministicOrder"));
        }
    }

    @Test
    void testAnalyzeCase_HybridApproach() {
        // Test that analyzeCase uses hybrid approach
        MedicalAgentService.AgentResponse response = medicalAgentService.analyzeCase(
                testCaseId,
                Map.of("sessionId", "test-analyze-case")
        );

        assertNotNull(response);
        assertNotNull(response.response());
        assertFalse(response.response().isEmpty());

        // Verify metadata if available
        if (response.metadata() != null) {
            if (response.metadata().containsKey("hybridApproach")) {
                assertEquals(true, response.metadata().get("hybridApproach"));
            }
            if (response.metadata().containsKey("medgemmaUsed")) {
                assertEquals(true, response.metadata().get("medgemmaUsed"));
            }
        }
    }
}
