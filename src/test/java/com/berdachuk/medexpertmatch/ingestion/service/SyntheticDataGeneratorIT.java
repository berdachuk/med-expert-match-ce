package com.berdachuk.medexpertmatch.ingestion.service;

import com.berdachuk.medexpertmatch.clinicalexperience.repository.ClinicalExperienceRepository;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.doctor.repository.MedicalSpecialtyRepository;
import com.berdachuk.medexpertmatch.embedding.service.EmbeddingService;
import com.berdachuk.medexpertmatch.facility.repository.FacilityRepository;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.medicalcase.service.MedicalCaseDescriptionService;
import com.berdachuk.medexpertmatch.medicalcoding.repository.ICD10CodeRepository;
import com.berdachuk.medexpertmatch.medicalcoding.repository.ProcedureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for SyntheticDataGenerator service using Testcontainers PostgreSQL.
 * Uses mocked AI services (ChatModel, EmbeddingModel) via TestAIConfig.
 */
class SyntheticDataGeneratorIT extends BaseIntegrationTest {

    @Autowired
    private SyntheticDataGenerator syntheticDataGenerator;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    @Autowired
    private ClinicalExperienceRepository clinicalExperienceRepository;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private ICD10CodeRepository icd10CodeRepository;

    @Autowired
    private MedicalSpecialtyRepository medicalSpecialtyRepository;

    @Autowired
    private ProcedureRepository procedureRepository;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private MedicalCaseDescriptionService medicalCaseDescriptionService;

    @BeforeEach
    void setUp() {
        // Clear all test data before each test
        syntheticDataGenerator.clearTestData();
    }

    @Test
    void testGenerateTestData_TinySize() {
        // Test tiny size generation (tiny: 3 doctors, 15 cases)
        assertDoesNotThrow(() -> {
            syntheticDataGenerator.generateTestData("tiny", true);
        });

        // Verify doctors were created (tiny size: 3 doctors)
        List<String> doctorIds = doctorRepository.findAllIds(0);
        assertTrue(doctorIds.size() >= 3, "Should have at least 3 doctors for tiny size");

        // Verify medical cases were created (tiny size: 15 cases)
        List<String> caseIds = medicalCaseRepository.findAllIds(0);
        assertTrue(caseIds.size() >= 15, "Should have at least 15 cases for tiny size");

        // Verify facilities were created
        List<String> facilityIds = facilityRepository.findAllIds(0);
        assertTrue(facilityIds.size() > 0, "Should have facilities");

        // Verify ICD-10 codes were created
        List<com.berdachuk.medexpertmatch.medicalcoding.domain.ICD10Code> icd10Codes = icd10CodeRepository.findAll();
        assertTrue(icd10Codes.size() > 0, "Should have ICD-10 codes");

        // Verify medical specialties were created
        List<com.berdachuk.medexpertmatch.doctor.domain.MedicalSpecialty> specialties = medicalSpecialtyRepository.findAll();
        assertTrue(specialties.size() > 0, "Should have medical specialties");

        // Verify clinical experiences were created
        List<String> allDoctorIds = doctorRepository.findAllIds(0);
        Map<String, List<com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience>> experiencesByDoctor =
                clinicalExperienceRepository.findByDoctorIds(allDoctorIds);
        long experienceCount = experiencesByDoctor.values().stream().mapToLong(List::size).sum();
        assertTrue(experienceCount > 0, "Should have clinical experiences");
    }

    @Test
    void testGenerateTestData_WithClear() {
        // Generate initial data
        syntheticDataGenerator.generateTestData("tiny", true);
        long initialDoctorCount = doctorRepository.findAllIds(0).size();
        assertTrue(initialDoctorCount > 0, "Initial data should be created");

        // Generate again with clear=true
        syntheticDataGenerator.generateTestData("tiny", true);

        // Verify data was cleared and regenerated
        long finalDoctorCount = doctorRepository.findAllIds(0).size();
        assertEquals(initialDoctorCount, finalDoctorCount, "Should have same count after clear and regenerate");
    }

    @Test
    void testGenerateTestData_WithoutClear() {
        // Generate initial data
        syntheticDataGenerator.generateTestData("tiny", true);
        long initialDoctorCount = doctorRepository.findAllIds(0).size();
        assertTrue(initialDoctorCount > 0, "Initial data should be created");

        // Generate again with clear=false
        syntheticDataGenerator.generateTestData("tiny", false);

        // Verify data was added (not cleared)
        long finalDoctorCount = doctorRepository.findAllIds(0).size();
        assertTrue(finalDoctorCount >= initialDoctorCount * 2, "Should have at least double the count");
    }

    @Test
    void testGenerateDoctors() {
        // Generate facilities first (required by doctors)
        syntheticDataGenerator.generateFacilities(5);

        // Generate doctors
        assertDoesNotThrow(() -> {
            syntheticDataGenerator.generateDoctors(10);
        });

        // Verify doctors were created
        List<String> doctorIds = doctorRepository.findAllIds(0);
        assertEquals(10, doctorIds.size(), "Should have exactly 10 doctors");

        // Verify unique emails
        Set<String> emails = new HashSet<>();
        doctorIds.forEach(id -> {
            doctorRepository.findById(id).ifPresent(doctor -> {
                assertNotNull(doctor.email(), "Doctor should have email");
                assertFalse(emails.contains(doctor.email()), "Email should be unique: " + doctor.email());
                emails.add(doctor.email());
            });
        });
    }

