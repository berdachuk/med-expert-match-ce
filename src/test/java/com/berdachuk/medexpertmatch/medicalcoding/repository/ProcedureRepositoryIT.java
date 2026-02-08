package com.berdachuk.medexpertmatch.medicalcoding.repository;

import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.berdachuk.medexpertmatch.medicalcoding.domain.Procedure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for ProcedureRepository.
 * Uses Testcontainers PostgreSQL database.
 */
class ProcedureRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private ProcedureRepository procedureRepository;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clear existing data to ensure test independence
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.procedures");
    }

    @Test
    void testFindById() {
        // Create test procedure
        Procedure testProcedure = new Procedure(
                IdGenerator.generateId(),
                "Echocardiogram",
                "echocardiogram",
                "Ultrasound examination of the heart",
                "Diagnostic"
        );

        String id = procedureRepository.insert(testProcedure);

        // Test findById
        var result = procedureRepository.findById(id);
        assertTrue(result.isPresent());
        assertEquals("Echocardiogram", result.get().name());
        assertEquals("echocardiogram", result.get().normalizedName());
        assertEquals("Ultrasound examination of the heart", result.get().description());
        assertEquals("Diagnostic", result.get().category());
    }

    @Test
    void testFindByName() {
        // Create test procedure
        Procedure testProcedure = new Procedure(
                IdGenerator.generateId(),
                "CT Scan",
                "ctscan",
                "Computed tomography scan",
                "Diagnostic"
        );

        procedureRepository.insert(testProcedure);

        // Test findByName (case-insensitive)
        var result = procedureRepository.findByName("CT Scan");
        assertTrue(result.isPresent());
        assertEquals("CT Scan", result.get().name());
        assertEquals("Computed tomography scan", result.get().description());

        // Test case-insensitive search
        var resultLower = procedureRepository.findByName("ct scan");
        assertTrue(resultLower.isPresent());
        assertEquals("CT Scan", resultLower.get().name());
    }

    @Test
    void testFindByNormalizedName() {
        // Create test procedure
        Procedure testProcedure = new Procedure(
                IdGenerator.generateId(),
                "Blood Test",
                "bloodtest",
                "Laboratory blood analysis",
                "Diagnostic"
        );

        procedureRepository.insert(testProcedure);

        // Test findByNormalizedName
        var result = procedureRepository.findByNormalizedName("bloodtest");
        assertTrue(result.isPresent());
        assertEquals("Blood Test", result.get().name());
        assertEquals("bloodtest", result.get().normalizedName());

        // Test case-insensitive normalized name search
        var resultUpper = procedureRepository.findByNormalizedName("BLOODTEST");
        assertTrue(resultUpper.isPresent());
        assertEquals("Blood Test", resultUpper.get().name());
    }

    @Test
    void testFindByCategory() {
        // Create test procedures
        Procedure diagnostic1 = new Procedure(
                IdGenerator.generateId(),
                "MRI",
                "mri",
                "Magnetic resonance imaging",
                "Diagnostic"
        );

        Procedure diagnostic2 = new Procedure(
                IdGenerator.generateId(),
                "X-Ray",
                "xray",
                "Radiographic examination",
                "Diagnostic"
        );

        Procedure therapeutic = new Procedure(
                IdGenerator.generateId(),
                "Surgery",
                "surgery",
                "Surgical intervention",
                "Therapeutic"
        );

        procedureRepository.insert(diagnostic1);
        procedureRepository.insert(diagnostic2);
        procedureRepository.insert(therapeutic);

        // Test findByCategory
        List<Procedure> diagnosticProcedures = procedureRepository.findByCategory("Diagnostic");
        assertNotNull(diagnosticProcedures);
        assertTrue(diagnosticProcedures.size() >= 2);
        assertTrue(diagnosticProcedures.stream().allMatch(p -> p.category().equals("Diagnostic")));

        List<Procedure> therapeuticProcedures = procedureRepository.findByCategory("Therapeutic");
        assertNotNull(therapeuticProcedures);
        assertEquals(1, therapeuticProcedures.size());
        assertEquals("Surgery", therapeuticProcedures.get(0).name());
    }

    @Test
    void testFindAll() {
        // Create test procedures
        Procedure procedure1 = new Procedure(
                IdGenerator.generateId(),
                "Echocardiogram",
                "echocardiogram",
                "Heart ultrasound",
                "Diagnostic"
        );

        Procedure procedure2 = new Procedure(
                IdGenerator.generateId(),
                "Surgery",
                "surgery",
                "Surgical procedure",
                "Therapeutic"
        );

        procedureRepository.insert(procedure1);
        procedureRepository.insert(procedure2);

        // Test findAll
        List<Procedure> allProcedures = procedureRepository.findAll();
        assertNotNull(allProcedures);
        assertTrue(allProcedures.size() >= 2);
        assertTrue(allProcedures.stream().anyMatch(p -> p.name().equals("Echocardiogram")));
        assertTrue(allProcedures.stream().anyMatch(p -> p.name().equals("Surgery")));
    }

    @Test
    void testInsert() {
        // Create procedure
        Procedure newProcedure = new Procedure(
                IdGenerator.generateId(),
                "Biopsy",
                "biopsy",
                "Tissue sample collection",
                "Diagnostic"
        );

        String id = procedureRepository.insert(newProcedure);
        assertEquals(newProcedure.id(), id);

        // Verify procedure was created
        var result = procedureRepository.findByName("Biopsy");
        assertTrue(result.isPresent());
        assertEquals("Tissue sample collection", result.get().description());
        assertEquals("Diagnostic", result.get().category());

        // Verify insert throws exception on duplicate ID
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            procedureRepository.insert(newProcedure);
        });
    }

    @Test
    void testInsertWithNullFields() {
        // Create procedure with null description and category
        Procedure newProcedure = new Procedure(
                IdGenerator.generateId(),
                "Consultation",
                "consultation",
                null,
                null
        );

        String id = procedureRepository.insert(newProcedure);
        assertEquals(newProcedure.id(), id);

        // Verify procedure was created with null fields
        var result = procedureRepository.findById(id);
        assertTrue(result.isPresent());
        assertEquals("Consultation", result.get().name());
        assertNull(result.get().description());
        assertNull(result.get().category());
    }

    @Test
    void testUpdate() {
        // Create procedure first
        Procedure newProcedure = new Procedure(
                IdGenerator.generateId(),
                "Physical Examination",
                "physicalexamination",
                "General physical exam",
                "Diagnostic"
        );
        procedureRepository.insert(newProcedure);

        // Update procedure
        Procedure updatedProcedure = new Procedure(
                newProcedure.id(),
                "Physical Examination",
                "physicalexamination",
                "Comprehensive physical examination (updated)",
                "Diagnostic"
        );

        String updatedId = procedureRepository.update(updatedProcedure);
        assertEquals(newProcedure.id(), updatedId);

        // Verify procedure was updated
        var updatedResult = procedureRepository.findById(newProcedure.id());
        assertTrue(updatedResult.isPresent());
        assertEquals("Comprehensive physical examination (updated)", updatedResult.get().description());

        // Verify update throws exception on non-existent ID
        Procedure nonExistentProcedure = new Procedure(
                IdGenerator.generateId(),
                "Non-existent",
                "nonexistent",
                "Test",
                "Test"
        );
        assertThrows(org.springframework.dao.EmptyResultDataAccessException.class, () -> {
            procedureRepository.update(nonExistentProcedure);
        });
    }

    @Test
    void testDeleteAll() {
        // Create test procedure
        Procedure testProcedure = new Procedure(
                IdGenerator.generateId(),
                "Vaccination",
                "vaccination",
                "Immunization procedure",
                "Preventive"
        );

        procedureRepository.insert(testProcedure);
        assertTrue(procedureRepository.findByName("Vaccination").isPresent());

        // Delete all
        int deleted = procedureRepository.deleteAll();
        assertTrue(deleted >= 1);

        // Verify procedure was deleted
        assertTrue(procedureRepository.findByName("Vaccination").isEmpty());
    }

    @Test
    void testFindByNameWithNonExistentName() {
        // Test findByName with non-existent name
        var result = procedureRepository.findByName("Non-existent Procedure");
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindByNormalizedNameWithNonExistentName() {
        // Test findByNormalizedName with non-existent normalized name
        var result = procedureRepository.findByNormalizedName("nonexistentprocedure");
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindByCategoryWithNonExistentCategory() {
        // Test findByCategory with non-existent category
        List<Procedure> procedures = procedureRepository.findByCategory("Non-existent Category");
        assertNotNull(procedures);
        assertTrue(procedures.isEmpty());
    }

    @Test
    void testFindAllWithEmptyTable() {
        // Test findAll with empty table
        List<Procedure> procedures = procedureRepository.findAll();
        assertNotNull(procedures);
        assertTrue(procedures.isEmpty());
    }
}
