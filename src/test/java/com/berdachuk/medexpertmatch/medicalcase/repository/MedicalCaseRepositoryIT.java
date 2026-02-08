package com.berdachuk.medexpertmatch.medicalcase.repository;

import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for MedicalCaseRepository.
 * Uses Testcontainers PostgreSQL database.
 */
class MedicalCaseRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clear existing data to ensure test independence
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.clinical_experiences");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.consultation_matches");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.medical_cases");
    }

    @Test
    void testFindById() {
        // Create test case
        MedicalCase testCase = new MedicalCase(
                IdGenerator.generateId(),
                45,
                "Chest pain",
                "Severe chest pain radiating to left arm",
                "Acute myocardial infarction",
                List.of("I21.9"),
                List.of("22298006"),
                UrgencyLevel.CRITICAL,
                "Cardiology",
                CaseType.INPATIENT,
                "Patient requires immediate cardiac intervention",
                null
        );

        String id = medicalCaseRepository.insert(testCase);
        assertEquals(testCase.id(), id);

        // Test findById
        var result = medicalCaseRepository.findById(testCase.id());
        assertTrue(result.isPresent());
        assertEquals(45, result.get().patientAge());
        assertEquals("Chest pain", result.get().chiefComplaint());
        assertEquals(UrgencyLevel.CRITICAL, result.get().urgencyLevel());
        assertEquals(CaseType.INPATIENT, result.get().caseType());
        assertTrue(result.get().icd10Codes().contains("I21.9"));
    }

    @Test
    void testFindByIds() {
        // Create test cases
        MedicalCase case1 = new MedicalCase(
                IdGenerator.generateId(),
                30,
                "Headache",
                "Persistent headache for 3 days",
                "Migraine",
                List.of("G43.9"),
                List.of(),
                UrgencyLevel.MEDIUM,
                "Neurology",
                CaseType.SECOND_OPINION,
                null,
                null
        );

        MedicalCase case2 = new MedicalCase(
                IdGenerator.generateId(),
                55,
                "Shortness of breath",
                "Difficulty breathing on exertion",
                "Heart failure",
                List.of("I50.9"),
                List.of(),
                UrgencyLevel.HIGH,
                "Cardiology",
                CaseType.CONSULT_REQUEST,
                null,
                null
        );

        String id1 = medicalCaseRepository.insert(case1);
        String id2 = medicalCaseRepository.insert(case2);

        // Test findByIds
        List<MedicalCase> cases = medicalCaseRepository.findByIds(List.of(id1, id2));
        assertNotNull(cases);
        assertEquals(2, cases.size());
        assertTrue(cases.stream().anyMatch(c -> c.id().equals(id1)));
        assertTrue(cases.stream().anyMatch(c -> c.id().equals(id2)));
    }

    @Test
    void testFindByUrgencyLevel() {
        // Create test cases with different urgency levels
        MedicalCase criticalCase = new MedicalCase(
                IdGenerator.generateId(),
                40,
                "Severe trauma",
                "Multiple injuries",
                "Trauma",
                List.of("S00-T88"),
                List.of(),
                UrgencyLevel.CRITICAL,
                "Emergency Medicine",
                CaseType.INPATIENT,
                null,
                null
        );

        MedicalCase highCase = new MedicalCase(
                IdGenerator.generateId(),
                50,
                "Acute pain",
                "Severe abdominal pain",
                "Appendicitis",
                List.of("K35.9"),
                List.of(),
                UrgencyLevel.HIGH,
                "Surgery",
                CaseType.INPATIENT,
                null,
                null
        );

        medicalCaseRepository.insert(criticalCase);
        medicalCaseRepository.insert(highCase);

        // Test findByUrgencyLevel
        List<MedicalCase> criticalCases = medicalCaseRepository.findByUrgencyLevel("CRITICAL", 10);
        assertNotNull(criticalCases);
        assertTrue(criticalCases.size() >= 1);
        assertTrue(criticalCases.stream().anyMatch(c -> c.id().equals(criticalCase.id())));
    }

    @Test
    void testFindByCaseType() {
        // Create test cases with different types
        MedicalCase inpatientCase = new MedicalCase(
                IdGenerator.generateId(),
                35,
                "Fever",
                "High fever for 2 days",
                "Infection",
                List.of("A00-B99"),
                List.of(),
                UrgencyLevel.MEDIUM,
                "Internal Medicine",
                CaseType.INPATIENT,
                null,
                null
        );

        MedicalCase secondOpinionCase = new MedicalCase(
                IdGenerator.generateId(),
                60,
                "Cancer diagnosis",
                "Seeking second opinion",
                "Cancer",
                List.of("C00-D49"),
                List.of(),
                UrgencyLevel.HIGH,
                "Oncology",
                CaseType.SECOND_OPINION,
                null,
                null
        );

        medicalCaseRepository.insert(inpatientCase);
        medicalCaseRepository.insert(secondOpinionCase);

        // Test findByCaseType
        List<MedicalCase> secondOpinions = medicalCaseRepository.findByCaseType("SECOND_OPINION", 10);
        assertNotNull(secondOpinions);
        assertTrue(secondOpinions.size() >= 1);
        assertTrue(secondOpinions.stream().anyMatch(c -> c.id().equals(secondOpinionCase.id())));
    }

    @Test
    void testFindByRequiredSpecialty() {
        // Create test cases
        MedicalCase cardioCase = new MedicalCase(
                IdGenerator.generateId(),
                65,
                "Heart rhythm issues",
                "Irregular heartbeat",
                "Arrhythmia",
                List.of("I49.9"),
                List.of(),
                UrgencyLevel.HIGH,
                "Cardiology",
                CaseType.CONSULT_REQUEST,
                null,
                null
        );

        medicalCaseRepository.insert(cardioCase);

        // Test findByRequiredSpecialty
        List<MedicalCase> cardioCases = medicalCaseRepository.findByRequiredSpecialty("Cardiology", 10);
        assertNotNull(cardioCases);
        assertTrue(cardioCases.size() >= 1);
        assertTrue(cardioCases.stream().anyMatch(c -> c.id().equals(cardioCase.id())));
    }

    @Test
    void testFindByIcd10Code() {
        // Create test case
        MedicalCase testCase = new MedicalCase(
                IdGenerator.generateId(),
                25,
                "Diabetes management",
                "Blood sugar control issues",
                "Type 2 Diabetes",
                List.of("E11.9"),
                List.of(),
                UrgencyLevel.MEDIUM,
                "Endocrinology",
                CaseType.SECOND_OPINION,
                null,
                null
        );

        medicalCaseRepository.insert(testCase);

        // Test findByIcd10Code
        List<MedicalCase> cases = medicalCaseRepository.findByIcd10Code("E11.9", 10);
        assertNotNull(cases);
        assertTrue(cases.size() >= 1);
        assertTrue(cases.stream().anyMatch(c -> c.id().equals(testCase.id())));
    }

    @Test
    void testInsert() {
        // Create case
        MedicalCase newCase = new MedicalCase(
                IdGenerator.generateId(),
                40,
                "Test complaint",
                "Test symptoms",
                "Test diagnosis",
                List.of("Z00.0"),
                List.of(),
                UrgencyLevel.LOW,
                "General Practice",
                CaseType.CONSULT_REQUEST,
                "Test notes",
                null
        );

        String id = medicalCaseRepository.insert(newCase);
        assertEquals(newCase.id(), id);

        // Verify case was created
        var result = medicalCaseRepository.findById(newCase.id());
        assertTrue(result.isPresent());
        assertEquals("Test complaint", result.get().chiefComplaint());

        // Verify insert throws exception on duplicate ID
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            medicalCaseRepository.insert(newCase);
        });
    }

    @Test
    void testUpdate() {
        // Create case first
        MedicalCase newCase = new MedicalCase(
                IdGenerator.generateId(),
                40,
                "Test complaint",
                "Test symptoms",
                "Test diagnosis",
                List.of("Z00.0"),
                List.of(),
                UrgencyLevel.LOW,
                "General Practice",
                CaseType.CONSULT_REQUEST,
                "Test notes",
                null
        );
        medicalCaseRepository.insert(newCase);

        // Update case
        MedicalCase updatedCase = new MedicalCase(
                newCase.id(),
                40,
                "Updated complaint",
                "Updated symptoms",
                "Updated diagnosis",
                List.of("Z00.0", "Z00.1"),
                List.of(),
                UrgencyLevel.MEDIUM,
                "General Practice",
                CaseType.CONSULT_REQUEST,
                "Updated notes",
                null
        );

        String updatedId = medicalCaseRepository.update(updatedCase);
        assertEquals(newCase.id(), updatedId);

        // Verify case was updated
        var updatedResult = medicalCaseRepository.findById(newCase.id());
        assertTrue(updatedResult.isPresent());
        assertEquals("Updated complaint", updatedResult.get().chiefComplaint());
        assertEquals(2, updatedResult.get().icd10Codes().size());

        // Verify update throws exception on non-existent ID
        MedicalCase nonExistentCase = new MedicalCase(
                IdGenerator.generateId(),
                30,
                "Non-existent",
                "Non-existent",
                "Non-existent",
                List.of(),
                List.of(),
                UrgencyLevel.LOW,
                "General Practice",
                CaseType.CONSULT_REQUEST,
                null,
                null
        );
        assertThrows(org.springframework.dao.EmptyResultDataAccessException.class, () -> {
            medicalCaseRepository.update(nonExistentCase);
        });
    }

    @Test
    void testDeleteAll() {
        // Create test case
        MedicalCase testCase = new MedicalCase(
                IdGenerator.generateId(),
                30,
                "Test",
                "Test",
                "Test",
                List.of(),
                List.of(),
                UrgencyLevel.LOW,
                "General Practice",
                CaseType.CONSULT_REQUEST,
                null,
                null
        );

        medicalCaseRepository.insert(testCase);
        assertTrue(medicalCaseRepository.findById(testCase.id()).isPresent());

        // Delete all
        int deleted = medicalCaseRepository.deleteAll();
        assertTrue(deleted >= 1);

        // Verify case was deleted
        assertTrue(medicalCaseRepository.findById(testCase.id()).isEmpty());
    }

    @Test
    void testSearch_ByQuery() {
        // Create test cases
        MedicalCase case1 = new MedicalCase(
                IdGenerator.generateId(),
                45,
                "Chest pain",
                "Severe chest pain radiating to left arm",
                "Acute myocardial infarction",
                List.of("I21.9"),
                List.of(),
                UrgencyLevel.CRITICAL,
                "Cardiology",
                CaseType.INPATIENT,
                "Patient requires immediate cardiac intervention",
                null
        );

        MedicalCase case2 = new MedicalCase(
                IdGenerator.generateId(),
                30,
                "Headache",
                "Persistent headache for 3 days",
                "Migraine",
                List.of("G43.9"),
                List.of(),
                UrgencyLevel.MEDIUM,
                "Neurology",
                CaseType.SECOND_OPINION,
                "Patient has history of migraines",
                null
        );

        medicalCaseRepository.insert(case1);
        medicalCaseRepository.insert(case2);

        // Search by query matching chief complaint
        List<MedicalCase> results = medicalCaseRepository.search("chest", null, null, null, 0, 10);
        assertEquals(1, results.size());
        assertEquals("Chest pain", results.get(0).chiefComplaint());

        // Search by query matching symptoms
        results = medicalCaseRepository.search("headache", null, null, null, 0, 10);
        assertEquals(1, results.size());
        assertEquals("Headache", results.get(0).chiefComplaint());

        // Search by query matching additional notes
        results = medicalCaseRepository.search("migraines", null, null, null, 0, 10);
        assertEquals(1, results.size());
        assertEquals("Headache", results.get(0).chiefComplaint());
    }

    @Test
    void testSearch_BySpecialty() {
        // Create test cases
        MedicalCase case1 = new MedicalCase(
                IdGenerator.generateId(),
                45,
                "Chest pain",
                "Severe chest pain",
                null,
                List.of(),
                List.of(),
                UrgencyLevel.HIGH,
                "Cardiology",
                CaseType.INPATIENT,
                null,
                null
        );

        MedicalCase case2 = new MedicalCase(
                IdGenerator.generateId(),
                30,
                "Headache",
                "Persistent headache",
                null,
                List.of(),
                List.of(),
                UrgencyLevel.MEDIUM,
                "Neurology",
                CaseType.SECOND_OPINION,
                null,
                null
        );

        medicalCaseRepository.insert(case1);
        medicalCaseRepository.insert(case2);

        // Search by specialty
        List<MedicalCase> results = medicalCaseRepository.search(null, "Cardiology", null, null, 0, 10);
        assertEquals(1, results.size());
        assertEquals("Cardiology", results.get(0).requiredSpecialty());

        results = medicalCaseRepository.search(null, "Neurology", null, null, 0, 10);
        assertEquals(1, results.size());
        assertEquals("Neurology", results.get(0).requiredSpecialty());
    }

    @Test
    void testSearch_ByUrgencyLevel() {
        // Create test cases
        MedicalCase case1 = new MedicalCase(
                IdGenerator.generateId(),
                45,
                "Chest pain",
                "Severe chest pain",
                null,
                List.of(),
                List.of(),
                UrgencyLevel.CRITICAL,
                "Cardiology",
                CaseType.INPATIENT,
                null,
                null
        );

        MedicalCase case2 = new MedicalCase(
                IdGenerator.generateId(),
                30,
                "Headache",
                "Persistent headache",
                null,
                List.of(),
                List.of(),
                UrgencyLevel.LOW,
                "Neurology",
                CaseType.SECOND_OPINION,
                null,
                null
        );

        medicalCaseRepository.insert(case1);
        medicalCaseRepository.insert(case2);

        // Search by urgency level
        List<MedicalCase> results = medicalCaseRepository.search(null, null, "CRITICAL", null, 0, 10);
        assertEquals(1, results.size());
        assertEquals(UrgencyLevel.CRITICAL, results.get(0).urgencyLevel());

        results = medicalCaseRepository.search(null, null, "LOW", null, 0, 10);
        assertEquals(1, results.size());
        assertEquals(UrgencyLevel.LOW, results.get(0).urgencyLevel());
    }

    @Test
    void testSearch_WithAllFilters() {
        // Create test cases
        MedicalCase case1 = new MedicalCase(
                IdGenerator.generateId(),
                45,
                "Chest pain",
                "Severe chest pain radiating to left arm",
                null,
                List.of(),
                List.of(),
                UrgencyLevel.HIGH,
                "Cardiology",
                CaseType.INPATIENT,
                "Patient requires immediate attention",
                null
        );

        MedicalCase case2 = new MedicalCase(
                IdGenerator.generateId(),
                30,
                "Headache",
                "Persistent headache",
                null,
                List.of(),
                List.of(),
                UrgencyLevel.MEDIUM,
                "Neurology",
                CaseType.SECOND_OPINION,
                null,
                null
        );

        medicalCaseRepository.insert(case1);
        medicalCaseRepository.insert(case2);

        // Search with all filters
        List<MedicalCase> results = medicalCaseRepository.search("chest", "Cardiology", "HIGH", null, 0, 10);
        assertEquals(1, results.size());
        assertEquals("Chest pain", results.get(0).chiefComplaint());
        assertEquals("Cardiology", results.get(0).requiredSpecialty());
        assertEquals(UrgencyLevel.HIGH, results.get(0).urgencyLevel());

        // Search with filters that don't match
        results = medicalCaseRepository.search("chest", "Neurology", "HIGH", null, 0, 10);
        assertEquals(0, results.size());
    }

    @Test
    void testSearch_WithMaxResults() {
        // Create multiple test cases
        for (int i = 0; i < 5; i++) {
            MedicalCase testCase = new MedicalCase(
                    IdGenerator.generateId(),
                    30 + i,
                    "Test case " + i,
                    "Symptoms " + i,
                    null,
                    List.of(),
                    List.of(),
                    UrgencyLevel.MEDIUM,
                    "Cardiology",
                    CaseType.INPATIENT,
                    null,
                    null
            );
            medicalCaseRepository.insert(testCase);
        }

        // Search with maxResults limit
        List<MedicalCase> results = medicalCaseRepository.search("Test", null, null, null, 0, 3);
        assertTrue(results.size() <= 3, "Results should be limited to maxResults");
    }

    @Test
    void testSearch_EmptyQuery() {
        // Create test case
        MedicalCase testCase = new MedicalCase(
                IdGenerator.generateId(),
                45,
                "Chest pain",
                "Severe chest pain",
                null,
                List.of(),
                List.of(),
                UrgencyLevel.HIGH,
                "Cardiology",
                CaseType.INPATIENT,
                null,
                null
        );
        medicalCaseRepository.insert(testCase);

        // Search with empty/null query should return all cases
        List<MedicalCase> results = medicalCaseRepository.search(null, null, null, null, 0, 10);
        assertFalse(results.isEmpty(), "Should return cases even with null query");

        results = medicalCaseRepository.search("", null, null, null, 0, 10);
        assertFalse(results.isEmpty(), "Should return cases even with empty query");
    }

    @Test
    void testSearch_CaseInsensitive() {
        // Create test case
        MedicalCase testCase = new MedicalCase(
                IdGenerator.generateId(),
                45,
                "Chest pain",
                "Severe chest pain radiating to left arm",
                null,
                List.of(),
                List.of(),
                UrgencyLevel.HIGH,
                "Cardiology",
                CaseType.INPATIENT,
                null,
                null
        );
        medicalCaseRepository.insert(testCase);

        // Test case-insensitive search
        List<MedicalCase> results = medicalCaseRepository.search("CHEST", null, null, null, 0, 10);
        assertEquals(1, results.size());
        assertEquals("Chest pain", results.get(0).chiefComplaint());

        results = medicalCaseRepository.search("ChEsT", null, null, null, 0, 10);
        assertEquals(1, results.size());

        results = medicalCaseRepository.search("RADIATING", null, null, null, 0, 10);
        assertEquals(1, results.size());
        assertTrue(results.get(0).symptoms().toLowerCase().contains("radiating"));
    }

    @Test
    void testSearch_PartialMatch() {
        // Create test cases
        MedicalCase case1 = new MedicalCase(
                IdGenerator.generateId(),
                45,
                "Chest pain and discomfort",
                "Severe chest pain",
                null,
                List.of(),
                List.of(),
                UrgencyLevel.HIGH,
                "Cardiology",
                CaseType.INPATIENT,
                null,
                null
        );

        MedicalCase case2 = new MedicalCase(
                IdGenerator.generateId(),
                30,
                "Headache and dizziness",
                "Persistent headache",
                null,
                List.of(),
                List.of(),
                UrgencyLevel.MEDIUM,
                "Neurology",
                CaseType.SECOND_OPINION,
                null,
                null
        );

        medicalCaseRepository.insert(case1);
        medicalCaseRepository.insert(case2);

        // Test partial match in chief complaint
        List<MedicalCase> results = medicalCaseRepository.search("pain", null, null, null, 0, 10);
        assertEquals(1, results.size());
        assertTrue(results.get(0).chiefComplaint().toLowerCase().contains("pain"));

        // Test partial match in symptoms
        results = medicalCaseRepository.search("persistent", null, null, null, 0, 10);
        assertEquals(1, results.size());
        assertTrue(results.get(0).symptoms().toLowerCase().contains("persistent"));
    }

    @Test
    void testSearch_OrderByCreatedAt() {
        // Create multiple test cases
        for (int i = 0; i < 3; i++) {
            MedicalCase testCase = new MedicalCase(
                    IdGenerator.generateId(),
                    30 + i,
                    "Test case " + i,
                    "Symptoms",
                    null,
                    List.of(),
                    List.of(),
                    UrgencyLevel.MEDIUM,
                    "Cardiology",
                    CaseType.INPATIENT,
                    null,
                    null
            );
            medicalCaseRepository.insert(testCase);
            // Small delay to ensure different timestamps
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Search should return results ordered by created_at DESC (most recent first)
        List<MedicalCase> results = medicalCaseRepository.search("Test", null, null, null, 0, 10);
        assertTrue(results.size() >= 3, "Should return at least 3 test cases");

        // Verify ordering (most recent first)
        // The last inserted case should be first in results
        assertTrue(results.get(0).chiefComplaint().contains("Test case"));
    }

    @Test
    void testHasEmbedding() {
        // Create test case without embedding
        MedicalCase caseWithoutEmbedding = new MedicalCase(
                IdGenerator.generateId(),
                45,
                "Chest pain",
                "Severe chest pain",
                "Acute myocardial infarction",
                List.of("I21.9"),
                List.of(),
                UrgencyLevel.CRITICAL,
                "Cardiology",
                CaseType.INPATIENT,
                null,
                null
        );

        String caseId = medicalCaseRepository.insert(caseWithoutEmbedding);

        // Test case without embedding
        assertFalse(medicalCaseRepository.hasEmbedding(caseId));

        // Create test embedding
        List<Double> embedding = new java.util.ArrayList<>();
        for (int i = 0; i < 1536; i++) {
            embedding.add(0.1 + (i * 0.001));
        }

        // Add embedding to case
        medicalCaseRepository.updateEmbedding(caseId, embedding, 1536);

        // Test case with embedding
        assertTrue(medicalCaseRepository.hasEmbedding(caseId));
    }

    @Test
    void testCalculateVectorSimilarity() {
        // Create test cases
        MedicalCase case1 = new MedicalCase(
                IdGenerator.generateId(),
                45,
                "Chest pain",
                "Severe chest pain",
                "Acute myocardial infarction",
                List.of("I21.9"),
                List.of(),
                UrgencyLevel.CRITICAL,
                "Cardiology",
                CaseType.INPATIENT,
                null,
                null
        );

        MedicalCase case2 = new MedicalCase(
                IdGenerator.generateId(),
                50,
                "Heart attack",
                "Acute myocardial infarction",
                "Myocardial infarction",
                List.of("I21.9"),
                List.of(),
                UrgencyLevel.CRITICAL,
                "Cardiology",
                CaseType.INPATIENT,
                null,
                null
        );

        String case1Id = medicalCaseRepository.insert(case1);
        String case2Id = medicalCaseRepository.insert(case2);

        // Create test embeddings
        List<Double> embedding1 = new java.util.ArrayList<>();
        List<Double> embedding2 = new java.util.ArrayList<>();
        for (int i = 0; i < 1536; i++) {
            embedding1.add(0.1 + (i * 0.001));
            embedding2.add(0.15 + (i * 0.001)); // Slightly different
        }

        // Add embeddings to cases
        medicalCaseRepository.updateEmbedding(case1Id, embedding1, 1536);
        medicalCaseRepository.updateEmbedding(case2Id, embedding2, 1536);

        // Test vector similarity calculation
        Double similarity = medicalCaseRepository.calculateVectorSimilarity(case1Id, List.of(case2Id));

        // Should return a valid similarity value (between 0 and 1)
        assertNotNull(similarity);
        assertTrue(similarity >= 0 && similarity <= 1);
    }
}