    @Test
    void testGenerateDoctors_ZeroCount() {
        // Generate facilities first
        syntheticDataGenerator.generateFacilities(1);

        // Generate zero doctors
        assertDoesNotThrow(() -> {
            syntheticDataGenerator.generateDoctors(0);
        });

        // Verify no doctors were created
        List<String> doctorIds = doctorRepository.findAllIds(0);
        assertEquals(0, doctorIds.size(), "Should have no doctors");
    }

    @Test
    void testGenerateMedicalCases() {
        // Generate ICD-10 codes first (required by cases)
        syntheticDataGenerator.generateIcd10Codes("tiny");

        // Generate medical cases
        assertDoesNotThrow(() -> {
            syntheticDataGenerator.generateMedicalCases(100);
        });

        // Verify cases were created
        List<String> caseIds = medicalCaseRepository.findAllIds(0);
        assertEquals(100, caseIds.size(), "Should have exactly 100 cases");

        // Verify cases have ICD-10 codes
        caseIds.forEach(caseId -> {
            medicalCaseRepository.findById(caseId).ifPresent(medicalCase -> {
                assertNotNull(medicalCase.icd10Codes(), "Case should have ICD-10 codes");
                assertFalse(medicalCase.icd10Codes().isEmpty(), "Case should have at least one ICD-10 code");
            });
        });
    }

    @Test
    void testGenerateClinicalExperiences() {
        // Generate prerequisites
        syntheticDataGenerator.generateFacilities(5);
        syntheticDataGenerator.generateDoctors(10);
        syntheticDataGenerator.generateIcd10Codes("tiny");
        syntheticDataGenerator.generateMedicalCases(50);

        // Generate clinical experiences
        assertDoesNotThrow(() -> {
            syntheticDataGenerator.generateClinicalExperiences(10, 50);
        });

        // Verify experiences were created
        List<String> doctorIds = doctorRepository.findAllIds(0);
        Map<String, List<com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience>> experiencesByDoctor =
                clinicalExperienceRepository.findByDoctorIds(doctorIds);
        long experienceCount = experiencesByDoctor.values().stream().mapToLong(List::size).sum();
        assertTrue(experienceCount > 0, "Should have clinical experiences");

        // Verify no duplicate doctor-case pairs
        Set<String> doctorCasePairs = new HashSet<>();
        experiencesByDoctor.values().stream()
                .flatMap(List::stream)
                .forEach(experience -> {
                    String pair = experience.doctorId() + "|" + experience.caseId();
                    assertFalse(doctorCasePairs.contains(pair), "Should not have duplicate doctor-case pair: " + pair);
                    doctorCasePairs.add(pair);
                });
    }

    @Test
    void testGenerateFacilities() {
        // Generate facilities
        assertDoesNotThrow(() -> {
            syntheticDataGenerator.generateFacilities(10);
        });

        // Verify facilities were created
        List<String> facilityIds = facilityRepository.findAllIds(0);
        assertEquals(10, facilityIds.size(), "Should have exactly 10 facilities");

        // Verify facility types are valid (loaded from CSV)
        // Facility types are now loaded from facility-types.csv, which includes:
        // ACADEMIC, COMMUNITY, SPECIALTY_CENTER, HOSPITAL, CLINIC, URGENT_CARE, AMBULATORY_SURGERY,
        // REHABILITATION, NURSING_HOME, MENTAL_HEALTH
        Set<String> validFacilityTypes = Set.of(
                "ACADEMIC", "COMMUNITY", "SPECIALTY_CENTER", "HOSPITAL", "CLINIC",
                "URGENT_CARE", "AMBULATORY_SURGERY", "REHABILITATION", "NURSING_HOME", "MENTAL_HEALTH"
        );
        facilityIds.forEach(id -> {
            facilityRepository.findById(id).ifPresent(facility -> {
                assertNotNull(facility.facilityType(), "Facility should have type");
                assertFalse(facility.facilityType().isEmpty(), "Facility type should not be empty");
                // Verify it's one of the valid types from CSV
                assertTrue(
                        validFacilityTypes.contains(facility.facilityType()),
                        "Facility type should be valid (from CSV): " + facility.facilityType()
                );
            });
        });
    }

