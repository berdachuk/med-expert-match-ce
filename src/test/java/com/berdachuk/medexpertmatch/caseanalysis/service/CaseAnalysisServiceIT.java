package com.berdachuk.medexpertmatch.caseanalysis.service;

import com.berdachuk.medexpertmatch.caseanalysis.domain.CaseAnalysisResult;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for CaseAnalysisService.
 */
class CaseAnalysisServiceIT extends BaseIntegrationTest {

    @Autowired
    private CaseAnalysisService caseAnalysisService;

    @BeforeEach
    void setUp() {
        // Test data is created per test
    }

    @Test
    void testAnalyzeCase() {
        // Given
        MedicalCase medicalCase = new MedicalCase(
                "test-case-id-001",
                45,
                "Chest pain and shortness of breath",
                "Severe chest pain radiating to left arm, shortness of breath, sweating",
                "Acute myocardial infarction",
                List.of("I21.9"),
                List.of(),
                UrgencyLevel.CRITICAL,
                "CARDIOLOGY",
                CaseType.INPATIENT,
                "Patient presented to ER with acute symptoms",
                null  // abstractText
        );

        // When
        CaseAnalysisResult result = caseAnalysisService.analyzeCase(medicalCase);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.clinicalFindings()).isNotEmpty();
        assertThat(result.potentialDiagnoses()).isNotEmpty();
        assertThat(result.recommendedNextSteps()).isNotEmpty();
    }

    @Test
    void testExtractICD10Codes() {
        // Given
        MedicalCase medicalCase = new MedicalCase(
                "test-case-id-002",
                50,
                "Type 2 diabetes mellitus",
                "Increased thirst, frequent urination, fatigue",
                "Type 2 diabetes mellitus",
                List.of(),
                List.of(),
                UrgencyLevel.MEDIUM,
                "ENDOCRINOLOGY",
                CaseType.SECOND_OPINION,
                "Patient has been experiencing symptoms for several months",
                null  // abstractText
        );

        // When
        List<String> icd10Codes = caseAnalysisService.extractICD10Codes(medicalCase);

        // Then
        assertThat(icd10Codes).isNotNull();
        // Note: Actual codes depend on LLM response, but should be valid ICD-10 format if present
        icd10Codes.forEach(code -> {
            assertThat(code).isNotEmpty();
            assertThat(code).matches("^[A-Z]\\d{2}(\\.\\d{1,4})?$");
        });
    }

    @Test
    void testClassifyUrgency() {
        // Given - Critical case
        MedicalCase criticalCase = new MedicalCase(
                "test-case-id-003",
                60,
                "Cardiac arrest",
                "Unresponsive, no pulse, not breathing",
                "Cardiac arrest",
                List.of("I46.9"),
                List.of(),
                null, // Will be classified
                "CARDIOLOGY",
                CaseType.INPATIENT,
                "Patient found unresponsive",
                null  // abstractText
        );

        // When
        UrgencyLevel urgency = caseAnalysisService.classifyUrgency(criticalCase);

        // Then
        assertThat(urgency).isNotNull();
        assertThat(urgency).isIn((Object[]) UrgencyLevel.values());
        // Critical cases should be classified as CRITICAL or HIGH
        assertThat(urgency).isIn(UrgencyLevel.CRITICAL, UrgencyLevel.HIGH);

        // Given - Low urgency case
        MedicalCase lowUrgencyCase = new MedicalCase(
                "test-case-id-004",
                30,
                "Routine check-up",
                "No symptoms, general wellness visit",
                "Healthy",
                List.of(),
                List.of(),
                null, // Will be classified
                "FAMILY_MEDICINE",
                CaseType.SECOND_OPINION,
                "Annual physical examination",
                null  // abstractText
        );

        // When
        UrgencyLevel lowUrgency = caseAnalysisService.classifyUrgency(lowUrgencyCase);

        // Then
        assertThat(lowUrgency).isNotNull();
        assertThat(lowUrgency).isIn((Object[]) UrgencyLevel.values());
        // Routine cases should be classified as LOW or MEDIUM
        assertThat(lowUrgency).isIn(UrgencyLevel.LOW, UrgencyLevel.MEDIUM);
    }

    @Test
    void testDetermineRequiredSpecialty() {
        // Given
        MedicalCase medicalCase = new MedicalCase(
                "test-case-id-005",
                55,
                "Acute stroke symptoms",
                "Sudden weakness on right side, slurred speech, facial droop",
                "Acute ischemic stroke",
                List.of("I63.9"),
                List.of(),
                UrgencyLevel.CRITICAL,
                null, // Will be determined
                CaseType.INPATIENT,
                "Patient presented to ER with stroke symptoms",
                null  // abstractText
        );

        // When
        List<String> specialties = caseAnalysisService.determineRequiredSpecialty(medicalCase);

        // Then
        assertThat(specialties).isNotNull();
        assertThat(specialties).isNotEmpty();
        // Should include NEUROLOGY for stroke cases
        assertThat(specialties).anyMatch(s -> s.contains("NEUROLOGY") || s.contains("NEURO"));
    }
}
