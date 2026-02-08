package com.berdachuk.medexpertmatch.medicalcoding.repository;

import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.berdachuk.medexpertmatch.medicalcoding.domain.ICD10Code;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for ICD10CodeRepository.
 * Uses Testcontainers PostgreSQL database.
 */
class ICD10CodeRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private ICD10CodeRepository icd10CodeRepository;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clear existing data to ensure test independence
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.icd10_codes");
    }

    @Test
    void testFindById() {
        // Create test ICD-10 code
        ICD10Code testCode = new ICD10Code(
                IdGenerator.generateId(),
                "I21.9",
                "Acute myocardial infarction, unspecified",
                "Diseases of the circulatory system",
                "I21",
                List.of("I21.0", "I21.1", "I21.2")
        );

        String id = icd10CodeRepository.insert(testCode);

        // Test findById
        var result = icd10CodeRepository.findById(id);
        assertTrue(result.isPresent());
        assertEquals("I21.9", result.get().code());
        assertEquals("Acute myocardial infarction, unspecified", result.get().description());
        assertEquals("Diseases of the circulatory system", result.get().category());
    }

    @Test
    void testFindByCode() {
        // Create test ICD-10 code
        ICD10Code testCode = new ICD10Code(
                IdGenerator.generateId(),
                "E11.9",
                "Type 2 diabetes mellitus without complications",
                "Endocrine, nutritional and metabolic diseases",
                "E11",
                List.of()
        );

        icd10CodeRepository.insert(testCode);

        // Test findByCode
        var result = icd10CodeRepository.findByCode("E11.9");
        assertTrue(result.isPresent());
        assertEquals("E11.9", result.get().code());
        assertEquals("Type 2 diabetes mellitus without complications", result.get().description());
    }

    @Test
    void testFindByCodes() {
        // Create test ICD-10 codes
        ICD10Code code1 = new ICD10Code(
                IdGenerator.generateId(),
                "I21.9",
                "Acute myocardial infarction, unspecified",
                "Diseases of the circulatory system",
                "I21",
                List.of()
        );

        ICD10Code code2 = new ICD10Code(
                IdGenerator.generateId(),
                "E11.9",
                "Type 2 diabetes mellitus without complications",
                "Endocrine, nutritional and metabolic diseases",
                "E11",
                List.of()
        );

        icd10CodeRepository.insert(code1);
        icd10CodeRepository.insert(code2);

        // Test findByCodes
        List<ICD10Code> codes = icd10CodeRepository.findByCodes(List.of("I21.9", "E11.9"));
        assertNotNull(codes);
        assertEquals(2, codes.size());
        assertTrue(codes.stream().anyMatch(c -> c.code().equals("I21.9")));
        assertTrue(codes.stream().anyMatch(c -> c.code().equals("E11.9")));
    }

    @Test
    void testFindByCategory() {
        // Create test ICD-10 codes
        ICD10Code code1 = new ICD10Code(
                IdGenerator.generateId(),
                "I21.9",
                "Acute myocardial infarction, unspecified",
                "Diseases of the circulatory system",
                "I21",
                List.of()
        );

        ICD10Code code2 = new ICD10Code(
                IdGenerator.generateId(),
                "I50.9",
                "Heart failure, unspecified",
                "Diseases of the circulatory system",
                "I50",
                List.of()
        );

        icd10CodeRepository.insert(code1);
        icd10CodeRepository.insert(code2);

        // Test findByCategory
        List<ICD10Code> codes = icd10CodeRepository.findByCategory("Diseases of the circulatory system", 10);
        assertNotNull(codes);
        assertTrue(codes.size() >= 2);
        assertTrue(codes.stream().allMatch(c -> c.category().equals("Diseases of the circulatory system")));
    }

    @Test
    void testFindByParentCode() {
        // Create test ICD-10 codes
        ICD10Code parentCode = new ICD10Code(
                IdGenerator.generateId(),
                "I21",
                "Acute myocardial infarction",
                "Diseases of the circulatory system",
                null,
                List.of()
        );

        ICD10Code childCode1 = new ICD10Code(
                IdGenerator.generateId(),
                "I21.0",
                "Acute transmural myocardial infarction of anterior wall",
                "Diseases of the circulatory system",
                "I21",
                List.of()
        );

        ICD10Code childCode2 = new ICD10Code(
                IdGenerator.generateId(),
                "I21.9",
                "Acute myocardial infarction, unspecified",
                "Diseases of the circulatory system",
                "I21",
                List.of()
        );

        icd10CodeRepository.insert(parentCode);
        icd10CodeRepository.insert(childCode1);
        icd10CodeRepository.insert(childCode2);

        // Test findByParentCode
        List<ICD10Code> childCodes = icd10CodeRepository.findByParentCode("I21");
        assertNotNull(childCodes);
        assertEquals(2, childCodes.size());
        assertTrue(childCodes.stream().allMatch(c -> c.parentCode().equals("I21")));
    }

    @Test
    void testInsert() {
        // Create code
        ICD10Code newCode = new ICD10Code(
                IdGenerator.generateId(),
                "G43.9",
                "Migraine, unspecified",
                "Diseases of the nervous system",
                "G43",
                List.of()
        );

        String id = icd10CodeRepository.insert(newCode);
        assertEquals(newCode.id(), id);

        // Verify code was created
        var result = icd10CodeRepository.findByCode("G43.9");
        assertTrue(result.isPresent());
        assertEquals("Migraine, unspecified", result.get().description());

        // Verify insert throws exception on duplicate ID
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            icd10CodeRepository.insert(newCode);
        });
    }

    @Test
    void testUpdate() {
        // Create code first
        ICD10Code newCode = new ICD10Code(
                IdGenerator.generateId(),
                "G43.9",
                "Migraine, unspecified",
                "Diseases of the nervous system",
                "G43",
                List.of()
        );
        icd10CodeRepository.insert(newCode);

        // Update code
        ICD10Code updatedCode = new ICD10Code(
                newCode.id(),
                "G43.9",
                "Migraine, unspecified (updated)",
                "Diseases of the nervous system",
                "G43",
                List.of("G43.0", "G43.1")
        );

        String updatedId = icd10CodeRepository.update(updatedCode);
        assertEquals(newCode.id(), updatedId);

        // Verify code was updated
        var updatedResult = icd10CodeRepository.findByCode("G43.9");
        assertTrue(updatedResult.isPresent());
        assertEquals("Migraine, unspecified (updated)", updatedResult.get().description());
        assertEquals(2, updatedResult.get().relatedCodes().size());

        // Verify update throws exception on non-existent ID
        ICD10Code nonExistentCode = new ICD10Code(
                IdGenerator.generateId(),
                "Z99.9",
                "Non-existent",
                "Test",
                null,
                List.of()
        );
        assertThrows(org.springframework.dao.EmptyResultDataAccessException.class, () -> {
            icd10CodeRepository.update(nonExistentCode);
        });
    }

    @Test
    void testDeleteAll() {
        // Create test code
        ICD10Code testCode = new ICD10Code(
                IdGenerator.generateId(),
                "Z00.0",
                "General examination without complaint",
                "Factors influencing health status",
                null,
                List.of()
        );

        icd10CodeRepository.insert(testCode);
        assertTrue(icd10CodeRepository.findByCode("Z00.0").isPresent());

        // Delete all
        int deleted = icd10CodeRepository.deleteAll();
        assertTrue(deleted >= 1);

        // Verify code was deleted
        assertTrue(icd10CodeRepository.findByCode("Z00.0").isEmpty());
    }
}
