package com.berdachuk.medexpertmatch.embedding.service;

import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for EmbeddingService.
 * Tests embedding generation using real EmbeddingModel (mocked in test profile).
 */
class EmbeddingServiceIT extends BaseIntegrationTest {

    @Autowired
    private EmbeddingService embeddingService;

    @Test
    void testGenerateEmbedding() {
        String text = "Patient presents with chest pain and shortness of breath. Diagnosis: Acute myocardial infarction. ICD-10: I21.9";

        List<Double> embedding = embeddingService.generateEmbedding(text);

        assertNotNull(embedding);
        assertFalse(embedding.isEmpty());
        // Embeddings should have reasonable dimension (typically 1536 for text-embedding-3-large)
        assertTrue(embedding.size() > 100, "Embedding should have reasonable dimension");
    }

    @Test
    void testGenerateEmbeddingsBatch() {
        List<String> texts = List.of(
                "Chest pain and shortness of breath",
                "Fever and headache",
                "Abdominal pain and nausea"
        );

        List<List<Double>> embeddings = embeddingService.generateEmbeddings(texts);

        assertNotNull(embeddings);
        assertEquals(texts.size(), embeddings.size());
        embeddings.forEach(embedding -> {
            assertNotNull(embedding);
            assertFalse(embedding.isEmpty());
        });
    }

    @Test
    void testGenerateEmbeddingAsFloatArray() {
        String text = "Medical case with symptoms and diagnosis";

        float[] embedding = embeddingService.generateEmbeddingAsFloatArray(text);

        assertNotNull(embedding);
        assertTrue(embedding.length > 100, "Embedding should have reasonable dimension");
    }

    @Test
    void testGenerateEmbeddingEmptyText() {
        String text = "";

        List<Double> embedding = embeddingService.generateEmbedding(text);

        // Empty text may return empty embedding or a valid embedding depending on model
        assertNotNull(embedding);
    }

    @Test
    void testGenerateEmbeddingForMedicalCase() {
        MedicalCase medicalCase = new MedicalCase(
                IdGenerator.generateId(),
                45,
                "Chest pain",
                "Severe chest pain, shortness of breath",
                "Acute MI",
                List.of("I21.9"),
                List.of(),
                UrgencyLevel.HIGH,
                "Cardiology",
                CaseType.INPATIENT,
                "Patient requires immediate attention",
                null
        );

        List<Double> embedding = embeddingService.generateEmbeddingForMedicalCase(medicalCase);

        assertNotNull(embedding);
        assertFalse(embedding.isEmpty());
        // Embeddings should have reasonable dimension (typically 1536 for text-embedding-3-large)
        assertTrue(embedding.size() > 100, "Embedding should have reasonable dimension");
    }

    @Test
    void testGenerateEmbeddingForMedicalCase_WithStoredAbstract() {
        String storedAbstract = "Comprehensive medical case abstract: Patient presents with chest pain and shortness of breath. Diagnosis: Acute myocardial infarction. ICD-10: I21.9. Requires cardiology consultation.";
        MedicalCase medicalCase = new MedicalCase(
                IdGenerator.generateId(),
                45,
                "Chest pain",
                "Severe chest pain, shortness of breath",
                "Acute MI",
                List.of("I21.9"),
                List.of(),
                UrgencyLevel.HIGH,
                "Cardiology",
                CaseType.INPATIENT,
                "Patient requires immediate attention",
                storedAbstract
        );

        List<Double> embedding = embeddingService.generateEmbeddingForMedicalCase(medicalCase);

        assertNotNull(embedding);
        assertFalse(embedding.isEmpty());
        assertTrue(embedding.size() > 100, "Embedding should have reasonable dimension");
    }

    @Test
    void testGenerateEmbeddingForMedicalCase_WithoutStoredAbstract() {
        MedicalCase medicalCase = new MedicalCase(
                IdGenerator.generateId(),
                45,
                "Chest pain",
                "Severe chest pain, shortness of breath",
                "Acute MI",
                List.of("I21.9"),
                List.of(),
                UrgencyLevel.HIGH,
                "Cardiology",
                CaseType.INPATIENT,
                "Patient requires immediate attention",
                null
        );

        List<Double> embedding = embeddingService.generateEmbeddingForMedicalCase(medicalCase);

        assertNotNull(embedding);
        assertFalse(embedding.isEmpty());
        assertTrue(embedding.size() > 100, "Embedding should have reasonable dimension");
    }

    @Test
    void testGenerateEmbeddingForMedicalCase_WithNullFields() {
        // Test that method handles null fields gracefully
        MedicalCase medicalCase = new MedicalCase(
                IdGenerator.generateId(),
                null,
                null,
                "Some symptoms",
                null,
                null,
                List.of(),
                UrgencyLevel.MEDIUM,
                null,
                CaseType.CONSULT_REQUEST,
                null,
                null
        );

        List<Double> embedding = embeddingService.generateEmbeddingForMedicalCase(medicalCase);

        assertNotNull(embedding);
        // Should still generate embedding even with null fields (fallback to simple text)
        assertFalse(embedding.isEmpty());
    }

    @Test
    void testGenerateEmbeddingForMedicalCase_WithEmptyLists() {
        MedicalCase medicalCase = new MedicalCase(
                IdGenerator.generateId(),
                30,
                "Headache",
                "Persistent headache",
                "Migraine",
                List.of(), // Empty ICD-10 codes
                List.of(), // Empty SNOMED codes
                UrgencyLevel.LOW,
                "Neurology",
                CaseType.CONSULT_REQUEST,
                "Routine consultation",
                null
        );

        List<Double> embedding = embeddingService.generateEmbeddingForMedicalCase(medicalCase);

        assertNotNull(embedding);
        assertFalse(embedding.isEmpty());
        assertTrue(embedding.size() > 100, "Embedding should have reasonable dimension");
    }

