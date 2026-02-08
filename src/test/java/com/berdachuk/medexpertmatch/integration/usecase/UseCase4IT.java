package com.berdachuk.medexpertmatch.integration.usecase;

import com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience;
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
 * Integration test for Use Case 4: Network Analytics.
 * <p>
 * Flow:
 * 1. Administrator requests network analytics for a condition
 * 2. System calls POST /api/v1/agent/network-analytics
 * 3. Agent uses network-analyzer skill to query Apache AGE graph
 * 4. Agent aggregates metrics: top experts, case volumes, outcomes, facility performance
 * 5. Response: analytics report with insights and recommendations
 */
class UseCase4IT extends BaseIntegrationTest {

    @Autowired
    private MedicalAgentController medicalAgentController;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    @Autowired
    private ClinicalExperienceRepository clinicalExperienceRepository;

    private String testDoctorId;
    private String testCaseId;
    private String conditionCode;

    @BeforeEach
    void setUp() {
        // Clear test data
        clinicalExperienceRepository.deleteAll();
        medicalCaseRepository.deleteAll();
        doctorRepository.deleteAll();

        conditionCode = "I21.9"; // Acute MI

        // Create test doctor
        testDoctorId = IdGenerator.generateDoctorId();
        Doctor doctor = new Doctor(
                testDoctorId,
                "Dr. Cardiology Expert",
                "cardio@example.com",
                List.of("Cardiology"),
                List.of("Board Certified Cardiology"),
                List.of(),
                true,
                "AVAILABLE"
        );
        doctorRepository.insert(doctor);

        // Create test case
        testCaseId = medicalCaseRepository.insert(new MedicalCase(
                null,
                65,
                "Chest pain",
                "Chest pain, Elevated troponin",
                "Acute MI",
                List.of(conditionCode),
                List.of(),
                UrgencyLevel.HIGH,
                "Cardiology",
                CaseType.INPATIENT,
                "Test case for network analytics",
                null
        ));

        // Create clinical experience linking doctor to case
        ClinicalExperience experience = new ClinicalExperience(
                null, // Will be generated
                testDoctorId,
                testCaseId,
                List.of("Cardiac Catheterization"), // proceduresPerformed
                "HIGH", // complexityLevel
                "SUCCESS", // outcome
                List.of(), // complications
                5, // timeToResolution (days)
                5 // rating (1-5)
        );
        clinicalExperienceRepository.insert(experience);
    }

    @Test
    void testNetworkAnalytics() {
        // Use Case 4: Network Analytics
        ResponseEntity<MedicalAgentService.AgentResponse> response = medicalAgentController.networkAnalytics(
                Map.of("conditionCode", conditionCode)
        );

        // Verify response
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().response());
        assertFalse(response.getBody().response().isEmpty(), "Response should contain network analytics");

        // Response content may vary with agent/mock flow; ensure we got a result
        assertNotNull(response.getBody().response());
    }

    @Test
    void testNetworkAnalyticsWithTimeRange() {
        // Test with time range filter
        ResponseEntity<MedicalAgentService.AgentResponse> response = medicalAgentController.networkAnalytics(
                Map.of(
                        "conditionCode", conditionCode,
                        "startDate", "2024-01-01",
                        "endDate", "2024-12-31"
                )
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().response());
    }
}
