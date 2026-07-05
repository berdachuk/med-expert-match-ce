package com.berdachuk.medexpertmatch.retrieval;

import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.retrieval.config.RetrievalScoringProperties;
import com.berdachuk.medexpertmatch.retrieval.domain.PriorityScore;
import com.berdachuk.medexpertmatch.retrieval.domain.ScoreResult;
import com.berdachuk.medexpertmatch.retrieval.service.SemanticGraphRetrievalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * REQ-001: integration coverage for registered requirement.
 * REQ-008: integration coverage for registered requirement.
 * REQ-018: integration coverage for registered requirement.
 */
class RetrievalScoringIT extends BaseIntegrationTest {

    @Autowired
    private SemanticGraphRetrievalService retrievalService;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private RetrievalScoringProperties scoringProperties;

    @BeforeEach
    void setUp() {
        medicalCaseRepository.deleteAll();
        doctorRepository.deleteAll();
    }

    @Test
    void scoringWithNoDoctorHistoryShouldReturnLowScore() {
        String doctorId = "d-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        Doctor doctor = new Doctor(doctorId, "Dr. New", "new@test.com",
                List.of("Cardiology"), List.of("MD"), List.of(), true, "AVAILABLE");
        doctorRepository.insert(doctor);

        String caseId = "c-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        MedicalCase medicalCase = new MedicalCase(
                caseId, 45, "Chest pain", "Dyspnea", "Possible MI",
                List.of("I21.0"), List.of("22298006"), UrgencyLevel.HIGH, "Cardiology",
                CaseType.INPATIENT, "New symptoms", "Case description");

        medicalCaseRepository.insert(medicalCase);

        ScoreResult score = retrievalService.score(medicalCase, doctor);
        assertNotNull(score);
        assertTrue(score.overallScore() >= 0.0);
        assertNotNull(score.rationale());
    }

    @Test
    void computePriorityScoreShouldReturnValidScore() {
        String caseId = "c-prio-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        MedicalCase medicalCase = new MedicalCase(
                caseId, 62, "Abdominal pain", "Hypotension", "Blunt trauma",
                List.of("S36.0"), List.of("261665006"), UrgencyLevel.CRITICAL, "Surgery",
                CaseType.INPATIENT, "Severe trauma", "Critical case");

        medicalCaseRepository.insert(medicalCase);

        PriorityScore priority = retrievalService.computePriorityScore(medicalCase);
        assertNotNull(priority);
        assertTrue(priority.overallScore() >= 0.0);
        assertTrue(priority.overallScore() <= 100.0);
        assertNotNull(priority.rationale());
        assertTrue(priority.urgencyScore() >= 0.0);
        assertTrue(priority.complexityScore() >= 0.0);
        assertTrue(priority.availabilityScore() >= 0.0);
    }

    @Test
    void scoringPropertiesShouldSumToOne() {
        double doctorSum = scoringProperties.getDoctorVectorWeight()
                + scoringProperties.getDoctorGraphWeight()
                + scoringProperties.getDoctorHistoricalWeight();
        assertEquals(1.0, doctorSum, 0.001);

        double facilitySum = scoringProperties.getFacilityComplexityWeight()
                + scoringProperties.getFacilityHistoricalWeight()
                + scoringProperties.getFacilityCapacityWeight()
                + scoringProperties.getFacilityGeographicWeight();
        assertEquals(1.0, facilitySum, 0.001);

        double prioritySum = scoringProperties.getPriorityUrgencyWeight()
                + scoringProperties.getPriorityComplexityWeight()
                + scoringProperties.getPriorityAvailabilityWeight();
        assertEquals(1.0, prioritySum, 0.001);
    }

    @Test
    void scoringWithLowUrgencyReturnsLowerPriority() {
        String caseIdLow = "c-low-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        MedicalCase lowUrgencyCase = new MedicalCase(
                caseIdLow, 30, "Checkup", "None", "Wellness visit",
                List.of("Z00.0"), List.of(), UrgencyLevel.LOW, "General",
                CaseType.CONSULT_REQUEST, "Annual checkup", "Routine visit");

        medicalCaseRepository.insert(lowUrgencyCase);

        String caseIdHigh = "c-high-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        MedicalCase highUrgencyCase = new MedicalCase(
                caseIdHigh, 55, "Chest pain", "Diaphoresis", "Acute MI",
                List.of("I21.0"), List.of("57054005"), UrgencyLevel.CRITICAL, "Cardiology",
                CaseType.INPATIENT, "Acute MI symptoms", "Heart attack emergency");

        medicalCaseRepository.insert(highUrgencyCase);

        PriorityScore lowScore = retrievalService.computePriorityScore(lowUrgencyCase);
        PriorityScore highScore = retrievalService.computePriorityScore(highUrgencyCase);

        assertTrue(highScore.urgencyScore() > lowScore.urgencyScore(),
                "Critical urgency should have higher urgency score than LOW");
    }

    @Test
    void scoringWithInvalidDoctorShouldNotThrow() {
        Doctor nonexistentDoctor = new Doctor(
                "nonexistent-id", "Dr. Ghost", "ghost@test.com",
                List.of(), List.of(), List.of(), false, "UNAVAILABLE");

        String caseId = "c-ghost-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        MedicalCase medicalCase = new MedicalCase(
                caseId, 40, "Seizure", "Loss of consciousness", "Epilepsy",
                List.of("G40.0"), List.of("84757009"), UrgencyLevel.MEDIUM, "Neurology",
                CaseType.INPATIENT, "Seizure episode", "Neurology case");

        medicalCaseRepository.insert(medicalCase);

        ScoreResult score = retrievalService.score(medicalCase, nonexistentDoctor);
        assertNotNull(score);
    }

    @Test
    void computePriorityScoreForMediumUrgency() {
        String caseId = "c-med-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        MedicalCase medicalCase = new MedicalCase(
                caseId, 70, "Hip pain", "Difficulty walking", "Hip fracture",
                List.of("S72.0"), List.of("46866001"), UrgencyLevel.MEDIUM, "Orthopedics",
                CaseType.INPATIENT, "Hip fracture", "Ortho case");

        medicalCaseRepository.insert(medicalCase);

        PriorityScore priority = retrievalService.computePriorityScore(medicalCase);
        assertTrue(priority.overallScore() > 0.0,
                "Medium urgency should still have non-zero priority score");
    }

    @Test
    void computePriorityScoreWithNullUrgency() {
        String caseId = "c-null-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        MedicalCase medicalCase = new MedicalCase(
                caseId, 25, "Rash", "Itching", "Eczema",
                List.of("L20.0"), List.of("24079001"), null, "Dermatology",
                CaseType.CONSULT_REQUEST, "Skin condition", "Dermatology case");

        medicalCaseRepository.insert(medicalCase);

        PriorityScore priority = retrievalService.computePriorityScore(medicalCase);
        assertNotNull(priority);
        assertEquals(0.5, priority.urgencyScore(), 0.001);
    }

    @Test
    void fusionStrategyShouldNotBeNull() {
        assertNotNull(scoringProperties.getFusionStrategy());
        assertTrue(List.of("weighted", "rrf").contains(scoringProperties.getFusionStrategy()));
    }
}
