package com.berdachuk.medexpertmatch.ingestion.service;

import com.berdachuk.medexpertmatch.doctor.repository.MedicalSpecialtyRepository;
import com.berdachuk.medexpertmatch.facility.repository.FacilityRepository;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for synthetic data edge cases:
 * empty catalog handling, size=0, duplicates, null safety, large batch thresholds.
 */
/**
 * REQ-015: integration coverage for registered requirement.
 * REQ-017: integration coverage for registered requirement.
 */
class SyntheticDataEdgeCasesIT extends BaseIntegrationTest {

    @Autowired
    private SyntheticDataGenerator syntheticDataGenerator;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    @Autowired
    private MedicalSpecialtyRepository medicalSpecialtyRepository;

    @BeforeEach
    void setUp() {
        syntheticDataGenerator.clearTestData();
    }

    @Test
    void testGenerateFacilities_ZeroCount() {
        syntheticDataGenerator.generateIcd10Codes("tiny");
        syntheticDataGenerator.generateMedicalSpecialties("tiny");

        assertDoesNotThrow(() -> syntheticDataGenerator.generateFacilities(0));
        assertEquals(0, facilityRepository.findAllIds(0).size(), "Zero facilities should be created");
    }

    @Test
    void testGenerateFacilities_NegativeCount() {
        syntheticDataGenerator.generateIcd10Codes("tiny");
        syntheticDataGenerator.generateMedicalSpecialties("tiny");

        assertThrows(IllegalArgumentException.class,
                () -> syntheticDataGenerator.generateFacilities(-1));
    }

    @Test
    void testGenerateIcd10Codes_EmptyCatalog() {
        assertDoesNotThrow(() -> syntheticDataGenerator.generateIcd10Codes("tiny"));
    }

    @Test
    void testGenerateMedicalSpecialties_EmptyCatalog() {
        assertDoesNotThrow(() -> syntheticDataGenerator.generateMedicalSpecialties("tiny"));
    }

    @Test
    void testNoDuplicateFacilities() {
        syntheticDataGenerator.generateIcd10Codes("tiny");
        syntheticDataGenerator.generateMedicalSpecialties("tiny");
        syntheticDataGenerator.generateFacilities(20);

        List<String> facilityIds = facilityRepository.findAllIds(0);
        assertEquals(20, facilityIds.size());

        Set<String> seen = new HashSet<>();
        for (String id : facilityIds) {
            assertFalse(seen.contains(id), "Facility IDs should be unique");
            seen.add(id);
        }
    }

    @Test
    void testNoDuplicateCaseIds() {
        syntheticDataGenerator.generateIcd10Codes("tiny");
        syntheticDataGenerator.generateMedicalSpecialties("tiny");
        syntheticDataGenerator.generateMedicalCases(50);

        List<String> caseIds = medicalCaseRepository.findAllIds(0);
        assertEquals(50, caseIds.size());

        Set<String> seen = new HashSet<>();
        for (String id : caseIds) {
            assertFalse(seen.contains(id), "Case IDs should be unique");
            seen.add(id);
        }
    }

    @Test
    void testNullSafety_ClearAfterNoGeneration() {
        assertDoesNotThrow(() -> syntheticDataGenerator.clearTestData());

        assertEquals(0, facilityRepository.findAllIds(0).size());
        assertEquals(0, medicalCaseRepository.findAllIds(0).size());
        assertTrue(medicalSpecialtyRepository.findAll().isEmpty());
    }

    @Test
    void testNullSafety_ClearAfterClear() {
        assertDoesNotThrow(() -> syntheticDataGenerator.clearTestData());
        assertDoesNotThrow(() -> syntheticDataGenerator.clearTestData());
        assertEquals(0, facilityRepository.findAllIds(0).size());
    }

    @Test
    void testGenerateLargeBatchOfCases() {
        syntheticDataGenerator.generateIcd10Codes("tiny");
        syntheticDataGenerator.generateMedicalSpecialties("tiny");
        syntheticDataGenerator.generateFacilities(10);

        syntheticDataGenerator.generateDoctors(20);
        syntheticDataGenerator.generateMedicalCases(200);
        syntheticDataGenerator.generateClinicalExperiences(20, 200);

        List<String> caseIds = medicalCaseRepository.findAllIds(0);
        assertEquals(200, caseIds.size(), "Should have exactly 200 cases");
    }

    @Test
    void testGenerateDoctors_ZeroCount() {
        syntheticDataGenerator.generateFacilities(1);

        assertDoesNotThrow(() -> syntheticDataGenerator.generateDoctors(0));
    }

    @Test
    void testGenerateWithEmptyCatalog() {
        assertDoesNotThrow(() -> syntheticDataGenerator.generateIcd10Codes("tiny"));
        assertDoesNotThrow(() -> syntheticDataGenerator.generateMedicalSpecialties("tiny"));
    }
}
