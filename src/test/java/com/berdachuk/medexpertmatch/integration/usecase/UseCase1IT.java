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
import com.berdachuk.medexpertmatch.retrieval.repository.ConsultationMatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Use Case 1: Specialist Matching for Complex Inpatient Cases.
 * <p>
 * Flow:
 * 1. Physician documents case in EMR; FHIR Bundle sent to MedExpertMatch → MedicalCase created
 * 2. Physician clicks "Find specialist" → EMR calls POST /api/v1/agent/match/{caseId}
 * 3. Agent uses case-analyzer and doctor-matcher skills with Semantic Graph Retrieval scoring
 * 4. Agent returns ranked list of doctors with scores and rationales
 */
class UseCase1IT extends BaseIntegrationTest {

    @Autowired
    private MedicalAgentController medicalAgentController;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    @Autowired
    private ConsultationMatchRepository consultationMatchRepository;

    @Autowired
    private ClinicalExperienceRepository clinicalExperienceRepository;

    private String testCaseId;
    private String testDoctorId1;
    private String testDoctorId2;

    @BeforeEach
    void setUp() {
        // Clear test data
        consultationMatchRepository.deleteAll();
        clinicalExperienceRepository.deleteAll();
        medicalCaseRepository.deleteAll();
        doctorRepository.deleteAll();

        // Create test doctors
        testDoctorId1 = IdGenerator.generateDoctorId();
        Doctor doctor1 = new Doctor(
                testDoctorId1,
                "Dr. Cardiology Specialist",
                "cardio@example.com",
                List.of("Cardiology"),
                List.of("Board Certified Cardiology"),
                List.of(),
                true,
                "AVAILABLE"
        );
        doctorRepository.insert(doctor1);

        testDoctorId2 = IdGenerator.generateDoctorId();
        Doctor doctor2 = new Doctor(
                testDoctorId2,
                "Dr. General Medicine",
                "general@example.com",
                List.of("Internal Medicine"),
                List.of("Board Certified Internal Medicine"),
                List.of(),
                false,
                "AVAILABLE"
        );
        doctorRepository.insert(doctor2);

        // Create test medical case (complex inpatient case)
        MedicalCase medicalCase = new MedicalCase(
                null, // Will be generated
                65, // patientAge
                "Chest pain and shortness of breath", // chiefComplaint
                "Chest pain, Shortness of breath, Elevated troponin", // symptoms
                "Acute myocardial infarction", // currentDiagnosis
                List.of("I21.9"), // icd10Codes
                List.of(), // snomedCodes
                UrgencyLevel.HIGH, // urgencyLevel
                "Cardiology", // requiredSpecialty
                CaseType.INPATIENT, // caseType
                "Patient presents with acute MI. Requires immediate specialist consultation.", // additionalNotes
                null // abstractText
        );
        testCaseId = medicalCaseRepository.insert(medicalCase);
    }

    @Test
    void testSpecialistMatching() {
        // Use Case 1: Specialist Matching for Complex Inpatient Cases
        ResponseEntity<MedicalAgentService.AgentResponse> response = medicalAgentController.matchDoctors(
                testCaseId,
                Map.of()
        );

        // Verify response
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().response());
        assertFalse(response.getBody().response().isEmpty(), "Response should contain matched doctors");

        // Verify that the response mentions the case ID
        assertTrue(response.getBody().response().contains(testCaseId) ||
                        response.getBody().response().toLowerCase().contains("cardiology") ||
                        response.getBody().response().toLowerCase().contains("specialist"),
                "Response should reference the case or matched specialists");
    }

    @Test
    void testSpecialistMatchingWithFilters() {
        // Test with optional filters
        ResponseEntity<MedicalAgentService.AgentResponse> response = medicalAgentController.matchDoctors(
                testCaseId,
                Map.of(
                        "maxResults", 5,
                        "minScore", 0.7
                )
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().response());
    }

    @Test
    void testMatchDoctorsForExistingCase_NoDuplicateCaseOrConsultationMatch() {
        // Run Match Doctors for existing case (POST /api/v1/agent/match/{caseId})
        ResponseEntity<MedicalAgentService.AgentResponse> response1 = medicalAgentController.matchDoctors(
                testCaseId,
                Map.of()
        );
        assertNotNull(response1);
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertNotNull(response1.getBody());
        assertNotNull(response1.getBody().response());

        long caseCountAfterFirst = medicalCaseRepository.findAllIds(100).size();
        long matchCountAfterFirst = consultationMatchRepository.count();
        assertEquals(1, caseCountAfterFirst, "Exactly one case should exist after first match");
        assertTrue(matchCountAfterFirst >= 1, "ConsultationMatch rows should be persisted for the case");

        // Run Match Doctors again for the same caseId (replace, not duplicate)
        ResponseEntity<MedicalAgentService.AgentResponse> response2 = medicalAgentController.matchDoctors(
                testCaseId,
                Map.of()
        );
        assertNotNull(response2);
        assertEquals(HttpStatus.OK, response2.getStatusCode());

        long caseCountAfterSecond = medicalCaseRepository.findAllIds(100).size();
        long matchCountAfterSecond = consultationMatchRepository.count();
        assertEquals(1, caseCountAfterSecond, "Still exactly one case after second match (no duplicate case)");
        assertEquals(matchCountAfterFirst, matchCountAfterSecond,
                "ConsultationMatch set should be replaced, not duplicated (same count after second run)");
    }
}
