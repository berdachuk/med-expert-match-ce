package com.berdachuk.medexpertmatch.retrieval.service;

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
import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;
import com.berdachuk.medexpertmatch.retrieval.domain.FacilityMatch;
import com.berdachuk.medexpertmatch.retrieval.domain.MatchOptions;
import com.berdachuk.medexpertmatch.retrieval.domain.RoutingOptions;
import com.berdachuk.medexpertmatch.retrieval.repository.ConsultationMatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for MatchingService.
 */
class MatchingServiceIT extends BaseIntegrationTest {

    @Autowired
    private MatchingService matchingService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    @Autowired
    private ClinicalExperienceRepository clinicalExperienceRepository;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private ConsultationMatchRepository consultationMatchRepository;

    @BeforeEach
    void setUp() {
        // Clear test data
        consultationMatchRepository.deleteAll();
        clinicalExperienceRepository.deleteAll();
        medicalCaseRepository.deleteAll();
        doctorRepository.deleteAll();
        facilityRepository.deleteAll();
    }

    @Test
    void testMatchDoctorsToCase() {
        // Create test doctors
        String doctorId1 = IdGenerator.generateDoctorId();
        Doctor doctor1 = new Doctor(
                doctorId1,
                "Dr. Cardiology Specialist",
                "cardio1@example.com",
                List.of("Cardiology"),
                List.of("Board Certified"),
                List.of(),
                true,
                "AVAILABLE"
        );
        doctorRepository.insert(doctor1);

        String doctorId2 = IdGenerator.generateDoctorId();
        Doctor doctor2 = new Doctor(
                doctorId2,
                "Dr. General Practitioner",
                "gp1@example.com",
                List.of("General Medicine"),
                List.of(),
                List.of(),
                false,
                "AVAILABLE"
        );
        doctorRepository.insert(doctor2);

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

        // Match doctors
        MatchOptions options = MatchOptions.defaultOptions();
        List<DoctorMatch> matches = matchingService.matchDoctorsToCase(medicalCase.id(), options);

        assertNotNull(matches);
        assertFalse(matches.isEmpty());
        assertTrue(matches.stream().anyMatch(m -> m.doctor().id().equals(doctor1.id())));
        assertTrue(consultationMatchRepository.count() >= matches.size(),
                "Consultation matches should be persisted after matchDoctorsToCase");
    }

