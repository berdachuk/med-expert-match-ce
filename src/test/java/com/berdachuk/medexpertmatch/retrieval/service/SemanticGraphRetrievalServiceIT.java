package com.berdachuk.medexpertmatch.retrieval.service;

import com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience;
import com.berdachuk.medexpertmatch.clinicalexperience.repository.ClinicalExperienceRepository;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.facility.domain.Facility;
import com.berdachuk.medexpertmatch.facility.repository.FacilityRepository;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.retrieval.domain.PriorityScore;
import com.berdachuk.medexpertmatch.retrieval.domain.RouteScoreResult;
import com.berdachuk.medexpertmatch.retrieval.domain.ScoreResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for SemanticGraphRetrievalService.
 */
class SemanticGraphRetrievalServiceIT extends BaseIntegrationTest {

    @Autowired
    private SemanticGraphRetrievalService semanticGraphRetrievalService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    @Autowired
    private ClinicalExperienceRepository clinicalExperienceRepository;

    @Autowired
    private FacilityRepository facilityRepository;

    @BeforeEach
    void setUp() {
        // Clear test data
        clinicalExperienceRepository.deleteAll();
        medicalCaseRepository.deleteAll();
        doctorRepository.deleteAll();
        facilityRepository.deleteAll();
    }

    @Test
    void testScore() {
        // Create test doctor
        Doctor doctor = new Doctor(
                IdGenerator.generateId(),
                "Dr. Test Specialist",
                "test@example.com",
                List.of("Cardiology"),
                List.of("Board Certified"),
                List.of(),
                true,
                "AVAILABLE"
        );
        doctorRepository.insert(doctor);

        // Create test case
        MedicalCase medicalCase = new MedicalCase(
                IdGenerator.generateId(),
                45,
                "Chest pain",
                "Severe chest pain radiating to left arm",
                "Acute myocardial infarction",
                List.of("I21.9"),
                List.of(),
                UrgencyLevel.CRITICAL,
                "Cardiology",
                CaseType.CONSULT_REQUEST,
                "Patient presents with acute MI",
                null
        );
        medicalCaseRepository.insert(medicalCase);

        // Score the match
        ScoreResult result = semanticGraphRetrievalService.score(medicalCase, doctor);

        assertNotNull(result);
        assertTrue(result.overallScore() >= 0 && result.overallScore() <= 100);
        assertTrue(result.vectorSimilarityScore() >= 0 && result.vectorSimilarityScore() <= 1);
        assertTrue(result.graphRelationshipScore() >= 0 && result.graphRelationshipScore() <= 1);
        assertTrue(result.historicalPerformanceScore() >= 0 && result.historicalPerformanceScore() <= 1);
        assertNotNull(result.rationale());
    }

    @Test
    void testScoreWithHistoricalData() {
        // Create test doctor
        String doctorId = IdGenerator.generateDoctorId();
        Doctor doctor = new Doctor(
                doctorId,
                "Dr. Experienced Specialist",
                "experienced@example.com",
                List.of("Cardiology"),
                List.of(),
                List.of(),
                true,
                "AVAILABLE"
        );
        doctorRepository.insert(doctor);

        // Create test case
        MedicalCase medicalCase = new MedicalCase(
                IdGenerator.generateId(),
                50,
                "Heart failure",
                "Shortness of breath, edema",
                "Congestive heart failure",
                List.of("I50.9"),
                List.of(),
                UrgencyLevel.HIGH,
                "Cardiology",
                CaseType.CONSULT_REQUEST,
                "Patient with CHF",
                null
        );
        medicalCaseRepository.insert(medicalCase);

        // Create historical experience with good outcomes
        ClinicalExperience experience = new ClinicalExperience(
                IdGenerator.generateId(),
                doctor.id(),
                medicalCase.id(),
                List.of("Echocardiogram", "Medication adjustment"),
                "MEDIUM",
                "SUCCESS",
                List.of(),
                7,
                5
        );
        clinicalExperienceRepository.insert(experience);

        // Score the match
        ScoreResult result = semanticGraphRetrievalService.score(medicalCase, doctor);

        assertNotNull(result);
        assertTrue(result.overallScore() >= 0 && result.overallScore() <= 100);
        // Historical performance should be higher due to good outcomes
        assertTrue(result.historicalPerformanceScore() > 0.5);
    }

    @Test
    void testSemanticGraphRetrievalRouteScore() {
        // Create test facility
        Facility facility = new Facility(
                "8009377469709733890",
                "Test Medical Center",
                "ACADEMIC",
                "Boston",
                "MA",
                "USA",
                BigDecimal.valueOf(42.3601),
                BigDecimal.valueOf(-71.0589),
                List.of("ICU", "SURGERY", "CARDIOLOGY"),
                100,
                50
        );

        // Create test case
        MedicalCase medicalCase = new MedicalCase(
                IdGenerator.generateId(),
                40,
                "Complex surgery needed",
                "Multiple comorbidities",
                "Complex case",
                List.of("I21.9"),
                List.of(),
                UrgencyLevel.HIGH,
                "Cardiology",
                CaseType.INPATIENT,
                "Requires specialized care",
                null
        );
        medicalCaseRepository.insert(medicalCase);

        // Score the route
        RouteScoreResult result = semanticGraphRetrievalService.semanticGraphRetrievalRouteScore(medicalCase, facility);

        assertNotNull(result);
        assertTrue(result.overallScore() >= 0 && result.overallScore() <= 100);
        assertTrue(result.complexityMatchScore() >= 0 && result.complexityMatchScore() <= 1);
        assertTrue(result.historicalOutcomesScore() >= 0 && result.historicalOutcomesScore() <= 1);
        assertTrue(result.capacityScore() >= 0 && result.capacityScore() <= 1);
        assertTrue(result.geographicScore() >= 0 && result.geographicScore() <= 1);
        assertNotNull(result.rationale());
    }