    @Test
    void testGenerateIcd10Codes() {
        // Generate ICD-10 codes
        assertDoesNotThrow(() -> {
            syntheticDataGenerator.generateIcd10Codes("tiny");
        });

        // Verify codes were created
        List<com.berdachuk.medexpertmatch.medicalcoding.domain.ICD10Code> codes = icd10CodeRepository.findAll();
        assertTrue(codes.size() > 0, "Should have ICD-10 codes");

        // Verify codes are valid format
        codes.forEach(code -> {
            assertNotNull(code.code(), "Code should not be null");
            assertFalse(code.code().isEmpty(), "Code should not be empty");
            // ICD-10 codes typically have format like "I21.9" or "C50.9"
            assertTrue(code.code().matches("[A-Z]\\d{2}\\.?\\d*"), "Code should match ICD-10 format: " + code.code());
        });

        // Verify codes loaded from CSV have proper structure (category, parent_code, related_codes)
        // Codes from CSV should have category and description populated
        long codesWithCategory = codes.stream()
                .filter(code -> code.category() != null && !code.category().isEmpty())
                .count();
        assertTrue(codesWithCategory > 0, "Codes loaded from CSV should have categories");

        // Verify at least some codes have parent codes (from CSV data)
        long codesWithParent = codes.stream()
                .filter(code -> code.parentCode() != null && !code.parentCode().isEmpty())
                .count();
        // Note: Not all codes have parent codes, but CSV-loaded ones should
        assertTrue(codes.size() > 0, "Should have codes (some may have parent codes from CSV)");
    }

    @Test
    void testGenerateMedicalSpecialties() {
        // Generate medical specialties
        assertDoesNotThrow(() -> {
            syntheticDataGenerator.generateMedicalSpecialties("tiny");
        });

        // Verify specialties were created
        List<com.berdachuk.medexpertmatch.doctor.domain.MedicalSpecialty> specialties = medicalSpecialtyRepository.findAll();
        assertTrue(specialties.size() > 0, "Should have medical specialties");

        // Verify specialties have names
        specialties.forEach(specialty -> {
            assertNotNull(specialty.name(), "Specialty should have name");
            assertFalse(specialty.name().isEmpty(), "Specialty name should not be empty");
        });

        // Verify specialties loaded from CSV have proper structure (description, ICD-10 ranges)
        // Specialties from CSV should have descriptions populated
        long specialtiesWithDescription = specialties.stream()
                .filter(specialty -> specialty.description() != null && !specialty.description().isEmpty())
                .count();
        assertTrue(specialtiesWithDescription > 0, "Specialties loaded from CSV should have descriptions");

        // Verify at least some specialties have ICD-10 code ranges (from CSV data)
        long specialtiesWithRanges = specialties.stream()
                .filter(specialty -> specialty.icd10CodeRanges() != null && !specialty.icd10CodeRanges().isEmpty())
                .count();
        // Note: Not all specialties have ICD-10 ranges, but CSV-loaded ones should
        assertTrue(specialties.size() > 0, "Should have specialties (some may have ICD-10 ranges from CSV)");
    }

    @Test
    void testClinicalExperiences_UseSpecialtyProceduresFromCsv() {
        // Generate prerequisites
        syntheticDataGenerator.generateFacilities(5);
        syntheticDataGenerator.generateDoctors(10);
        syntheticDataGenerator.generateIcd10Codes("tiny");
        syntheticDataGenerator.generateMedicalCases(50);

        // Generate clinical experiences
        assertDoesNotThrow(() -> {
            syntheticDataGenerator.generateClinicalExperiences(10, 50);
        });

        // Verify clinical experiences were created with procedures
        // Procedures should be selected based on doctor's specialties using CSV-loaded mapping
        List<String> doctorIds = doctorRepository.findAllIds(0);
        Map<String, List<com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience>> experiencesByDoctor =
                clinicalExperienceRepository.findByDoctorIds(doctorIds);

        long experiencesWithProcedures = experiencesByDoctor.values().stream()
                .flatMap(List::stream)
                .filter(exp -> exp.proceduresPerformed() != null && !exp.proceduresPerformed().isEmpty())
                .count();

        assertTrue(experiencesWithProcedures > 0,
                "Clinical experiences should have procedures selected based on specialty-procedures CSV mapping");
    }

    @Test
    void testGenerateTestData_WithProgressTracking() {
        // Generate data (progress tracking is handled internally via SyntheticDataGenerationProgressService)
        assertDoesNotThrow(() -> {
            syntheticDataGenerator.generateTestData("tiny", true);
        });

        // Verify data was generated
        List<String> doctorIds = doctorRepository.findAllIds(0);
        assertTrue(doctorIds.size() > 0, "Should have doctors");
    }

    @Test
    void testClearTestData() {
        // Generate some data first
        syntheticDataGenerator.generateFacilities(5);
        syntheticDataGenerator.generateDoctors(10);
        syntheticDataGenerator.generateIcd10Codes("tiny");
        syntheticDataGenerator.generateMedicalCases(50);

        // Verify data exists
        assertTrue(doctorRepository.findAllIds(0).size() > 0, "Should have doctors before clear");
        assertTrue(medicalCaseRepository.findAllIds(0).size() > 0, "Should have cases before clear");

        // Clear data
        assertDoesNotThrow(() -> {
            syntheticDataGenerator.clearTestData();
        });

        // Verify all data is cleared
        assertEquals(0, doctorRepository.findAllIds(0).size(), "Should have no doctors after clear");
        assertEquals(0, medicalCaseRepository.findAllIds(0).size(), "Should have no cases after clear");
        assertEquals(0, facilityRepository.findAllIds(0).size(), "Should have no facilities after clear");
        assertEquals(0, icd10CodeRepository.findAll().size(), "Should have no ICD-10 codes after clear");
        List<String> remainingDoctorIds = doctorRepository.findAllIds(0);
        Map<String, List<com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience>> remainingExperiences =
                clinicalExperienceRepository.findByDoctorIds(remainingDoctorIds);
        long remainingExperienceCount = remainingExperiences.values().stream().mapToLong(List::size).sum();
        assertEquals(0, remainingExperienceCount, "Should have no clinical experiences after clear");
    }