    @Test
    void testGenerateEmbeddingsForMedicalCases_Batch() {
        List<MedicalCase> medicalCases = List.of(
                new MedicalCase(
                        IdGenerator.generateId(),
                        45,
                        "Chest pain",
                        "Severe chest pain, shortness of breath",
                        "Acute MI",
                        List.of("I21.9"),
                        List.of(),
                        UrgencyLevel.HIGH,
                        "Cardiology",
                        CaseType.INPATIENT,
                        "Patient requires immediate attention",
                        null
                ),
                new MedicalCase(
                        IdGenerator.generateId(),
                        60,
                        "Diabetes management",
                        "Elevated blood sugar",
                        "Type 2 DM",
                        List.of("E11.9"),
                        List.of(),
                        UrgencyLevel.MEDIUM,
                        "Endocrinology",
                        CaseType.SECOND_OPINION,
                        "Routine follow-up",
                        null
                ),
                new MedicalCase(
                        IdGenerator.generateId(),
                        35,
                        "Fever and headache",
                        "High fever, severe headache",
                        "Meningitis",
                        List.of("G00.9"),
                        List.of(),
                        UrgencyLevel.HIGH,
                        "Infectious Disease",
                        CaseType.INPATIENT,
                        "Urgent evaluation needed",
                        null
                )
        );

        List<List<Double>> embeddings = embeddingService.generateEmbeddingsForMedicalCases(medicalCases);

        assertNotNull(embeddings);
        assertEquals(medicalCases.size(), embeddings.size(), "Should return embedding for each medical case");

        embeddings.forEach(embedding -> {
            assertNotNull(embedding);
            assertFalse(embedding.isEmpty());
            assertTrue(embedding.size() > 100, "Each embedding should have reasonable dimension");
        });
    }

    @Test
    void testGenerateEmbeddingsForMedicalCases_BatchWithMixedAbstracts() {
        List<MedicalCase> medicalCases = List.of(
                new MedicalCase(
                        IdGenerator.generateId(),
                        45,
                        "Chest pain",
                        "Severe chest pain, shortness of breath",
                        "Acute MI",
                        List.of("I21.9"),
                        List.of(),
                        UrgencyLevel.HIGH,
                        "Cardiology",
                        CaseType.INPATIENT,
                        "Patient requires immediate attention",
                        "Stored abstract for chest pain case"
                ),
                new MedicalCase(
                        IdGenerator.generateId(),
                        60,
                        "Diabetes management",
                        "Elevated blood sugar",
                        "Type 2 DM",
                        List.of("E11.9"),
                        List.of(),
                        UrgencyLevel.MEDIUM,
                        "Endocrinology",
                        CaseType.SECOND_OPINION,
                        "Routine follow-up",
                        null
                ),
                new MedicalCase(
                        IdGenerator.generateId(),
                        35,
                        "Fever and headache",
                        "High fever, severe headache",
                        "Meningitis",
                        List.of("G00.9"),
                        List.of(),
                        UrgencyLevel.HIGH,
                        "Infectious Disease",
                        CaseType.INPATIENT,
                        "Urgent evaluation needed",
                        "Stored abstract for meningitis case"
                )
        );

        List<List<Double>> embeddings = embeddingService.generateEmbeddingsForMedicalCases(medicalCases);

        assertNotNull(embeddings);
        assertEquals(medicalCases.size(), embeddings.size(), "Should return embedding for each medical case");

        embeddings.forEach(embedding -> {
            assertNotNull(embedding);
            assertFalse(embedding.isEmpty());
            assertTrue(embedding.size() > 100, "Each embedding should have reasonable dimension");
        });
    }

    @Test
    void testGenerateEmbeddingsForMedicalCases_EmptyList() {
        List<MedicalCase> emptyList = List.of();

        List<List<Double>> embeddings = embeddingService.generateEmbeddingsForMedicalCases(emptyList);

        assertNotNull(embeddings);
        assertTrue(embeddings.isEmpty(), "Should return empty list for empty input");
    }

    @Test
    void testGenerateEmbeddingsForMedicalCases_SingleCase() {
        MedicalCase medicalCase = new MedicalCase(
                IdGenerator.generateId(),
                50,
                "Abdominal pain",
                "Severe abdominal pain, nausea",
                "Appendicitis",
                List.of("K35.9"),
                List.of(),
                UrgencyLevel.HIGH,
                "General Surgery",
                CaseType.INPATIENT,
                "Surgical evaluation",
                null
        );

        List<List<Double>> embeddings = embeddingService.generateEmbeddingsForMedicalCases(List.of(medicalCase));

        assertNotNull(embeddings);
        assertEquals(1, embeddings.size(), "Should return one embedding for one case");
        assertFalse(embeddings.get(0).isEmpty());
        assertTrue(embeddings.get(0).size() > 100, "Embedding should have reasonable dimension");
    }

    @Test
    void testGenerateEmbeddingsForMedicalCases_MultipleICDCodes() {
        MedicalCase medicalCase = new MedicalCase(
                IdGenerator.generateId(),
                70,
                "Multiple conditions",
                "Various symptoms",
                "Multiple diagnoses",
                List.of("I21.9", "E11.9", "G00.9"), // Multiple ICD-10 codes
                List.of(),
                UrgencyLevel.MEDIUM,
                "Internal Medicine",
                CaseType.INPATIENT,
                "Complex case",
                null
        );

        List<Double> embedding = embeddingService.generateEmbeddingForMedicalCase(medicalCase);

        assertNotNull(embedding);
        assertFalse(embedding.isEmpty());
        assertTrue(embedding.size() > 100, "Embedding should have reasonable dimension");
    }
}
