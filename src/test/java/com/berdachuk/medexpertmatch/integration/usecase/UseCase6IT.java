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
 * Integration test for Use Case 6: Regional Routing.
 * <p>
 * Flow:
 * 1. Case manager needs to route a case to appropriate facility
 * 2. System calls POST /api/v1/agent/route-case/{caseId}
 * 3. Agent uses case-analyzer and routing-planner skills
 * 4. Agent queries graph for candidate facilities and uses Semantic Graph Retrieval for routing scores
 * 5. Response: ranked list of facilities with routing scores and rationales
 */
class UseCase6IT extends BaseIntegrationTest {

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

        // Create test doctor with facility association
        testDoctorId = IdGenerator.generateDoctorId();
        Doctor doctor = new Doctor(
                testDoctorId,
                "Dr. Regional Specialist",
                "regional@example.com",
                List.of("Cardiology"),
                List.of("Board Certified"),
                List.of("8009377469709733890"), // Facility ID
                true,
                "AVAILABLE"
        );
        doctorRepository.insert(doctor);

        // Create case requiring routing
        testCaseId = medicalCaseRepository.insert(new MedicalCase(
                null,
                70,
                "Complex cardiac case requiring specialized facility",
                "Complex MI with complications",
                "Complex MI",
                List.of("I21.9"),
                List.of(),
                UrgencyLevel.HIGH,
                "Cardiology",
                CaseType.INPATIENT,
                "Case requires routing to appropriate facility with cardiac care capabilities",
                null
        ));
    }

    @Test
    void testRegionalRouting() {
        // Use Case 6: Regional Routing
        ResponseEntity<MedicalAgentService.AgentResponse> response = medicalAgentController.routeCaseSync(
                testCaseId,
                Map.of()
        );

        // Verify response
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().response());
        assertFalse(response.getBody().response().isEmpty(), "Response should contain routing recommendations");

        // Verify that response mentions routing or facility
        String responseText = response.getBody().response().toLowerCase();
        assertTrue(responseText.contains("routing") ||
                        responseText.contains("facility") ||
                        responseText.contains("route") ||
                        responseText.contains("center"),
                "Response should reference facility routing");
    }

    @Test
    void testRegionalRoutingWithOptions() {
        // Test with routing options
        ResponseEntity<MedicalAgentService.AgentResponse> response = medicalAgentController.routeCaseSync(
                testCaseId,
                Map.of(
                        "preferAcademic", true,
                        "maxDistance", 100,
                        "maxResults", 5
                )
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().response());
    }
}
