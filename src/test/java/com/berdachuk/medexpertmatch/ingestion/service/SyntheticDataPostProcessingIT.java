package com.berdachuk.medexpertmatch.ingestion.service;

import com.berdachuk.medexpertmatch.clinicalexperience.repository.ClinicalExperienceRepository;
import com.berdachuk.medexpertmatch.doctor.repository.MedicalSpecialtyRepository;
import com.berdachuk.medexpertmatch.embedding.service.EmbeddingService;
import com.berdachuk.medexpertmatch.facility.repository.FacilityRepository;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for synthetic data post-processing (clear, graph rebuild cycles).
 */
/**
 * REQ-015: integration coverage for registered requirement.
 * REQ-017: integration coverage for registered requirement.
 */
class SyntheticDataPostProcessingIT extends BaseIntegrationTest {

    @Autowired
    private SyntheticDataGenerator syntheticDataGenerator;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private MedicalSpecialtyRepository medicalSpecialtyRepository;

    @Autowired
    private ClinicalExperienceRepository clinicalExperienceRepository;

    @Autowired
    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        syntheticDataGenerator.clearTestData();
    }

    @Test
    void testClearGraphWithExistingData() {
        syntheticDataGenerator.generateFacilities(3);
        syntheticDataGenerator.generateIcd10Codes("tiny");
        syntheticDataGenerator.generateMedicalSpecialties("tiny");
        syntheticDataGenerator.generateDoctors(5);
        syntheticDataGenerator.generateMedicalCases(10);
        syntheticDataGenerator.generateClinicalExperiences(5, 10);

        assertTrue(facilityRepository.findAllIds(0).size() > 0, "Facilities should exist before clear");
        assertTrue(medicalCaseRepository.findAllIds(0).size() > 0, "Cases should exist before clear");

        syntheticDataGenerator.clearTestData();

        assertEquals(0, facilityRepository.findAllIds(0).size(), "Facilities should be cleared");
        assertEquals(0, medicalCaseRepository.findAllIds(0).size(), "Cases should be cleared");
        assertEquals(0, medicalSpecialtyRepository.findAll().size(), "Specialties should be cleared");
    }

    @Test
    void testClearGraphWithEmptyGraph() {
        syntheticDataGenerator.clearTestData();

        assertDoesNotThrow(() -> syntheticDataGenerator.clearTestData());
        assertEquals(0, facilityRepository.findAllIds(0).size());
        assertEquals(0, medicalCaseRepository.findAllIds(0).size());
    }

    @Test
    void testSequentialClearGenerateClearCycle() {
        for (int iteration = 0; iteration < 3; iteration++) {
            syntheticDataGenerator.generateFacilities(2);
            syntheticDataGenerator.generateIcd10Codes("tiny");
            syntheticDataGenerator.generateMedicalSpecialties("tiny");
            syntheticDataGenerator.generateDoctors(3);
            syntheticDataGenerator.generateMedicalCases(5);
            syntheticDataGenerator.generateClinicalExperiences(3, 5);

            assertTrue(medicalCaseRepository.findAllIds(0).size() >= 5,
                    "Iteration " + iteration + ": Should have at least 5 cases");

            assertEquals(2,
                    facilityRepository.findAllIds(0).size(),
                    "Iteration " + iteration + ": Should have exactly 2 facilities (from fresh generation)");

            syntheticDataGenerator.clearTestData();

            assertEquals(0, facilityRepository.findAllIds(0).size(),
                    "Iteration " + iteration + ": Should be empty after clear");
            assertEquals(0, medicalCaseRepository.findAllIds(0).size(),
                    "Iteration " + iteration + ": Should be empty after clear");
        }
    }

    @Test
    void testIdempotentClearAfterPartialGeneration() {
        syntheticDataGenerator.generateFacilities(2);
        syntheticDataGenerator.generateIcd10Codes("tiny");

        assertTrue(facilityRepository.findAllIds(0).size() > 0);

        syntheticDataGenerator.clearTestData();
        assertEquals(0, facilityRepository.findAllIds(0).size());

        syntheticDataGenerator.clearTestData();
        assertEquals(0, facilityRepository.findAllIds(0).size());
    }

    @Test
    void testClearPreservesCatalogStateCaches() {
        syntheticDataGenerator.generateIcd10Codes("tiny");
        syntheticDataGenerator.generateMedicalSpecialties("tiny");

        assertTrue(medicalSpecialtyRepository.findAll().size() > 0, "Specialties should exist before clear");

        syntheticDataGenerator.clearTestData();

        assertTrue(medicalSpecialtyRepository.findAll().isEmpty(), "Specialties should be cleared");
    }

    @Test
    void testGenerateTinyWithClearThenGenerateTinyWithoutClear() {
        syntheticDataGenerator.generateTestData("tiny", true);

        List<String> caseIds1 = medicalCaseRepository.findAllIds(0);
        assertFalse(caseIds1.isEmpty(), "First run should produce cases");

        syntheticDataGenerator.generateTestData("tiny", false);

        List<String> caseIds2 = medicalCaseRepository.findAllIds(0);
        assertTrue(caseIds2.size() > caseIds1.size(),
                "Second run without clear should add more cases");
    }
}
