package com.berdachuk.medexpertmatch.integration.usecase;

import com.berdachuk.medexpertmatch.clinicalexperience.repository.ClinicalExperienceRepository;
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
 * Integration test for Use Case 3: Consultation Queue Prioritization.
 * <p>
 * Flow:
 * 1. Multiple consultation requests accumulate in queue
 * 2. System calls POST /api/v1/agent/prioritize-consults
 * 3. Agent uses case-analyzer to assess urgency, complexity, and resource needs
 * 4. Agent returns prioritized queue with scores and rationales
 */
class UseCase3IT extends BaseIntegrationTest {

    @Autowired
    private MedicalAgentController medicalAgentController;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private ClinicalExperienceRepository clinicalExperienceRepository;

    private String criticalCaseId;
    private String highUrgencyCaseId;
    private String mediumUrgencyCaseId;
    private String lowUrgencyCaseId;

    @BeforeEach
    void setUp() {
        // Clear test data
        clinicalExperienceRepository.deleteAll();
        medicalCaseRepository.deleteAll();
        doctorRepository.deleteAll();

        // Create multiple cases with different urgency levels
        criticalCaseId = medicalCaseRepository.insert(new MedicalCase(
                null,
                70,
                "Acute stroke symptoms",
                "Facial droop, Arm weakness, Speech difficulty",
                "Acute stroke",
                List.of("I63.9"),
                List.of(),
                UrgencyLevel.CRITICAL,
                "Neurology",
                CaseType.CONSULT_REQUEST,
                "Time-sensitive stroke case requiring immediate specialist consultation",
                null
        ));

        highUrgencyCaseId = medicalCaseRepository.insert(new MedicalCase(
                null,
                65,
                "Chest pain",
                "Chest pain, Elevated troponin",
                "Acute MI",
                List.of("I21.9"),
                List.of(),
                UrgencyLevel.HIGH,
                "Cardiology",
                CaseType.CONSULT_REQUEST,
                "High priority cardiac case",
                null
        ));

        mediumUrgencyCaseId = medicalCaseRepository.insert(new MedicalCase(
                null,
                50,
                "Follow-up consultation",
                "Stable condition, routine follow-up",
                "Stable condition",
                List.of("Z00.00"),
                List.of(),
                UrgencyLevel.MEDIUM,
                "Internal Medicine",
                CaseType.CONSULT_REQUEST,
                "Routine follow-up consultation",
                null
        ));

        lowUrgencyCaseId = medicalCaseRepository.insert(new MedicalCase(
                null,
                40,
                "Preventive care",
                "Annual checkup",
                "Preventive care",
                List.of("Z00.00"),
                List.of(),
                UrgencyLevel.LOW,
                "Primary Care",
                CaseType.CONSULT_REQUEST,
                "Low priority preventive care",
                null
        ));
    }

    @Test
    void testQueuePrioritization() {
        // Use Case 3: Consultation Queue Prioritization
        ResponseEntity<MedicalAgentService.AgentResponse> response = medicalAgentController.prioritizeConsults(
                Map.of()
        );

        // Verify response
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().response());
        assertFalse(response.getBody().response().isEmpty(), "Response should contain prioritized queue");

        // Verify that response mentions prioritization or queue
        String responseText = response.getBody().response().toLowerCase();
        assertTrue(responseText.contains("priority") ||
                        responseText.contains("queue") ||
                        responseText.contains("urgent") ||
                        responseText.contains("critical"),
                "Response should reference prioritization");
    }

    @Test
    void testQueuePrioritization_DeterministicOrder_ContainsAllCasesAndStrictUrgencyOrder() {
        ResponseEntity<MedicalAgentService.AgentResponse> response = medicalAgentController.prioritizeConsults(
                Map.of()
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        String body = response.getBody().response();
        assertNotNull(body);
        assertFalse(body.isEmpty());

        // All four cases from setUp must appear exactly once
        for (String caseId : List.of(criticalCaseId, highUrgencyCaseId, mediumUrgencyCaseId, lowUrgencyCaseId)) {
            int first = body.indexOf(caseId);
            assertTrue(first >= 0, "Response must contain case ID: " + caseId);
            assertEquals(first, body.lastIndexOf(caseId), "Case ID must appear exactly once: " + caseId);
        }

        // When multiple urgency levels appear, strict order: CRITICAL before HIGH before MEDIUM before LOW
        int idxCritical = body.indexOf("CRITICAL");
        int idxHigh = body.indexOf("HIGH");
        int idxMedium = body.indexOf("MEDIUM");
        int idxLow = body.indexOf("LOW");
        if (idxCritical >= 0 && idxHigh >= 0) {
            assertTrue(idxCritical < idxHigh, "CRITICAL must appear before HIGH");
        }
        if (idxHigh >= 0 && idxMedium >= 0) {
            assertTrue(idxHigh < idxMedium, "HIGH must appear before MEDIUM");
        }
        if (idxMedium >= 0 && idxLow >= 0) {
            assertTrue(idxMedium < idxLow, "MEDIUM must appear before LOW");
        }

        if (response.getBody().metadata() != null && response.getBody().metadata().containsKey("deterministicOrder")) {
            assertEquals(true, response.getBody().metadata().get("deterministicOrder"));
        }
    }

    @Test
    void testQueuePrioritizationWithFilters() {
        // Test with specific filters
        ResponseEntity<MedicalAgentService.AgentResponse> response = medicalAgentController.prioritizeConsults(
                Map.of(
                        "minUrgency", "HIGH",
                        "maxResults", 10
                )
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().response());
    }
}