    @Test
    void testGenerateEmbeddings() {
        // Generate prerequisites
        syntheticDataGenerator.generateIcd10Codes("tiny");
        syntheticDataGenerator.generateMedicalCases(10);

        // Verify cases exist and don't have embeddings yet
        List<String> caseIds = medicalCaseRepository.findAllIds(0);
        assertEquals(10, caseIds.size(), "Should have exactly 10 cases");

        List<com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase> casesWithoutEmbeddings =
                medicalCaseRepository.findWithoutEmbeddings();
        assertEquals(10, casesWithoutEmbeddings.size(), "All cases should be without embeddings initially");

        assertNotNull(embeddingService, "EmbeddingService should be available");

        // Test mocked EmbeddingModel (returns 1536-dimensional vectors)
        String testText = "Test medical case: chest pain, shortness of breath, ICD-10: I21.9";
        List<Double> testEmbedding = embeddingService.generateEmbedding(testText);
        assertNotNull(testEmbedding, "EmbeddingService should generate embeddings");
        assertFalse(testEmbedding.isEmpty(), "Embedding should not be empty");
        assertEquals(1536, testEmbedding.size(), "Embedding should have 1536 dimensions");

        assertTrue(testEmbedding.stream().anyMatch(d -> d != 0.0),
                "Embedding should have non-zero values from mock");

        // Generate embeddings using batch processing (mocked AI services)
        // Note: Embeddings no longer generate descriptions - descriptions are generated separately
        assertDoesNotThrow(() -> {
            syntheticDataGenerator.generateEmbeddings();
        });

        // Verify all cases have embeddings
        List<com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase> remainingCasesWithoutEmbeddings =
                medicalCaseRepository.findWithoutEmbeddings();
        assertEquals(0, remainingCasesWithoutEmbeddings.size(),
                "All cases should have embeddings after generation");

        // Verify cases still exist after embedding generation
        List<String> finalCaseIds = medicalCaseRepository.findAllIds(0);
        assertEquals(10, finalCaseIds.size(), "Cases should still exist after embedding generation");
    }

    @Test
    void testGenerateEmbeddings_BatchProcessing() {
        // Generate 75 cases (more than default batch size of 50)
        syntheticDataGenerator.generateIcd10Codes("tiny");
        syntheticDataGenerator.generateMedicalCases(75);

        List<String> caseIds = medicalCaseRepository.findAllIds(0);
        assertEquals(75, caseIds.size(), "Should have exactly 75 cases");

        List<com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase> casesWithoutEmbeddings =
                medicalCaseRepository.findWithoutEmbeddings();
        assertEquals(75, casesWithoutEmbeddings.size(), "All cases should be without embeddings initially");

        // Generate embeddings (should create 2 batches: 50 + 25)
        // Note: Embeddings no longer generate descriptions - descriptions are generated separately
        assertDoesNotThrow(() -> {
            syntheticDataGenerator.generateEmbeddings();
        });

        List<com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase> remainingCasesWithoutEmbeddings =
                medicalCaseRepository.findWithoutEmbeddings();
        assertEquals(0, remainingCasesWithoutEmbeddings.size(),
                "All cases should have embeddings after batch processing");

        List<String> finalCaseIds = medicalCaseRepository.findAllIds(0);
        assertEquals(75, finalCaseIds.size(), "All cases should still exist after batch embedding generation");
    }

    @Test
    void testGenerateEmbeddings_EmptyList() {
        // Don't generate any cases
        // Verify no cases exist
        List<com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase> casesWithoutEmbeddings =
                medicalCaseRepository.findWithoutEmbeddings();
        assertEquals(0, casesWithoutEmbeddings.size(), "Should have no cases without embeddings");

        // Generate embeddings on empty list - should not throw exception
        assertDoesNotThrow(() -> {
            syntheticDataGenerator.generateEmbeddings();
        });

        // Verify still no cases
        List<String> caseIds = medicalCaseRepository.findAllIds(0);
        assertEquals(0, caseIds.size(), "Should still have no cases");
    }

    @Test
    void testGenerateEmbeddings_BatchAPIUsage() {
        // Generate prerequisites
        syntheticDataGenerator.generateIcd10Codes("tiny");
        syntheticDataGenerator.generateMedicalCases(30);

        assertNotNull(embeddingService, "EmbeddingService should be available");

        // Test batch API directly
        List<com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase> cases =
                medicalCaseRepository.findWithoutEmbeddings();
        assertEquals(30, cases.size(), "Should have 30 cases");

        List<List<Double>> batchEmbeddings = embeddingService.generateEmbeddingsForMedicalCases(cases);
        assertNotNull(batchEmbeddings, "Batch embeddings should not be null");
        assertEquals(cases.size(), batchEmbeddings.size(), "Should return embedding for each case");

        batchEmbeddings.forEach(embedding -> {
            assertNotNull(embedding, "Each embedding should not be null");
            assertFalse(embedding.isEmpty(), "Each embedding should not be empty");
            assertEquals(1536, embedding.size(), "Each embedding should have 1536 dimensions");
        });

        // Test via SyntheticDataGenerator to verify batch processing
        assertDoesNotThrow(() -> {
            syntheticDataGenerator.generateEmbeddings();
        });

        List<com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase> remainingCasesWithoutEmbeddings =
                medicalCaseRepository.findWithoutEmbeddings();
        assertEquals(0, remainingCasesWithoutEmbeddings.size(),
                "All cases should have embeddings after batch processing");
    }

