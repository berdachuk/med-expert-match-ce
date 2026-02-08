package com.berdachuk.medexpertmatch.clinicalexperience.repository;

import com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for ClinicalExperienceRepository.
 * Uses Testcontainers PostgreSQL database.
 */
class ClinicalExperienceRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private ClinicalExperienceRepository clinicalExperienceRepository;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clear existing data to ensure test independence
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.clinical_experiences");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.consultation_matches");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.medical_cases");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.doctors");
    }

    @Test
    void testFindById() {
        // Create test data
        String doctorId = IdGenerator.generateDoctorId();
        String caseId = createTestDoctorAndCase(doctorId);

        ClinicalExperience experience = new ClinicalExperience(
                IdGenerator.generateId(),
                doctorId,
                caseId,
                List.of("Cardiac catheterization", "PCI"),
                "HIGH",
                "SUCCESS",
                List.of(),
                5,
                5
        );

        String id = clinicalExperienceRepository.insert(experience);

        // Test findById
        var result = clinicalExperienceRepository.findById(id);
        assertTrue(result.isPresent());
        assertEquals(doctorId, result.get().doctorId());
        assertEquals(caseId, result.get().caseId());
        assertEquals("HIGH", result.get().complexityLevel());
        assertEquals("SUCCESS", result.get().outcome());
    }

    @Test
    void testFindByDoctorId() {
        // Create test data
        String doctorId = IdGenerator.generateDoctorId();
        String caseId1 = createTestDoctorAndCase(doctorId);
        String caseId2 = createTestDoctorAndCase(doctorId);

        ClinicalExperience exp1 = new ClinicalExperience(
                IdGenerator.generateId(),
                doctorId,
                caseId1,
                List.of("Procedure 1"),
                "MEDIUM",
                "IMPROVED",
                List.of(),
                3,
                4
        );

        ClinicalExperience exp2 = new ClinicalExperience(
                IdGenerator.generateId(),
                doctorId,
                caseId2,
                List.of("Procedure 2"),
                "HIGH",
                "SUCCESS",
                List.of(),
                7,
                5
        );

        clinicalExperienceRepository.insert(exp1);
        clinicalExperienceRepository.insert(exp2);

        // Test findByDoctorId
        List<ClinicalExperience> experiences = clinicalExperienceRepository.findByDoctorId(doctorId);
        assertNotNull(experiences);
        assertEquals(2, experiences.size());
        assertTrue(experiences.stream().allMatch(e -> e.doctorId().equals(doctorId)));
    }

    @Test
    void testFindByCaseId() {
        // Create test data
        String doctorId1 = IdGenerator.generateDoctorId();
        String doctorId2 = IdGenerator.generateDoctorId();
        String caseId = createTestDoctorAndCase(doctorId1);
        // Create doctor2 before creating clinical experience
        createTestDoctorAndCase(doctorId2);

        ClinicalExperience exp1 = new ClinicalExperience(
                IdGenerator.generateId(),
                doctorId1,
                caseId,
                List.of("Consultation"),
                "MEDIUM",
                "STABLE",
                List.of(),
                2,
                4
        );

        ClinicalExperience exp2 = new ClinicalExperience(
                IdGenerator.generateId(),
                doctorId2,
                caseId,
                List.of("Second opinion"),
                "MEDIUM",
                "IMPROVED",
                List.of(),
                1,
                5
        );

        clinicalExperienceRepository.insert(exp1);
        clinicalExperienceRepository.insert(exp2);

        // Test findByCaseId
        List<ClinicalExperience> experiences = clinicalExperienceRepository.findByCaseId(caseId);
        assertNotNull(experiences);
        assertEquals(2, experiences.size());
        assertTrue(experiences.stream().allMatch(e -> e.caseId().equals(caseId)));
    }

    @Test
    void testFindByDoctorIds_BatchLoading() {
        // Create test data
        String doctorId1 = IdGenerator.generateDoctorId();
        String doctorId2 = IdGenerator.generateDoctorId();
        String caseId1 = createTestDoctorAndCase(doctorId1);
        String caseId2 = createTestDoctorAndCase(doctorId2);

        ClinicalExperience exp1 = new ClinicalExperience(
                IdGenerator.generateId(),
                doctorId1,
                caseId1,
                List.of("Procedure 1"),
                "MEDIUM",
                "SUCCESS",
                List.of(),
                3,
                4
        );

        ClinicalExperience exp2 = new ClinicalExperience(
                IdGenerator.generateId(),
                doctorId2,
                caseId2,
                List.of("Procedure 2"),
                "HIGH",
                "IMPROVED",
                List.of(),
                5,
                5
        );

        clinicalExperienceRepository.insert(exp1);
        clinicalExperienceRepository.insert(exp2);

        // Test batch loading
        Map<String, List<ClinicalExperience>> experiencesByDoctor = clinicalExperienceRepository.findByDoctorIds(List.of(doctorId1, doctorId2));

        assertNotNull(experiencesByDoctor);
        assertEquals(2, experiencesByDoctor.size());
        assertTrue(experiencesByDoctor.containsKey(doctorId1));
        assertTrue(experiencesByDoctor.containsKey(doctorId2));
        assertEquals(1, experiencesByDoctor.get(doctorId1).size());
        assertEquals(1, experiencesByDoctor.get(doctorId2).size());
    }

    @Test
    void testFindByCaseIds_BatchLoading() {
        // Create test data
        String doctorId = IdGenerator.generateDoctorId();
        String caseId1 = createTestDoctorAndCase(doctorId);
        String caseId2 = createTestDoctorAndCase(doctorId);

        ClinicalExperience exp1 = new ClinicalExperience(
                IdGenerator.generateId(),
                doctorId,
                caseId1,
                List.of("Procedure 1"),
                "MEDIUM",
                "SUCCESS",
                List.of(),
                3,
                4
        );

        ClinicalExperience exp2 = new ClinicalExperience(
                IdGenerator.generateId(),
                doctorId,
                caseId2,
                List.of("Procedure 2"),
                "HIGH",
                "IMPROVED",
                List.of(),
                5,
                5
        );

        clinicalExperienceRepository.insert(exp1);
        clinicalExperienceRepository.insert(exp2);

        // Test batch loading
        Map<String, List<ClinicalExperience>> experiencesByCase = clinicalExperienceRepository.findByCaseIds(List.of(caseId1, caseId2));

        assertNotNull(experiencesByCase);
        assertEquals(2, experiencesByCase.size());
        assertTrue(experiencesByCase.containsKey(caseId1));
        assertTrue(experiencesByCase.containsKey(caseId2));
        assertEquals(1, experiencesByCase.get(caseId1).size());
        assertEquals(1, experiencesByCase.get(caseId2).size());
    }

    @Test
    void testInsert() {
        // Create test data
        String doctorId = IdGenerator.generateDoctorId();
        String caseId = createTestDoctorAndCase(doctorId);

        ClinicalExperience experience = new ClinicalExperience(
                IdGenerator.generateId(),
                doctorId,
                caseId,
                List.of("Test procedure"),
                "LOW",
                "STABLE",
                List.of(),
                1,
                3
        );

        String id = clinicalExperienceRepository.insert(experience);
        assertEquals(experience.id(), id);

        // Verify experience was created
        var result = clinicalExperienceRepository.findById(experience.id());
        assertTrue(result.isPresent());
        assertEquals("LOW", result.get().complexityLevel());

        // Verify insert throws exception on duplicate ID
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            clinicalExperienceRepository.insert(experience);
        });
    }

    @Test
    void testUpdate() {
        // Create test data
        String doctorId = IdGenerator.generateDoctorId();
        String caseId = createTestDoctorAndCase(doctorId);

        ClinicalExperience experience = new ClinicalExperience(
                IdGenerator.generateId(),
                doctorId,
                caseId,
                List.of("Test procedure"),
                "LOW",
                "STABLE",
                List.of(),
                1,
                3
        );
        clinicalExperienceRepository.insert(experience);

        // Update experience
        ClinicalExperience updatedExperience = new ClinicalExperience(
                experience.id(),
                doctorId,
                caseId,
                List.of("Updated procedure"),
                "MEDIUM",
                "IMPROVED",
                List.of("Minor complication"),
                2,
                4
        );

        String updatedId = clinicalExperienceRepository.update(updatedExperience);
        assertEquals(experience.id(), updatedId);

        // Verify experience was updated
        var updatedResult = clinicalExperienceRepository.findById(experience.id());
        assertTrue(updatedResult.isPresent());
        assertEquals("MEDIUM", updatedResult.get().complexityLevel());
        assertEquals("IMPROVED", updatedResult.get().outcome());
        assertEquals(1, updatedResult.get().complications().size());

        // Verify update throws exception on non-existent ID
        ClinicalExperience nonExistentExperience = new ClinicalExperience(
                IdGenerator.generateId(),
                doctorId,
                caseId,
                List.of(),
                "LOW",
                "PENDING",
                List.of(),
                1,
                3
        );
        assertThrows(org.springframework.dao.EmptyResultDataAccessException.class, () -> {
            clinicalExperienceRepository.update(nonExistentExperience);
        });
    }

    @Test
    void testDeleteAll() {
        // Create test data
        String doctorId = IdGenerator.generateDoctorId();
        String caseId = createTestDoctorAndCase(doctorId);

        ClinicalExperience experience = new ClinicalExperience(
                IdGenerator.generateId(),
                doctorId,
                caseId,
                List.of(),
                "LOW",
                "STABLE",
                List.of(),
                1,
                3
        );

        clinicalExperienceRepository.insert(experience);
        assertTrue(clinicalExperienceRepository.findById(experience.id()).isPresent());

        // Delete all
        int deleted = clinicalExperienceRepository.deleteAll();
        assertTrue(deleted >= 1);

        // Verify experience was deleted
        assertTrue(clinicalExperienceRepository.findById(experience.id()).isEmpty());
    }

    /**
     * Helper method to create test doctor and case.
     */
    private String createTestDoctorAndCase(String doctorId) {
        // Create doctor with unique email to avoid duplicate key errors
        // Use ON CONFLICT to handle case where doctor already exists
        String insertDoctorSql = """
                INSERT INTO medexpertmatch.doctors (id, name, email, specialties, telehealth_enabled, availability_status)
                VALUES (:id, :name, :email, :specialties, :telehealthEnabled, :availabilityStatus)
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name
                """;
        Map<String, Object> doctorParams = new HashMap<>();
        doctorParams.put("id", doctorId);
        doctorParams.put("name", "Dr. Test " + doctorId.substring(0, 4));
        // Generate unique email using full doctorId to avoid duplicates
        doctorParams.put("email", "test" + doctorId.replaceAll("[^0-9]", "") + "@hospital.com");
        doctorParams.put("specialties", new String[]{"General Practice"});
        doctorParams.put("telehealthEnabled", false);
        doctorParams.put("availabilityStatus", "AVAILABLE");
        namedJdbcTemplate.update(insertDoctorSql, doctorParams);

        // Create case
        String caseId = IdGenerator.generateId();
        String insertCaseSql = """
                INSERT INTO medexpertmatch.medical_cases (id, patient_age, chief_complaint, urgency_level, case_type, required_specialty)
                VALUES (:id, :patientAge, :chiefComplaint, :urgencyLevel, :caseType, :requiredSpecialty)
                """;
        Map<String, Object> caseParams = new HashMap<>();
        caseParams.put("id", caseId);
        caseParams.put("patientAge", 40);
        caseParams.put("chiefComplaint", "Test complaint");
        caseParams.put("urgencyLevel", "MEDIUM");
        caseParams.put("caseType", "CONSULT_REQUEST");
        caseParams.put("requiredSpecialty", "General Practice");
        namedJdbcTemplate.update(insertCaseSql, caseParams);

        return caseId;
    }
}
