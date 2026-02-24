package com.berdachuk.medexpertmatch.integration.usecase;

import com.berdachuk.medexpertmatch.clinicalexperience.repository.ClinicalExperienceRepository;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.berdachuk.medexpertmatch.llm.rest.MedicalAgentController;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Use Case 5: Decision Support.
 * <p>
 * Flow:
 * 1. Physician requests case analysis for complex case
 * 2. System calls POST /api/v1/agent/analyze-case/{caseId}
 * 3. Agent uses case-analyzer, evidence-retriever, and recommendation-engine skills
 * 4. Agent provides differential diagnosis, risk assessment, and evidence-based recommendations
 * 5. System calls POST /api/v1/agent/recommendations/{matchId} for expert recommendations
 * 6. Response: comprehensive analysis with recommendations and expert matches
 */
class UseCase5IT extends BaseIntegrationTest {

    @Autowired
    private MedicalAgentController medicalAgentController;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    @Autowired
    private ClinicalExperienceRepository clinicalExperienceRepository;

    private String testCaseId;
    private String testDoctorId;

    @BeforeEach
    void setUp() {
        // Clear test data
        clinicalExperienceRepository.deleteAll();
        medicalCaseRepository.deleteAll();
        doctorRepository.deleteAll();

        // Create test doctor
        testDoctorId = IdGenerator.generateDoctorId();
        Doctor doctor = new Doctor(
                testDoctorId,
                "Dr. Decision Support Specialist",
                "support@example.com",
                List.of("Internal Medicine", "Critical Care"),
                List.of("Board Certified"),
                List.of(),
                true,
                "AVAILABLE"
        );
        doctorRepository.insert(doctor);

        // Create complex case for analysis
        testCaseId = medicalCaseRepository.insert(new MedicalCase(
                null,
                60,
                "Complex multi-system presentation",
                "Fever, Rash, Joint pain, Elevated inflammatory markers",
                "Unknown etiology",
                List.of("R50.9", "M79.3"),
                List.of(),
                UrgencyLevel.HIGH,
                "Internal Medicine",
                CaseType.INPATIENT,
                "Complex case requiring differential diagnosis and evidence-based recommendations",
                null
        ));
    }

    @Test
    void testCaseAnalysis() {
        // Use Case 5: Decision Support - Case Analysis
        ResponseEntity<MedicalAgentService.AgentResponse> response = medicalAgentController.analyzeCaseSync(
                testCaseId,
                Map.of()
        );

        // Verify response
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().response());
        assertFalse(response.getBody().response().isEmpty(), "Response should contain case analysis");

        // Verify that response mentions analysis or diagnosis
        String responseText = response.getBody().response().toLowerCase();
        assertTrue(responseText.contains("analysis") ||
                        responseText.contains("diagnosis") ||
                        responseText.contains("recommendation") ||
                        responseText.contains("evidence"),
                "Response should reference case analysis or recommendations");
    }

    @Test
    void testGenerateRecommendations() {
        // First analyze the case
        ResponseEntity<MedicalAgentService.AgentResponse> analysisResponse = medicalAgentController.analyzeCaseSync(
                testCaseId,
                Map.of()
        );
        assertNotNull(analysisResponse.getBody());

        // Then generate recommendations (using a mock matchId)
        String matchId = "test-match-id";
        ResponseEntity<MedicalAgentService.AgentResponse> recommendationsResponse = medicalAgentController.generateRecommendations(
                matchId,
                Map.of()
        );

        // Verify response
        assertNotNull(recommendationsResponse);
        assertEquals(HttpStatus.OK, recommendationsResponse.getStatusCode());
        assertNotNull(recommendationsResponse.getBody());
        assertNotNull(recommendationsResponse.getBody().response());
    }
}