    @Test
    void testMatchDoctorsToCaseWithSpecialtyFilter() {
        // Create test doctors
        String cardiologistId = IdGenerator.generateDoctorId();
        Doctor cardiologist = new Doctor(
                cardiologistId,
                "Dr. Cardiology Specialist",
                "cardio1@example.com",
                List.of("Cardiology"),
                List.of(),
                List.of(),
                true,
                "AVAILABLE"
        );
        doctorRepository.insert(cardiologist);

        String neurologistId = IdGenerator.generateDoctorId();
        Doctor neurologist = new Doctor(
                neurologistId,
                "Dr. Neurology Specialist",
                "neuro1@example.com",
                List.of("Neurology"),
                List.of(),
                List.of(),
                true,
                "AVAILABLE"
        );
        doctorRepository.insert(neurologist);

        // Create test case
        MedicalCase medicalCase = new MedicalCase(
                IdGenerator.generateId(),
                50,
                "Heart failure",
                "Shortness of breath",
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

        // Match with specialty filter
        MatchOptions options = MatchOptions.builder()
                .maxResults(10)
                .preferredSpecialties(List.of("Cardiology"))
                .build();

        List<DoctorMatch> matches = matchingService.matchDoctorsToCase(medicalCase.id(), options);

        assertNotNull(matches);
        // Should only find cardiologist
        assertTrue(matches.stream().allMatch(m -> m.doctor().id().equals(cardiologist.id())));
        assertFalse(matches.stream().anyMatch(m -> m.doctor().id().equals(neurologist.id())));
    }

    @Test
    void testMatchDoctorsToCaseWithTelehealthFilter() {
        // Create test doctors
        String telehealthDoctorId = IdGenerator.generateDoctorId();
        Doctor telehealthDoctor = new Doctor(
                telehealthDoctorId,
                "Dr. Telehealth Specialist",
                "telehealth1@example.com",
                List.of("Cardiology"),
                List.of(),
                List.of(),
                true, // Telehealth enabled
                "AVAILABLE"
        );
        doctorRepository.insert(telehealthDoctor);

        String nonTelehealthDoctorId = IdGenerator.generateDoctorId();
        Doctor nonTelehealthDoctor = new Doctor(
                nonTelehealthDoctorId,
                "Dr. In-Person Only",
                "inperson1@example.com",
                List.of("Cardiology"),
                List.of(),
                List.of(),
                false, // Telehealth disabled
                "AVAILABLE"
        );
        doctorRepository.insert(nonTelehealthDoctor);

        // Create test case
        MedicalCase medicalCase = new MedicalCase(
                IdGenerator.generateId(),
                40,
                "Chest pain",
                "Mild chest discomfort",
                "Stable angina",
                List.of("I20.9"),
                List.of(),
                UrgencyLevel.MEDIUM,
                "Cardiology",
                CaseType.SECOND_OPINION,
                "Second opinion requested",
                null
        );
        medicalCaseRepository.insert(medicalCase);

        // Match with telehealth filter
        MatchOptions options = MatchOptions.builder()
                .maxResults(10)
                .requireTelehealth(true)
                .build();

        List<DoctorMatch> matches = matchingService.matchDoctorsToCase(medicalCase.id(), options);

        assertNotNull(matches);
        // Should only find telehealth-enabled doctor
        assertTrue(matches.stream().allMatch(m -> m.doctor().telehealthEnabled()));
        assertFalse(matches.stream().anyMatch(m -> m.doctor().id().equals(nonTelehealthDoctor.id())));
    }

    @Test
    void testMatchDoctorsToCaseWithMinScore() {
        // Create test doctor
        String doctorId = IdGenerator.generateDoctorId();
        Doctor doctor = new Doctor(
                doctorId,
                "Dr. Test Specialist",
                "test1@example.com",
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
                45,
                "Chest pain",
                "Severe chest pain",
                "Acute MI",
                List.of("I21.9"),
                List.of(),
                UrgencyLevel.CRITICAL,
                "Cardiology",
                CaseType.CONSULT_REQUEST,
                "Acute case",
                null
        );
        medicalCaseRepository.insert(medicalCase);

        // Match with high minimum score (should filter out low-scoring matches)
        MatchOptions options = MatchOptions.builder()
                .maxResults(10)
                .minScore(90.0) // Very high threshold
                .build();

        List<DoctorMatch> matches = matchingService.matchDoctorsToCase(medicalCase.id(), options);

        assertNotNull(matches);
        // All matches should meet the minimum score threshold
        assertTrue(matches.stream().allMatch(m -> m.matchScore() >= 90.0));
    }

    @Test
    void testMatchDoctorsToCaseWithMaxResults() {
        // Create multiple test doctors
        for (int i = 0; i < 5; i++) {
            String doctorId = IdGenerator.generateDoctorId();
            Doctor doctor = new Doctor(
                    doctorId,
                    "Dr. Test Specialist " + i,
                    "test" + i + "@example.com",
                    List.of("Cardiology"),
                    List.of(),
                    List.of(),
                    true,
                    "AVAILABLE"
            );
            doctorRepository.insert(doctor);
        }

        // Create test case
        MedicalCase medicalCase = new MedicalCase(
                IdGenerator.generateId(),
                45,
                "Chest pain",
                "Severe chest pain",
                "Acute MI",
                List.of("I21.9"),
                List.of(),
                UrgencyLevel.CRITICAL,
                "Cardiology",
                CaseType.CONSULT_REQUEST,
                "Acute case",
                null
        );
        medicalCaseRepository.insert(medicalCase);

        // Match with limited results
        MatchOptions options = MatchOptions.builder()
                .maxResults(3)
                .build();

        List<DoctorMatch> matches = matchingService.matchDoctorsToCase(medicalCase.id(), options);

        assertNotNull(matches);
        assertTrue(matches.size() <= 3);
    }

    @Test
    void testMatchDoctorsToCaseNotFound() {
        // Try to match with non-existent case
        MatchOptions options = MatchOptions.defaultOptions();

        assertThrows(IllegalArgumentException.class, () -> {
            matchingService.matchDoctorsToCase("nonexistent-case-id", options);
        });
    }

    @Test
    void testMatchFacilitiesForCase() {
        // Create test facility
        String facilityId = IdGenerator.generateFacilityId();
        Facility facility = new Facility(
                facilityId,
                "Test Medical Center",
                "HOSPITAL",
                "City",
                "State",
                "US",
                BigDecimal.valueOf(40.7128),
                BigDecimal.valueOf(-74.0060),
                List.of("ICU", "SURGERY"),
                100,
                50
        );
        facilityRepository.insert(facility);

        // Create test case
        MedicalCase medicalCase = new MedicalCase(
                IdGenerator.generateId(),
                45,
                "Chest pain",
                "Severe chest pain",
                "Acute MI",
                List.of("I21.9"),
                List.of(),
                UrgencyLevel.CRITICAL,
                "Cardiology",
                CaseType.CONSULT_REQUEST,
                "Acute case",
                null
        );
        medicalCaseRepository.insert(medicalCase);

        List<FacilityMatch> matches = matchingService.matchFacilitiesForCase(medicalCase.id(), RoutingOptions.defaultOptions());

        assertNotNull(matches);
        assertFalse(matches.isEmpty());
        assertTrue(matches.stream().anyMatch(m -> m.facility().id().equals(facilityId)));
    }

    @Test
    void testMatchFacilitiesForCaseWithPreferredType() {
        String academicId = IdGenerator.generateFacilityId();
        facilityRepository.insert(new Facility(
                academicId,
                "Academic Medical Center",
                "ACADEMIC",
                "City",
                "State",
                "US",
                null,
                null,
                List.of("ICU"),
                50,
                25
        ));
        String communityId = IdGenerator.generateFacilityId();
        facilityRepository.insert(new Facility(
                communityId,
                "Community Hospital",
                "COMMUNITY",
                "City",
                "State",
                "US",
                null,
                null,
                List.of("ICU"),
                30,
                15
        ));

        MedicalCase medicalCase = new MedicalCase(
                IdGenerator.generateId(),
                50,
                "Heart failure",
                "Shortness of breath",
                "CHF",
                List.of("I50.9"),
                List.of(),
                UrgencyLevel.HIGH,
                "Cardiology",
                CaseType.CONSULT_REQUEST,
                "Case for routing",
                null
        );
        medicalCaseRepository.insert(medicalCase);

        RoutingOptions options = RoutingOptions.builder()
                .maxResults(10)
                .preferredFacilityTypes(List.of("ACADEMIC"))
                .build();
        List<FacilityMatch> matches = matchingService.matchFacilitiesForCase(medicalCase.id(), options);

        assertNotNull(matches);
        assertTrue(matches.stream().allMatch(m -> "ACADEMIC".equalsIgnoreCase(m.facility().facilityType())));
        assertTrue(matches.stream().anyMatch(m -> m.facility().id().equals(academicId)));
        assertFalse(matches.stream().anyMatch(m -> m.facility().id().equals(communityId)));
    }

    @Test
    void testMatchFacilitiesForCaseWithRequiredCapabilities() {
        String facilityWithIcuId = IdGenerator.generateFacilityId();
        facilityRepository.insert(new Facility(
                facilityWithIcuId,
                "ICU Center",
                "HOSPITAL",
                "City",
                "State",
                "US",
                null,
                null,
                List.of("ICU", "SURGERY"),
                50,
                20
        ));
        String facilityWithoutIcuId = IdGenerator.generateFacilityId();
        facilityRepository.insert(new Facility(
                facilityWithoutIcuId,
                "Outpatient Only",
                "CLINIC",
                "City",
                "State",
                "US",
                null,
                null,
                List.of("LAB"),
                20,
                5
        ));

        MedicalCase medicalCase = new MedicalCase(
                IdGenerator.generateId(),
                60,
                "Critical care",
                "Requires ICU",
                "Sepsis",
                List.of("A41.9"),
                List.of(),
                UrgencyLevel.CRITICAL,
                "Critical Care",
                CaseType.CONSULT_REQUEST,
                "Needs ICU",
                null
        );
        medicalCaseRepository.insert(medicalCase);

        RoutingOptions options = RoutingOptions.builder()
                .maxResults(10)
                .requiredCapabilities(List.of("ICU"))
                .build();
        List<FacilityMatch> matches = matchingService.matchFacilitiesForCase(medicalCase.id(), options);

        assertNotNull(matches);
        assertTrue(matches.stream().allMatch(m -> m.facility().capabilities() != null && m.facility().capabilities().contains("ICU")));
        assertTrue(matches.stream().anyMatch(m -> m.facility().id().equals(facilityWithIcuId)));
        assertFalse(matches.stream().anyMatch(m -> m.facility().id().equals(facilityWithoutIcuId)));
    }

    @Test
    void testMatchFacilitiesForCaseMaxResults() {
        for (int i = 0; i < 5; i++) {
            facilityRepository.insert(new Facility(
                    IdGenerator.generateFacilityId(),
                    "Facility " + i,
                    "HOSPITAL",
                    "City",
                    "State",
                    "US",
                    null,
                    null,
                    List.of("ICU"),
                    50,
                    25
            ));
        }
        MedicalCase medicalCase = new MedicalCase(
                IdGenerator.generateId(),
                45,
                "Chest pain",
                "Severe",
                "MI",
                List.of("I21.9"),
                List.of(),
                UrgencyLevel.HIGH,
                "Cardiology",
                CaseType.CONSULT_REQUEST,
                "Case",
                null
        );
        medicalCaseRepository.insert(medicalCase);

        List<FacilityMatch> matches = matchingService.matchFacilitiesForCase(
                medicalCase.id(),
                RoutingOptions.builder().maxResults(3).build());

        assertNotNull(matches);
        assertTrue(matches.size() <= 3);
    }

    @Test
    void testMatchFacilitiesForCaseNotFound() {
        assertThrows(IllegalArgumentException.class, () -> {
            matchingService.matchFacilitiesForCase("nonexistent-case-id", RoutingOptions.defaultOptions());
        });
    }

    @Test
    void testMatchDoctorsToCase_RanksAreSequentialAfterSorting() {
        // Create multiple test doctors with different specialties to ensure different scores
        String doctorId1 = IdGenerator.generateDoctorId();
        Doctor doctor1 = new Doctor(
                doctorId1,
                "Dr. Cardiology Expert",
                "cardio1@example.com",
                List.of("Cardiology"),
                List.of("Board Certified", "Fellowship"),
                List.of(),
                true,
                "AVAILABLE"
        );
        doctorRepository.insert(doctor1);

        String doctorId2 = IdGenerator.generateDoctorId();
        Doctor doctor2 = new Doctor(
                doctorId2,
                "Dr. General Medicine",
                "general1@example.com",
                List.of("General Medicine"),
                List.of(),
                List.of(),
                false,
                "AVAILABLE"
        );
        doctorRepository.insert(doctor2);

        String doctorId3 = IdGenerator.generateDoctorId();
        Doctor doctor3 = new Doctor(
                doctorId3,
                "Dr. Cardiology Junior",
                "cardio2@example.com",
                List.of("Cardiology"),
                List.of(),
                List.of(),
                true,
                "AVAILABLE"
        );
        doctorRepository.insert(doctor3);

        // Create test case requiring cardiology
        MedicalCase medicalCase = new MedicalCase(
                IdGenerator.generateId(),
                55,
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

        // Match doctors
        MatchOptions options = MatchOptions.builder().maxResults(10).build();
        List<DoctorMatch> matches = matchingService.matchDoctorsToCase(medicalCase.id(), options);

        assertNotNull(matches);
        assertFalse(matches.isEmpty());

        // Verify ranks are sequential starting from 1
        for (int i = 0; i < matches.size(); i++) {
            assertEquals(i + 1, matches.get(i).rank(),
                    "Rank should be sequential: expected " + (i + 1) + " but got " + matches.get(i).rank());
        }

        // Verify matches are sorted by score descending
        for (int i = 0; i < matches.size() - 1; i++) {
            assertTrue(matches.get(i).matchScore() >= matches.get(i + 1).matchScore(),
                    "Matches should be sorted by score descending");
        }
    }

    @Test
    void testMatchFacilitiesForCase_RanksAreSequentialAfterSorting() {
        // Create multiple facilities with different characteristics
        String facilityId1 = IdGenerator.generateFacilityId();
        Facility facility1 = new Facility(
                facilityId1,
                "Academic Medical Center",
                "ACADEMIC",
                "New York",
                "NY",
                "US",
                BigDecimal.valueOf(40.7128),
                BigDecimal.valueOf(-74.0060),
                List.of("ICU", "CARDIOLOGY", "SURGERY"),
                500,
                200
        );
        facilityRepository.insert(facility1);

        String facilityId2 = IdGenerator.generateFacilityId();
        Facility facility2 = new Facility(
                facilityId2,
                "Community Hospital",
                "COMMUNITY",
                "Boston",
                "MA",
                "US",
                BigDecimal.valueOf(42.3601),
                BigDecimal.valueOf(-71.0589),
                List.of("ICU"),
                100,
                50
        );
        facilityRepository.insert(facility2);

        String facilityId3 = IdGenerator.generateFacilityId();
        Facility facility3 = new Facility(
                facilityId3,
                "Specialty Clinic",
                "SPECIALTY_CENTER",
                "Philadelphia",
                "PA",
                "US",
                BigDecimal.valueOf(39.9526),
                BigDecimal.valueOf(-75.1652),
                List.of("CARDIOLOGY"),
                50,
                25
        );
        facilityRepository.insert(facility3);

        // Create test case
        MedicalCase medicalCase = new MedicalCase(
                IdGenerator.generateId(),
                60,
                "Heart failure",
                "Severe shortness of breath",
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

        // Match facilities
        RoutingOptions options = RoutingOptions.builder().maxResults(10).build();
        List<FacilityMatch> matches = matchingService.matchFacilitiesForCase(medicalCase.id(), options);

        assertNotNull(matches);
        assertFalse(matches.isEmpty());

        // Verify ranks are sequential starting from 1
        for (int i = 0; i < matches.size(); i++) {
            assertEquals(i + 1, matches.get(i).rank(),
                    "Rank should be sequential: expected " + (i + 1) + " but got " + matches.get(i).rank());
        }

        // Verify matches are sorted by route score descending
        for (int i = 0; i < matches.size() - 1; i++) {
            assertTrue(matches.get(i).routeScore() >= matches.get(i + 1).routeScore(),
                    "Matches should be sorted by route score descending");
        }
    }

    @Test
    void testMatchFacilitiesForCase_HigherScoreGetsLowerRank() {
        // Create two facilities where one should clearly score higher
        String highScoreFacilityId = IdGenerator.generateFacilityId();
        Facility highScoreFacility = new Facility(
                highScoreFacilityId,
                "Premier Cardiology Center",
                "ACADEMIC",
                "City",
                "State",
                "US",
                null,
                null,
                List.of("ICU", "CARDIOLOGY", "SURGERY", "CATH_LAB"),
                500,
                100
        );
        facilityRepository.insert(highScoreFacility);

        String lowerScoreFacilityId = IdGenerator.generateFacilityId();
        Facility lowerScoreFacility = new Facility(
                lowerScoreFacilityId,
                "Basic Clinic",
                "CLINIC",
                "City",
                "State",
                "US",
                null,
                null,
                List.of("LAB"),
                20,
                15
        );
        facilityRepository.insert(lowerScoreFacility);

        // Create test case
        MedicalCase medicalCase = new MedicalCase(
                IdGenerator.generateId(),
                65,
                "Cardiac emergency",
                "Chest pain with ST elevation",
                "Acute MI",
                List.of("I21.0"),
                List.of(),
                UrgencyLevel.CRITICAL,
                "Cardiology",
                CaseType.CONSULT_REQUEST,
                "STEMI patient",
                null
        );
        medicalCaseRepository.insert(medicalCase);

        // Match facilities
        List<FacilityMatch> matches = matchingService.matchFacilitiesForCase(medicalCase.id(), RoutingOptions.defaultOptions());

        assertNotNull(matches);
        assertFalse(matches.isEmpty());

        // Find the premier facility in results
        FacilityMatch premierMatch = matches.stream()
                .filter(m -> m.facility().id().equals(highScoreFacilityId))
                .findFirst()
                .orElse(null);

        if (premierMatch != null) {
            // Premier facility should have rank 1 (best match)
            assertEquals(1, premierMatch.rank(), "Higher scoring facility should have rank 1");
        }
    }
}
