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
 * Integration test for MedicalCaseRepository embedding methods.
 * Tests findWithoutEmbeddings() and updateEmbedding() methods.
 */
class MedicalCaseRepositoryEmbeddingIT extends BaseIntegrationTest {

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
    void testFindWithoutEmbeddings() {
        // Create test cases without embeddings
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
                30,
                "Headache",
                "Severe headache",
                "Migraine",
                List.of("G43.9"),
                List.of(),
                UrgencyLevel.MEDIUM,
                "Neurology",
                CaseType.SECOND_OPINION,
                null,
                null
        );

        medicalCaseRepository.insert(case1);
        medicalCaseRepository.insert(case2);

        // Find cases without embeddings
        List<MedicalCase> casesWithoutEmbeddings = medicalCaseRepository.findWithoutEmbeddings();

        assertNotNull(casesWithoutEmbeddings);
        assertTrue(casesWithoutEmbeddings.size() >= 2);
        assertTrue(casesWithoutEmbeddings.stream().anyMatch(c -> c.id().equals(case1.id())));
        assertTrue(casesWithoutEmbeddings.stream().anyMatch(c -> c.id().equals(case2.id())));
    }

    @Test
    void testUpdateEmbedding() {
        // Create test case
        MedicalCase testCase = new MedicalCase(
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

        medicalCaseRepository.insert(testCase);

        // Create test embedding (1536 dimensions)
        List<Double> embedding = new java.util.ArrayList<>();
        for (int i = 0; i < 1536; i++) {
            embedding.add(0.1 + (i * 0.001));
        }

        // Update embedding
        medicalCaseRepository.updateEmbedding(testCase.id(), embedding, 1536);

        // Verify case no longer appears in findWithoutEmbeddings
        List<MedicalCase> casesWithoutEmbeddings = medicalCaseRepository.findWithoutEmbeddings();
        assertFalse(casesWithoutEmbeddings.stream().anyMatch(c -> c.id().equals(testCase.id())));

        // Verify embedding was stored (check database directly)
        String checkEmbeddingSql = """
                SELECT embedding IS NOT NULL as has_embedding,
                       embedding_dimension
                FROM medexpertmatch.medical_cases
                WHERE id = :id
                """;
        Boolean hasEmbedding = namedJdbcTemplate.queryForObject(
                checkEmbeddingSql,
                java.util.Map.of("id", testCase.id()),
                (rs, rowNum) -> rs.getBoolean("has_embedding"));
        Integer dimension = namedJdbcTemplate.queryForObject(
                checkEmbeddingSql,
                java.util.Map.of("id", testCase.id()),
                (rs, rowNum) -> rs.getInt("embedding_dimension"));

        assertTrue(hasEmbedding);
        assertEquals(1536, dimension);
    }

    @Test
    void testUpdateEmbeddingWithSmallerDimension() {
        // Create test case
        MedicalCase testCase = new MedicalCase(
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

        medicalCaseRepository.insert(testCase);

        // Create test embedding with smaller dimension (should be padded)
        List<Double> embedding = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            embedding.add(0.1 + (i * 0.001));
        }

        // Update embedding (should normalize to 1536 dimensions)
        medicalCaseRepository.updateEmbedding(testCase.id(), embedding, 100);

        // Verify embedding dimension is normalized to 1536
        String checkDimensionSql = """
                SELECT embedding_dimension
                FROM medexpertmatch.medical_cases
                WHERE id = :id
                """;
        Integer dimension = namedJdbcTemplate.queryForObject(
                checkDimensionSql,
                java.util.Map.of("id", testCase.id()),
                Integer.class);

        assertEquals(1536, dimension);
    }

    @Test
    void testFindWithoutEmbeddingsExcludesCasesWithEmbeddings() {
        // Create two cases
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
                30,
                "Headache",
                "Severe headache",
                "Migraine",
                List.of("G43.9"),
                List.of(),
                UrgencyLevel.MEDIUM,
                "Neurology",
                CaseType.SECOND_OPINION,
                null,
                null
        );

        medicalCaseRepository.insert(case1);
        medicalCaseRepository.insert(case2);

        // Add embedding to case1
        List<Double> embedding = new java.util.ArrayList<>();
        for (int i = 0; i < 1536; i++) {
            embedding.add(0.1);
        }
        medicalCaseRepository.updateEmbedding(case1.id(), embedding, 1536);

        // Find cases without embeddings - should only return case2
        List<MedicalCase> casesWithoutEmbeddings = medicalCaseRepository.findWithoutEmbeddings();

        assertNotNull(casesWithoutEmbeddings);
        assertFalse(casesWithoutEmbeddings.stream().anyMatch(c -> c.id().equals(case1.id())));
        assertTrue(casesWithoutEmbeddings.stream().anyMatch(c -> c.id().equals(case2.id())));
    }
}