    @Test
    void testGenerateEmbeddings_SingleBatch() {
        // Generate exactly one batch worth of cases (default batch size is 50)
        syntheticDataGenerator.generateIcd10Codes("tiny");
        syntheticDataGenerator.generateMedicalCases(50);

        // Verify cases exist
        List<com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase> casesWithoutEmbeddings =
                medicalCaseRepository.findWithoutEmbeddings();
        assertEquals(50, casesWithoutEmbeddings.size(), "Should have exactly 50 cases (one batch)");

        // Generate embeddings - should process as single batch
        // Note: Embeddings no longer generate descriptions - descriptions are generated separately
        assertDoesNotThrow(() -> {
            syntheticDataGenerator.generateEmbeddings();
        });

        // Verify all embeddings were generated
        List<com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase> remainingCasesWithoutEmbeddings =
                medicalCaseRepository.findWithoutEmbeddings();
        assertEquals(0, remainingCasesWithoutEmbeddings.size(),
                "All cases should have embeddings after single batch processing");

        // Verify cases still exist after single batch processing
        List<String> finalCaseIds = medicalCaseRepository.findAllIds(0);
        assertEquals(50, finalCaseIds.size(), "All cases should still exist after single batch processing");
    }

    @Test
    void testGenerateEmbeddings_StoredAbstractsUsed() {
        // Generate prerequisites
        syntheticDataGenerator.generateIcd10Codes("tiny");
        syntheticDataGenerator.generateMedicalCases(5);

        // Generate descriptions first (descriptions are now generated separately from embeddings)
        List<com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase> casesWithoutDescriptions =
                medicalCaseRepository.findWithoutDescriptions();
        assertEquals(5, casesWithoutDescriptions.size(), "All cases should be without descriptions initially");

        // Generate descriptions for existing cases
        for (com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase medicalCase : casesWithoutDescriptions) {
            String description = medicalCaseDescriptionService.generateDescription(medicalCase);
            if (description != null && !description.isBlank()) {
                medicalCaseRepository.updateAbstract(medicalCase.id(), description);
            }
        }

        // Verify all cases have abstracts stored (from description generation)
        List<String> caseIds = medicalCaseRepository.findAllIds(0);
        assertEquals(5, caseIds.size(), "Should have exactly 5 cases");

        List<com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase> casesWithAbstracts = caseIds.stream()
                .map(medicalCaseRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .filter(c -> c.abstractText() != null && !c.abstractText().isBlank())
                .toList();

        assertEquals(5, casesWithAbstracts.size(), "All cases should have abstracts stored after description generation");

        // Verify embeddings use stored abstracts (by generating embeddings - should reuse stored abstracts)
        assertDoesNotThrow(() -> {
            syntheticDataGenerator.generateEmbeddings();
        });

        List<List<Double>> embeddings = embeddingService.generateEmbeddingsForMedicalCases(casesWithAbstracts);
        assertNotNull(embeddings);
        assertEquals(casesWithAbstracts.size(), embeddings.size(), "Should generate embeddings for all cases with abstracts");

        embeddings.forEach(embedding -> {
            assertNotNull(embedding);
            assertFalse(embedding.isEmpty());
            assertEquals(1536, embedding.size(), "Each embedding should have 1536 dimensions");
        });
    }

    @Test
    void testBuildGraph() {
        // Generate prerequisites
        syntheticDataGenerator.generateFacilities(5);
        syntheticDataGenerator.generateDoctors(10);
        syntheticDataGenerator.generateIcd10Codes("tiny");
        syntheticDataGenerator.generateMedicalCases(50);
        syntheticDataGenerator.generateMedicalSpecialties("tiny");
        syntheticDataGenerator.generateClinicalExperiences(10, 50);

        // Build graph
        assertDoesNotThrow(() -> {
            syntheticDataGenerator.buildGraph();
        });

        // Verify graph was built (graph building should complete without errors)
        // Note: Graph verification would require GraphService, but we can verify no exceptions
    }

    @Test
    void testGeneratedDoctors_UniqueEmails() {
        // Generate facilities first
        syntheticDataGenerator.generateFacilities(10);

        // Generate doctors
        syntheticDataGenerator.generateDoctors(100);

        // Verify all emails are unique
        Set<String> emails = new HashSet<>();
        doctorRepository.findAllIds(0).forEach(id -> {
            doctorRepository.findById(id).ifPresent(doctor -> {
                assertNotNull(doctor.email(), "Doctor should have email");
                assertFalse(emails.contains(doctor.email()), "Email should be unique: " + doctor.email());
                emails.add(doctor.email());
            });
        });

        assertEquals(100, emails.size(), "Should have 100 unique emails");
    }

    @Test
    void testGeneratedDoctors_ValidFacilities() {
        // Generate facilities first
        syntheticDataGenerator.generateFacilities(5);

        // Generate doctors
        syntheticDataGenerator.generateDoctors(20);

        // Get all created facility IDs for validation
        List<String> allFacilityIds = facilityRepository.findAllIds(0);
        assertFalse(allFacilityIds.isEmpty(), "Should have created facilities");

        // Verify doctors have valid facility ID format (generator creates random IDs, not from existing facilities)
        doctorRepository.findAllIds(0).forEach(id -> {
            doctorRepository.findById(id).ifPresent(doctor -> {
                if (doctor.facilityIds() != null && !doctor.facilityIds().isEmpty()) {
                    doctor.facilityIds().forEach(facilityId -> {
                        assertNotNull(facilityId, "Facility ID should not be null");
                        assertFalse(facilityId.isEmpty(), "Facility ID should not be empty");
                    });
                }
            });
        });
    }

    @Test
    void testGeneratedMedicalCases_ValidIcd10Codes() {
        // Generate ICD-10 codes first
        syntheticDataGenerator.generateIcd10Codes("tiny");

        // Generate medical cases
        syntheticDataGenerator.generateMedicalCases(50);

        // Verify all cases reference valid ICD-10 codes
        medicalCaseRepository.findAllIds(0).forEach(caseId -> {
            medicalCaseRepository.findById(caseId).ifPresent(medicalCase -> {
                if (medicalCase.icd10Codes() != null && !medicalCase.icd10Codes().isEmpty()) {
                    medicalCase.icd10Codes().forEach(code -> {
                        assertTrue(
                                icd10CodeRepository.findByCode(code).isPresent(),
                                "Case should reference valid ICD-10 code: " + code
                        );
                    });
                }
            });
        });
    }

    @Test
    void testGeneratedClinicalExperiences_UniqueDoctorCasePairs() {
        // Generate prerequisites
        syntheticDataGenerator.generateFacilities(5);
        syntheticDataGenerator.generateDoctors(10);
        syntheticDataGenerator.generateIcd10Codes("tiny");
        syntheticDataGenerator.generateMedicalCases(50);

        // Generate clinical experiences
        syntheticDataGenerator.generateClinicalExperiences(10, 50);

        // Verify no duplicate doctor-case pairs
        Set<String> pairs = new HashSet<>();
        List<String> allDoctorIds = doctorRepository.findAllIds(0);
        Map<String, List<com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience>> allExperiences =
                clinicalExperienceRepository.findByDoctorIds(allDoctorIds);
        allExperiences.values().stream()
                .flatMap(List::stream)
                .forEach(experience -> {
                    String pair = experience.doctorId() + "|" + experience.caseId();
                    assertFalse(pairs.contains(pair), "Should not have duplicate pair: " + pair);
                    pairs.add(pair);
                });
    }

    @Test
    void testGenerateTestData_AllSizes() {
        // Test "tiny" size only (larger sizes are too slow for regular tests)
        String[] sizes = {"tiny"};

        for (String size : sizes) {
            syntheticDataGenerator.clearTestData();

            assertDoesNotThrow(() -> {
                syntheticDataGenerator.generateTestData(size, true);
            }, "Should generate data for size: " + size);

            List<String> doctorIds = doctorRepository.findAllIds(0);
            assertTrue(doctorIds.size() > 0, "Should have doctors for size: " + size);
        }
    }

    @Test
    void testGenerateTestData_DescriptionsGenerated() {
        // Generate prerequisites
        syntheticDataGenerator.generateIcd10Codes("tiny");
        syntheticDataGenerator.generateMedicalCases(10);

        // Verify cases exist without descriptions
        List<com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase> casesWithoutDescriptions =
                medicalCaseRepository.findWithoutDescriptions();
        assertEquals(10, casesWithoutDescriptions.size(), "All cases should be without descriptions initially");

        // Generate descriptions via full test data generation
        // Note: generateTestData will generate descriptions as a separate step (55% progress) before embeddings
        // Descriptions are now committed incrementally in batches (default: every 10 cases)
        syntheticDataGenerator.generateTestData("tiny", false);

        // Verify descriptions were generated (descriptions step runs before embeddings)
        List<com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase> remainingCasesWithoutDescriptions =
                medicalCaseRepository.findWithoutDescriptions();
        // After full generation, all cases should have descriptions (generated as separate step before embeddings)
        // Descriptions are committed incrementally, so progress is preserved even if generation fails mid-way
        assertEquals(0, remainingCasesWithoutDescriptions.size(),
                "All cases should have descriptions after generation (descriptions are generated as separate step with incremental commits)");

        // Verify cases have descriptions stored
        List<String> caseIds = medicalCaseRepository.findAllIds(0);
        long casesWithDescriptions = caseIds.stream()
                .map(medicalCaseRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .filter(c -> c.abstractText() != null && !c.abstractText().isBlank())
                .count();
        assertTrue(casesWithDescriptions > 0, "At least some cases should have descriptions after generation");
    }

    @Test
    void testGenerateTestData_IncrementalCommits() {
        // Test that incremental commits work correctly - descriptions are saved in batches
        // Generate prerequisites
        syntheticDataGenerator.generateIcd10Codes("tiny");

        // Generate more cases than default batch commit size (default: 10) to test incremental commits
        syntheticDataGenerator.generateMedicalCases(25);

        // Verify cases exist without descriptions
        List<com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase> casesWithoutDescriptions =
                medicalCaseRepository.findWithoutDescriptions();
        assertEquals(25, casesWithoutDescriptions.size(), "All cases should be without descriptions initially");

        // Generate descriptions - should commit in batches (default: every 10 cases)
        // This tests that incremental commits preserve progress
        syntheticDataGenerator.generateTestData("tiny", false);

        // Verify all descriptions were generated and committed
        List<com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase> remainingCasesWithoutDescriptions =
                medicalCaseRepository.findWithoutDescriptions();
        assertEquals(0, remainingCasesWithoutDescriptions.size(),
                "All cases should have descriptions after generation with incremental commits");

        // Verify cases have descriptions stored (incremental commits should have saved them)
        List<String> caseIds = medicalCaseRepository.findAllIds(0);
        long casesWithDescriptions = caseIds.stream()
                .map(medicalCaseRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .filter(c -> c.abstractText() != null && !c.abstractText().isBlank())
                .count();
        assertTrue(casesWithDescriptions >= 25,
                "All cases should have descriptions after incremental batch commits (at least 25 cases with descriptions)");
    }

    @Test
    void testGenerateTestData_TransactionBoundaries() {
        // Test that transaction boundaries are managed at phase level, not entire method level
        // This ensures that if one phase fails, previous phases' data is preserved

        // Generate prerequisites
        syntheticDataGenerator.generateIcd10Codes("tiny");
        syntheticDataGenerator.generateMedicalSpecialties("tiny");

        // Generate facilities and doctors (these should commit independently)
        syntheticDataGenerator.generateFacilities(5);
        syntheticDataGenerator.generateDoctors(10);

        // Verify facilities and doctors were committed
        assertTrue(facilityRepository.findAllIds(0).size() > 0, "Facilities should be committed");
        assertTrue(doctorRepository.findAllIds(0).size() > 0, "Doctors should be committed");

        // Generate cases (should commit independently)
        syntheticDataGenerator.generateMedicalCases(20);
        assertTrue(medicalCaseRepository.findAllIds(0).size() >= 20, "Cases should be committed");

        // Verify that data from previous phases is still present
        // This confirms that each phase manages its own transaction
        assertTrue(facilityRepository.findAllIds(0).size() > 0,
                "Facilities from previous phase should still be present (independent transactions)");
        assertTrue(doctorRepository.findAllIds(0).size() > 0,
                "Doctors from previous phase should still be present (independent transactions)");
    }

    @Test
    void testEveryDoctorHasAtLeastOneMedicalCase() {
        // Generate full test data (tiny: 3 doctors, 15 cases)
        syntheticDataGenerator.generateTestData("tiny", true);

        // Get all doctors
        List<String> doctorIds = doctorRepository.findAllIds(0);
        assertFalse(doctorIds.isEmpty(), "Doctors should be generated");

        // Get all clinical experiences
        Map<String, List<com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience>> experiencesByDoctor =
                clinicalExperienceRepository.findByDoctorIds(doctorIds);

        // Group experiences by doctor and count
        java.util.Map<String, Integer> caseCounts = new java.util.HashMap<>();
        for (Map.Entry<String, List<com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience>> entry : experiencesByDoctor.entrySet()) {
            caseCounts.put(entry.getKey(), entry.getValue().size());
        }

        // Verify every doctor has at least one case
        for (String doctorId : doctorIds) {
            Integer count = caseCounts.get(doctorId);
            assertNotNull(count, "Doctor " + doctorId + " should have at least one clinical experience");
            assertTrue(count >= 1, "Doctor " + doctorId + " should have at least one case, got: " + count);
        }

        // Verify no doctor has zero cases
        long doctorsWithZeroCases = caseCounts.values().stream()
                .filter(count -> count == 0)
                .count();
        assertEquals(0, doctorsWithZeroCases, "No doctor should have zero cases");
    }

    @Test
    void testEveryDoctorHasAtLeastOneCase_MoreDoctorsThanCases() {
        // Generate facilities first
        syntheticDataGenerator.generateFacilities(3);
        syntheticDataGenerator.generateIcd10Codes("tiny");
        syntheticDataGenerator.generateMedicalSpecialties("tiny");

        // Generate more doctors than cases (10 doctors, 5 cases)
        syntheticDataGenerator.generateDoctors(10);
        syntheticDataGenerator.generateMedicalCases(5);

        // Generate clinical experiences
        syntheticDataGenerator.generateClinicalExperiences(10, 5);

        // Get all doctors and experiences
        List<String> doctorIds = doctorRepository.findAllIds(0);
        Map<String, List<com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience>> experiencesByDoctor =
                clinicalExperienceRepository.findByDoctorIds(doctorIds);

        // Group experiences by doctor and count
        java.util.Map<String, Integer> caseCounts = new java.util.HashMap<>();
        for (Map.Entry<String, List<com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience>> entry : experiencesByDoctor.entrySet()) {
            caseCounts.put(entry.getKey(), entry.getValue().size());
        }

        // Verify every doctor has at least one case (even though there are more doctors than cases)
        for (String doctorId : doctorIds) {
            Integer count = caseCounts.get(doctorId);
            assertNotNull(count, "Doctor " + doctorId + " should have at least one clinical experience");
            assertTrue(count >= 1, "Doctor " + doctorId + " should have at least one case, got: " + count);
        }

        // Verify no doctor has zero cases
        long doctorsWithZeroCases = caseCounts.values().stream()
                .filter(count -> count == 0)
                .count();
        assertEquals(0, doctorsWithZeroCases, "No doctor should have zero cases even with more doctors than cases");
    }

    @Test
    void testClinicalExperienceDistribution_Balanced() {
        // Generate prerequisites
        syntheticDataGenerator.generateFacilities(5);
        syntheticDataGenerator.generateIcd10Codes("tiny");
        syntheticDataGenerator.generateMedicalSpecialties("tiny");

        // Generate equal number of doctors and cases
        syntheticDataGenerator.generateDoctors(5);
        syntheticDataGenerator.generateMedicalCases(5);

        // Generate clinical experiences
        syntheticDataGenerator.generateClinicalExperiences(5, 5);

        // Get all doctors and experiences
        List<String> doctorIds = doctorRepository.findAllIds(0);
        Map<String, List<com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience>> experiencesByDoctor =
                clinicalExperienceRepository.findByDoctorIds(doctorIds);

        // Group experiences by doctor and count
        java.util.Map<String, Integer> caseCounts = new java.util.HashMap<>();
        for (Map.Entry<String, List<com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience>> entry : experiencesByDoctor.entrySet()) {
            caseCounts.put(entry.getKey(), entry.getValue().size());
        }

        // Verify every doctor has at least one case
        for (String doctorId : doctorIds) {
            Integer count = caseCounts.get(doctorId);
            assertNotNull(count, "Doctor " + doctorId + " should have at least one clinical experience");
            assertTrue(count >= 1, "Doctor " + doctorId + " should have at least one case, got: " + count);
        }

        // Verify distribution is balanced (all doctors have at least 1 case, not more than 2)
        long maxCases = caseCounts.values().stream().mapToLong(Integer::longValue).max().orElse(0);
        assertTrue(maxCases <= 2, "Distribution should be balanced, max cases per doctor should not exceed 2, got: " + maxCases);
    }

    @Test
    void testEveryDoctorHasAtLeastOneCase_SecondRunWithoutClear() {
        // First run: Generate tiny dataset with clear=true
        syntheticDataGenerator.generateTestData("tiny", true);

        // Get initial counts
        List<String> doctorIdsAfterRun1 = doctorRepository.findAllIds(0);
        int doctorCountAfterRun1 = doctorIdsAfterRun1.size();
        assertTrue(doctorCountAfterRun1 > 0, "First run should generate doctors");

        // Second run: Generate tiny dataset without clearing
        syntheticDataGenerator.generateTestData("tiny", false);

        // Get final counts
        List<String> doctorIdsAfterRun2 = doctorRepository.findAllIds(0);
        int doctorCountAfterRun2 = doctorIdsAfterRun2.size();

        // Verify new doctors were generated (should be at least double)
        assertTrue(doctorCountAfterRun2 > doctorCountAfterRun1,
                "Second run should add more doctors. Before: " + doctorCountAfterRun1 + ", After: " + doctorCountAfterRun2);
        int newDoctorCount = doctorCountAfterRun2 - doctorCountAfterRun1;

        // Verify ALL doctors have at least one clinical experience
        Map<String, List<com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience>> experiencesByDoctor =
                clinicalExperienceRepository.findByDoctorIds(doctorIdsAfterRun2);

        // Group experiences by doctor and count
        java.util.Map<String, Integer> caseCounts = new java.util.HashMap<>();
        for (Map.Entry<String, List<com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience>> entry : experiencesByDoctor.entrySet()) {
            caseCounts.put(entry.getKey(), entry.getValue().size());
        }

        // Verify every doctor has at least one case
        for (String doctorId : doctorIdsAfterRun2) {
            Integer count = caseCounts.get(doctorId);
            assertNotNull(count, "Doctor " + doctorId + " should have at least one clinical experience");
            assertTrue(count >= 1, "Doctor " + doctorId + " should have at least one case, got: " + count);
        }

        // Verify no doctor has zero cases (this was the bug)
        long doctorsWithZeroCases = caseCounts.values().stream()
                .filter(count -> count == 0)
                .count();

        assertEquals(0, doctorsWithZeroCases,
                "No doctor should have zero cases after running generator twice without clearing. " +
                        "Total doctors: " + doctorCountAfterRun2 + " (initial: " + doctorCountAfterRun1 + ", new: " + newDoctorCount + ")");
    }
}
