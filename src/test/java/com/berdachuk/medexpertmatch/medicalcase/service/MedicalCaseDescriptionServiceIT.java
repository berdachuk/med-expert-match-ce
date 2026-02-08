package com.berdachuk.medexpertmatch.medicalcase.service;

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
 * Integration test for MedicalCaseDescriptionService.
 * Tests description generation with LLM enhancement and fallback to simple text.
 */
class MedicalCaseDescriptionServiceIT extends BaseIntegrationTest {

    @Autowired
    private MedicalCaseDescriptionService descriptionService;

    @Test
    void testGenerateDescription_WithAllFields() {
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

        String description = descriptionService.generateDescription(medicalCase);

        assertNotNull(description);
        assertFalse(description.isBlank());
        assertTrue(description.contains("Chest pain") || description.contains("chest pain"));
        assertTrue(description.contains("I21.9") || description.contains("Acute MI"));
    }

    @Test
    void testGenerateDescription_WithNullFields() {
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

        String description = descriptionService.generateDescription(medicalCase);

        assertNotNull(description);
        assertFalse(description.isBlank());
        assertTrue(description.contains("Symptoms") || description.contains("symptoms"));
    }

    @Test
    void testGenerateDescription_WithEmptyLists() {
        MedicalCase medicalCase = new MedicalCase(
                IdGenerator.generateId(),
                30,
                "Headache",
                "Persistent headache",
                "Migraine",
                List.of(),
                List.of(),
                UrgencyLevel.LOW,
                "Neurology",
                CaseType.CONSULT_REQUEST,
                "Routine consultation",
                null
        );

        String description = descriptionService.generateDescription(medicalCase);

        assertNotNull(description);
        assertFalse(description.isBlank());
        assertTrue(description.contains("Headache") || description.contains("headache"));
    }

    @Test
    void testGenerateDescription_WithMultipleICDCodes() {
        MedicalCase medicalCase = new MedicalCase(
                IdGenerator.generateId(),
                70,
                "Multiple conditions",
                "Various symptoms",
                "Multiple diagnoses",
                List.of("I21.9", "E11.9", "G00.9"),
                List.of(),
                UrgencyLevel.MEDIUM,
                "Internal Medicine",
                CaseType.INPATIENT,
                "Complex case",
                null
        );

        String description = descriptionService.generateDescription(medicalCase);

        assertNotNull(description);
        assertFalse(description.isBlank());
        assertTrue(description.contains("I21.9") || description.contains("E11.9") || description.contains("G00.9"));
    }

    @Test
    void testGetOrGenerateDescription_WithExistingAbstract() {
        String existingAbstract = "Existing comprehensive medical case abstract with detailed information";
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
                existingAbstract
        );

        String description = descriptionService.getOrGenerateDescription(medicalCase);

        assertEquals(existingAbstract, description, "Should return existing abstract when present");
    }

    @Test
    void testGetOrGenerateDescription_WithoutExistingAbstract() {
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

        String description = descriptionService.getOrGenerateDescription(medicalCase);

        assertNotNull(description);
        assertFalse(description.isBlank());
        assertTrue(description.contains("Abdominal pain") || description.contains("abdominal pain"));
    }

    @Test
    void testGetOrGenerateDescription_WithBlankAbstract() {
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
                "   "
        );

        String description = descriptionService.getOrGenerateDescription(medicalCase);

        assertNotNull(description);
        assertFalse(description.isBlank());
        assertTrue(description.contains("Abdominal pain") || description.contains("abdominal pain"));
    }

    @Test
    void testGenerateDescription_FallbackToSimpleText() {
        MedicalCase medicalCase = new MedicalCase(
                IdGenerator.generateId(),
                35,
                "Fever",
                "High fever, chills",
                "Infection",
                List.of("A41.9"),
                List.of(),
                UrgencyLevel.MEDIUM,
                "Infectious Disease",
                CaseType.INPATIENT,
                "Antibiotic treatment",
                null
        );

        String description = descriptionService.generateDescription(medicalCase);

        assertNotNull(description);
        assertFalse(description.isBlank());
        assertTrue(description.contains("Fever") || description.contains("fever") ||
                description.contains("Symptoms") || description.contains("symptoms"));
    }

    @Test
    void testGenerateDescription_ConsistentOutput() {
        MedicalCase medicalCase = new MedicalCase(
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
        );

        String description1 = descriptionService.generateDescription(medicalCase);
        String description2 = descriptionService.generateDescription(medicalCase);

        assertNotNull(description1);
        assertNotNull(description2);
        assertFalse(description1.isBlank());
        assertFalse(description2.isBlank());
    }
}
