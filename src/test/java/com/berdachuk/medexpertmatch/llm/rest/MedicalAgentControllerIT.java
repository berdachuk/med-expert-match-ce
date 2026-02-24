package com.berdachuk.medexpertmatch.llm.rest;

import com.berdachuk.medexpertmatch.clinicalexperience.repository.ClinicalExperienceRepository;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
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
 * Integration test for MedicalAgentController.
 * Tests all 6 use cases end-to-end.
 */
class MedicalAgentControllerIT extends BaseIntegrationTest {

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
                "Dr. Test Specialist",
                "test@example.com",
                List.of("Cardiology"),
                List.of("Board Certified"),
                List.of(),
                true,
                "AVAILABLE"
        );
        doctorRepository.insert(doctor);

        // Create test medical case
        MedicalCase medicalCase = new MedicalCase(
                null, // Will be generated
                65, // patientAge
                "Chest pain", // chiefComplaint
                "Chest pain, Shortness of breath", // symptoms
                "Acute myocardial infarction", // currentDiagnosis
                List.of("I21.9"), // icd10Codes
                List.of(), // snomedCodes
                UrgencyLevel.HIGH, // urgencyLevel
                "Cardiology", // requiredSpecialty
                CaseType.INPATIENT, // caseType
                "Patient presents with acute MI", // additionalNotes
                null // abstractText
        );
        testCaseId = medicalCaseRepository.insert(medicalCase);
    }

    @Test
    void testUseCase1_MatchDoctors() {
        // Use Case 1: Specialist Matching
        ResponseEntity<MedicalAgentService.AgentResponse> response = medicalAgentController.matchDoctorsSync(
                testCaseId,
                Map.of()
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().response());
    }

    @Test
    void testMatchFromText_WithRequiredCaseText() {
        // Test matchFromText with only required caseText
        Map<String, Object> request = Map.of(
                "caseText", "Patient presents with severe chest pain and shortness of breath"
        );

        ResponseEntity<MedicalAgentService.AgentResponse> response = medicalAgentController.matchFromTextSync(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().response());

        // Verify case was created - search for the newly created case
        List<String> allCaseIds = medicalCaseRepository.findAllIds(100);
        List<MedicalCase> cases = allCaseIds.stream()
                .map(id -> medicalCaseRepository.findById(id))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .filter(c -> c.chiefComplaint() != null &&
                        c.chiefComplaint().toLowerCase().contains("chest pain"))
                .toList();
        assertFalse(cases.isEmpty(), "Case should be created with chest pain chief complaint");

        // Verify the created case has correct default values
        MedicalCase createdCase = cases.get(0);
        assertEquals("Patient presents with severe chest pain and shortness of breath",
                createdCase.chiefComplaint());
        assertEquals(CaseType.INPATIENT, createdCase.caseType(),
                "Default case type should be INPATIENT");
    }

    @Test
    void testMatchFromText_WithAllOptionalParameters() {
        // Test matchFromText with all optional parameters
        Map<String, Object> request = Map.of(
                "caseText", "Patient presents with chest pain",
                "symptoms", "Chest pain, Shortness of breath",
                "additionalNotes", "History of hypertension",
                "patientAge", 65,
                "caseType", "INPATIENT"
        );

        ResponseEntity<MedicalAgentService.AgentResponse> response = medicalAgentController.matchFromTextSync(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().response());
    }

    @Test
    void testMatchFromText_Validation_MissingCaseText() {
        // Test validation - missing caseText
        Map<String, Object> request = Map.of();

        assertThrows(IllegalArgumentException.class, () -> {
            medicalAgentController.matchFromTextSync(request);
        });
    }

    @Test
    void testMatchFromText_Validation_EmptyCaseText() {
        // Test validation - empty caseText
        Map<String, Object> request = Map.of("caseText", "");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            medicalAgentController.matchFromTextSync(request);
        });
        assertTrue(exception.getMessage().contains("caseText") ||
                        exception.getMessage().contains("empty") ||
                        exception.getMessage().contains("required"),
                "Exception message should mention caseText requirement");
    }

    @Test
    void testMatchFromText_WithInvalidCaseType() {
        // Test that invalid caseType defaults to INPATIENT
        Map<String, Object> request = Map.of(
                "caseText", "Test case with invalid case type",
                "caseType", "INVALID_TYPE"
        );

        ResponseEntity<MedicalAgentService.AgentResponse> response = medicalAgentController.matchFromTextSync(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify case was created with default INPATIENT type
        List<String> allCaseIds = medicalCaseRepository.findAllIds(100);
        List<MedicalCase> cases = allCaseIds.stream()
                .map(id -> medicalCaseRepository.findById(id))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .filter(c -> c.chiefComplaint() != null &&
                        c.chiefComplaint().contains("Test case with invalid case type"))
                .toList();
        assertFalse(cases.isEmpty(), "Case should be created");
        assertEquals(CaseType.INPATIENT, cases.get(0).caseType(),
                "Invalid caseType should default to INPATIENT");
    }

    @Test
    void testMatchFromText_WithDifferentCaseTypes() {
        // Test SECOND_OPINION case type
        Map<String, Object> request1 = Map.of(
                "caseText", "Second opinion case",
                "caseType", "SECOND_OPINION"
        );
        ResponseEntity<MedicalAgentService.AgentResponse> response1 = medicalAgentController.matchFromTextSync(request1);
        assertEquals(HttpStatus.OK, response1.getStatusCode());

        // Test CONSULT_REQUEST case type
        Map<String, Object> request2 = Map.of(
                "caseText", "Consult request case",
                "caseType", "CONSULT_REQUEST"
        );
        ResponseEntity<MedicalAgentService.AgentResponse> response2 = medicalAgentController.matchFromTextSync(request2);
        assertEquals(HttpStatus.OK, response2.getStatusCode());

        // Verify both cases were created with correct types
        List<String> allCaseIds = medicalCaseRepository.findAllIds(100);
        List<MedicalCase> secondOpinionCases = allCaseIds.stream()
                .map(id -> medicalCaseRepository.findById(id))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .filter(c -> c.chiefComplaint() != null &&
                        c.chiefComplaint().contains("Second opinion case"))
                .toList();
        assertFalse(secondOpinionCases.isEmpty());
        assertEquals(CaseType.SECOND_OPINION, secondOpinionCases.get(0).caseType());

        List<MedicalCase> consultCases = allCaseIds.stream()
                .map(id -> medicalCaseRepository.findById(id))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .filter(c -> c.chiefComplaint() != null &&
                        c.chiefComplaint().contains("Consult request case"))
                .toList();
        assertFalse(consultCases.isEmpty());
        assertEquals(CaseType.CONSULT_REQUEST, consultCases.get(0).caseType());
    }

    @Test
    void testUseCase3_PrioritizeConsults() {
        // Use Case 3: Queue Prioritization
        ResponseEntity<MedicalAgentService.AgentResponse> response = medicalAgentController.prioritizeConsultsSync(
                Map.of()
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().response());
    }

    @Test
    void testUseCase4_NetworkAnalytics() {
        // Use Case 4: Network Analytics
        ResponseEntity<MedicalAgentService.AgentResponse> response = medicalAgentController.networkAnalytics(
                Map.of("conditionCode", "I21.9")
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().response());
    }

    @Test
    void testUseCase5_AnalyzeCase() {
        // Use Case 5: Decision Support - Case Analysis
        ResponseEntity<MedicalAgentService.AgentResponse> response = medicalAgentController.analyzeCaseSync(
                testCaseId,
                Map.of()
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().response());
    }

    @Test
    void testUseCase6_RouteCase() {
        // Use Case 6: Regional Routing
        ResponseEntity<MedicalAgentService.AgentResponse> response = medicalAgentController.routeCaseSync(
                testCaseId,
                Map.of()
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().response());
    }
}
