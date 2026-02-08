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
 * Integration test for Use Case 2: Online Second Opinion / Telehealth.
 * <p>
 * Flow:
 * 1. User uploads medical records; portal produces FHIR Bundle → MedicalCase with type SECOND_OPINION created
 * 2. Portal calls POST /api/v1/agent/match/{caseId}
 * 3. Agent uses case-analyzer to extract diagnosis, ICD-10/SNOMED codes, complexity
 * 4. Agent uses doctor-matcher with Semantic Graph Retrieval, prioritizing telehealth-enabled doctors
 * 5. Response: top specialists with scores, availability, and reasons
 */
class UseCase2IT extends BaseIntegrationTest {

    @Autowired
    private MedicalAgentController medicalAgentController;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    @Autowired
    private ClinicalExperienceRepository clinicalExperienceRepository;

    private String testCaseId;
    private String telehealthDoctorId;
    private String nonTelehealthDoctorId;

    @BeforeEach
    void setUp() {
        // Clear test data
        clinicalExperienceRepository.deleteAll();
        medicalCaseRepository.deleteAll();
        doctorRepository.deleteAll();

        // Create telehealth-enabled doctor
        telehealthDoctorId = IdGenerator.generateDoctorId();
        Doctor telehealthDoctor = new Doctor(
                telehealthDoctorId,
                "Dr. Telehealth Specialist",
                "telehealth@example.com",
                List.of("Oncology"),
                List.of("Board Certified Oncology"),
                List.of(),
                true, // telehealthEnabled
                "AVAILABLE"
        );
        doctorRepository.insert(telehealthDoctor);

        // Create non-telehealth doctor
        nonTelehealthDoctorId = IdGenerator.generateDoctorId();
        Doctor nonTelehealthDoctor = new Doctor(
                nonTelehealthDoctorId,
                "Dr. In-Person Only",
                "inperson@example.com",
                List.of("Oncology"),
                List.of("Board Certified Oncology"),
                List.of(),
                false, // telehealthEnabled
                "AVAILABLE"
        );
        doctorRepository.insert(nonTelehealthDoctor);

        // Create second opinion case
        MedicalCase medicalCase = new MedicalCase(
                null, // Will be generated
                55, // patientAge
                "Second opinion for cancer diagnosis", // chiefComplaint
                "Abnormal mammogram, Biopsy results pending", // symptoms
                "Breast cancer suspected", // currentDiagnosis
                List.of("C50.9"), // icd10Codes (breast cancer)
                List.of(), // snomedCodes
                UrgencyLevel.MEDIUM, // urgencyLevel
                "Oncology", // requiredSpecialty
                CaseType.SECOND_OPINION, // caseType
                "Patient seeking second opinion for breast cancer diagnosis and treatment plan.", // additionalNotes
                null // abstractText
        );
        testCaseId = medicalCaseRepository.insert(medicalCase);
    }

    @Test
    void testSecondOpinionMatching() {
        // Use Case 2: Online Second Opinion / Telehealth
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

        // Verify that response mentions second opinion or telehealth
        String responseText = response.getBody().response().toLowerCase();
        assertTrue(responseText.contains("oncology") ||
                        responseText.contains("specialist") ||
                        responseText.contains("doctor"),
                "Response should reference matched specialists");
    }

    @Test
    void testSecondOpinionWithTelehealthPreference() {
        // Test with telehealth preference
        ResponseEntity<MedicalAgentService.AgentResponse> response = medicalAgentController.matchDoctors(
                testCaseId,
                Map.of(
                        "preferTelehealth", true,
                        "maxResults", 10
                )
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().response());
    }
}