    @Test
    void testFacilityHistoricalOutcomesScore() {
        String facilityId = IdGenerator.generateFacilityId();
        Facility facility = new Facility(
                facilityId,
                "Test Medical Center",
                "HOSPITAL",
                "Boston",
                "MA",
                "USA",
                BigDecimal.valueOf(42.3601),
                BigDecimal.valueOf(-71.0589),
                List.of("ICU", "SURGERY"),
                100,
                50
        );
        facilityRepository.insert(facility);

        String doctorId = IdGenerator.generateDoctorId();
        Doctor doctor = new Doctor(
                doctorId,
                "Dr. Facility Specialist",
                "facility@example.com",
                List.of("Cardiology"),
                List.of(),
                List.of(facilityId),
                true,
                "AVAILABLE"
        );
        doctorRepository.insert(doctor);

        String pastCaseId = IdGenerator.generateId();
        MedicalCase pastCase = new MedicalCase(
                pastCaseId,
                50,
                "Past case",
                "Treated at facility",
                "Resolved",
                List.of("I21.9"),
                List.of(),
                UrgencyLevel.HIGH,
                "Cardiology",
                CaseType.INPATIENT,
                "Historical case",
                null
        );
        medicalCaseRepository.insert(pastCase);

        ClinicalExperience experience = new ClinicalExperience(
                IdGenerator.generateId(),
                doctorId,
                pastCaseId,
                List.of("Procedure"),
                "MEDIUM",
                "SUCCESS",
                List.of(),
                5,
                5
        );
        clinicalExperienceRepository.insert(experience);

        MedicalCase medicalCase = new MedicalCase(
                IdGenerator.generateId(),
                40,
                "New case for routing",
                "Needs facility",
                "Complex",
                List.of("I21.9"),
                List.of(),
                UrgencyLevel.HIGH,
                "Cardiology",
                CaseType.INPATIENT,
                "Route to facility",
                null
        );
        medicalCaseRepository.insert(medicalCase);

        RouteScoreResult result = semanticGraphRetrievalService.semanticGraphRetrievalRouteScore(medicalCase, facility);

        assertNotNull(result);
        assertTrue(result.historicalOutcomesScore() >= 0 && result.historicalOutcomesScore() <= 1);
        assertTrue(result.historicalOutcomesScore() > 0.5,
                "Facility with doctors who have good outcomes should score above neutral 0.5");
    }

    @Test
    void testComputePriorityScore() {
        // Create test case with CRITICAL urgency
        MedicalCase medicalCase = new MedicalCase(
                IdGenerator.generateId(),
                35,
                "Acute emergency",
                "Life-threatening condition",
                "Critical condition",
                List.of("I21.9"),
                List.of(),
                UrgencyLevel.CRITICAL,
                "Emergency Medicine",
                CaseType.INPATIENT,
                "Immediate attention required",
                null
        );
        medicalCaseRepository.insert(medicalCase);

        // Compute priority score
        PriorityScore result = semanticGraphRetrievalService.computePriorityScore(medicalCase);

        assertNotNull(result);
        assertTrue(result.overallScore() >= 0 && result.overallScore() <= 100);
        assertTrue(result.urgencyScore() >= 0 && result.urgencyScore() <= 1);
        assertTrue(result.complexityScore() >= 0 && result.complexityScore() <= 1);
        assertTrue(result.availabilityScore() >= 0 && result.availabilityScore() <= 1);
        assertNotNull(result.rationale());

        // CRITICAL urgency should result in high urgency score
        assertEquals(1.0, result.urgencyScore());
    }

    @Test
    void testComputePriorityScoreWithDifferentUrgencyLevels() {
        // Test LOW urgency
        MedicalCase lowUrgencyCase = new MedicalCase(
                IdGenerator.generateId(),
                30,
                "Routine checkup",
                "No acute symptoms",
                "Routine",
                List.of("Z00.00"),
                List.of(),
                UrgencyLevel.LOW,
                "General Medicine",
                CaseType.SECOND_OPINION,
                "Routine follow-up",
                null
        );
        medicalCaseRepository.insert(lowUrgencyCase);

        PriorityScore lowResult = semanticGraphRetrievalService.computePriorityScore(lowUrgencyCase);
        assertEquals(0.25, lowResult.urgencyScore());

        // Test HIGH urgency
        MedicalCase highUrgencyCase = new MedicalCase(
                IdGenerator.generateId(),
                45,
                "Urgent condition",
                "Requires attention",
                "Urgent",
                List.of("I21.9"),
                List.of(),
                UrgencyLevel.HIGH,
                "Cardiology",
                CaseType.CONSULT_REQUEST,
                "Urgent consultation needed",
                null
        );
        medicalCaseRepository.insert(highUrgencyCase);

        PriorityScore highResult = semanticGraphRetrievalService.computePriorityScore(highUrgencyCase);
        assertEquals(0.75, highResult.urgencyScore());
    }
}
