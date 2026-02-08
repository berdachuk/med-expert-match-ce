package com.berdachuk.medexpertmatch.ingestion.service;

import com.berdachuk.medexpertmatch.clinicalexperience.repository.ClinicalExperienceRepository;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.doctor.domain.MedicalSpecialty;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.doctor.repository.MedicalSpecialtyRepository;
import com.berdachuk.medexpertmatch.facility.repository.FacilityRepository;
import com.berdachuk.medexpertmatch.graph.service.MedicalGraphBuilderService;
import com.berdachuk.medexpertmatch.ingestion.exception.SyntheticDataGenerationException;
import com.berdachuk.medexpertmatch.ingestion.util.CsvDataLoader;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.medicalcase.service.MedicalCaseDescriptionService;
import com.berdachuk.medexpertmatch.medicalcoding.domain.ICD10Code;
import com.berdachuk.medexpertmatch.medicalcoding.domain.Procedure;
import com.berdachuk.medexpertmatch.medicalcoding.repository.ICD10CodeRepository;
import com.berdachuk.medexpertmatch.medicalcoding.repository.ProcedureRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r5.model.Bundle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for generating synthetic FHIR R5 compliant data using Datafaker.
 * Uses mocked AI services in tests, real MedGemma models in production.
 */
@Slf4j
@Service
public class SyntheticDataGenerator {

    // Data arrays removed - now loaded from external CSV files via @PostConstruct

    private final ResourceLoader resourceLoader;
    // Cache for extended results per size (for hierarchical reuse)
    private final Map<String, List<String>> cachedIcd10Codes = new ConcurrentHashMap<>();
    private final Map<String, List<String>> cachedSpecialties = new ConcurrentHashMap<>();
    private final Map<String, List<String>> cachedProcedures = new ConcurrentHashMap<>();
    private final DoctorRepository doctorRepository;
    private final DoctorGeneratorService doctorGeneratorService;
    private final MedicalCaseRepository medicalCaseRepository;
    private final MedicalCaseGeneratorService medicalCaseGeneratorService;
    private final ClinicalExperienceRepository clinicalExperienceRepository;
    private final ClinicalExperienceGeneratorService clinicalExperienceGeneratorService;
    private final EmbeddingGeneratorService embeddingGeneratorService;
    private final ICD10CodeRepository icd10CodeRepository;
    private final ProcedureRepository procedureRepository;
    private final FacilityRepository facilityRepository;
    private final FacilityGeneratorService facilityGeneratorService;
    private final MedicalSpecialtyRepository medicalSpecialtyRepository;
    private final SyntheticDataGenerationProgressService progressService;
    private final MedicalGraphBuilderService graphBuilderService;
    private final MedicalCaseDescriptionService medicalCaseDescriptionService;
    // Specialty to procedures mapping (loaded from CSV)
    private final Map<String, List<String>> specialtyProceduresMap = new HashMap<>();
    // Data size configurations loaded from CSV
    private final Map<String, DataSizeConfig> dataSizeConfigs = new LinkedHashMap<>();
    // Extended procedures list (populated based on test data size)
    private List<String> extendedProcedures = new ArrayList<>();
    // Extended ICD-10 codes list (populated based on test data size)
    private List<String> extendedIcd10Codes = new ArrayList<>();
    // Loaded from external CSV files via @PostConstruct
    private List<String> loadedMedicalSpecialties = new ArrayList<>();
    private List<String> loadedIcd10Codes = new ArrayList<>();
    private List<String> loadedProcedures = new ArrayList<>();
    private List<String> loadedFacilityTypes = new ArrayList<>();
    private List<String> loadedComplexityLevels = new ArrayList<>();
    private List<String> loadedOutcomes = new ArrayList<>();
    private List<String> loadedAvailabilityStatuses = new ArrayList<>();
    private List<String> loadedSeverities = new ArrayList<>();
    private List<String> loadedSymptoms = new ArrayList<>();
    private List<String> loadedEncounterClasses = new ArrayList<>();
    private List<String> loadedEncounterTypes = new ArrayList<>();
    private List<String> loadedComplications = new ArrayList<>();
    private List<String> loadedFacilityCapabilities = new ArrayList<>();
    // ICD-10 code to description mapping (loaded from CSV)
    private Map<String, String> icd10CodeDisplays = new HashMap<>();
    // Encounter class code to display mapping (loaded from CSV)
    private Map<String, String> encounterClassDisplays = new HashMap<>();
    @Value("${medexpertmatch.synthetic-data.data-files.medical-specialties:classpath:/data/medical-specialties.csv}")
    private String medicalSpecialtiesFile;
    @Value("${medexpertmatch.synthetic-data.data-files.icd10-codes:classpath:/data/icd10-codes.csv}")
    private String icd10CodesFile;
    @Value("${medexpertmatch.synthetic-data.data-files.procedures:classpath:/data/procedures.csv}")
    private String proceduresFile;
    @Value("${medexpertmatch.synthetic-data.data-files.facility-types:classpath:/data/facility-types.csv}")
    private String facilityTypesFile;
    @Value("${medexpertmatch.synthetic-data.data-files.complexity-levels:classpath:/data/complexity-levels.csv}")
    private String complexityLevelsFile;
    @Value("${medexpertmatch.synthetic-data.data-files.outcomes:classpath:/data/outcomes.csv}")
    private String outcomesFile;
    @Value("${medexpertmatch.synthetic-data.data-files.availability-statuses:classpath:/data/availability-statuses.csv}")
    private String availabilityStatusesFile;
    @Value("${medexpertmatch.synthetic-data.data-files.severities:classpath:/data/severities.csv}")
    private String severitiesFile;
    @Value("${medexpertmatch.synthetic-data.data-files.symptoms:classpath:/data/symptoms.csv}")
    private String symptomsFile;
    @Value("${medexpertmatch.synthetic-data.data-files.encounter-classes:classpath:/data/encounter-classes.csv}")
    private String encounterClassesFile;
    @Value("${medexpertmatch.synthetic-data.data-files.encounter-types:classpath:/data/encounter-types.csv}")
    private String encounterTypesFile;
    @Value("${medexpertmatch.synthetic-data.data-files.complications:classpath:/data/complications.csv}")
    private String complicationsFile;
    @Value("${medexpertmatch.synthetic-data.data-files.facility-capabilities:classpath:/data/facility-capabilities.csv}")
    private String facilityCapabilitiesFile;
    @Value("${medexpertmatch.synthetic-data.data-files.specialty-procedures:classpath:/data/specialty-procedures.csv}")
    private String specialtyProceduresFile;
    @Value("${medexpertmatch.synthetic-data.data-files.data-sizes:classpath:/data/data-sizes.csv}")
    private String dataSizesFile;
    // Configuration properties
    @Value("${medexpertmatch.synthetic-data.batch-size:5000}")
    private int batchSize;
    @Value("${medexpertmatch.synthetic-data.description.batch-commit-size:10}")
    private int descriptionBatchCommitSize;

    public SyntheticDataGenerator(
            ResourceLoader resourceLoader,
            DoctorRepository doctorRepository,
            DoctorGeneratorService doctorGeneratorService,
            MedicalCaseRepository medicalCaseRepository,
            MedicalCaseGeneratorService medicalCaseGeneratorService,
            ClinicalExperienceRepository clinicalExperienceRepository,
            ClinicalExperienceGeneratorService clinicalExperienceGeneratorService,
            EmbeddingGeneratorService embeddingGeneratorService,
            ICD10CodeRepository icd10CodeRepository,
            ProcedureRepository procedureRepository,
            FacilityRepository facilityRepository,
            FacilityGeneratorService facilityGeneratorService,
            MedicalSpecialtyRepository medicalSpecialtyRepository,
            SyntheticDataGenerationProgressService progressService,
            MedicalGraphBuilderService graphBuilderService,
            MedicalCaseDescriptionService medicalCaseDescriptionService) {
        this.resourceLoader = resourceLoader;
        this.doctorRepository = doctorRepository;
        this.doctorGeneratorService = doctorGeneratorService;
        this.medicalCaseRepository = medicalCaseRepository;
        this.medicalCaseGeneratorService = medicalCaseGeneratorService;
        this.clinicalExperienceRepository = clinicalExperienceRepository;
        this.clinicalExperienceGeneratorService = clinicalExperienceGeneratorService;
        this.embeddingGeneratorService = embeddingGeneratorService;
        this.icd10CodeRepository = icd10CodeRepository;
        this.procedureRepository = procedureRepository;
        this.facilityRepository = facilityRepository;
        this.facilityGeneratorService = facilityGeneratorService;
        this.medicalSpecialtyRepository = medicalSpecialtyRepository;
        this.progressService = progressService;
        this.graphBuilderService = graphBuilderService;
        this.medicalCaseDescriptionService = medicalCaseDescriptionService;

    }

    /**
     * Loads constants from external CSV files and persists relational data to database.
     * Files are required - application will fail to start if files are missing.
     */
    @PostConstruct
    public void loadDataFromFiles() {
        // Load and persist medical specialties from CSV
        List<Map<String, String>> specialtiesData = CsvDataLoader.loadCsv(resourceLoader, medicalSpecialtiesFile, "medical specialties");
        persistMedicalSpecialties(specialtiesData);
        loadedMedicalSpecialties = specialtiesData.stream()
                .map(row -> row.get("name"))
                .filter(name -> name != null && !name.isEmpty())
                .collect(Collectors.toList());
        log.info("Loaded {} medical specialties from {}", loadedMedicalSpecialties.size(), medicalSpecialtiesFile);

        // Load and persist ICD-10 codes from CSV
        List<Map<String, String>> icd10Data = CsvDataLoader.loadCsv(resourceLoader, icd10CodesFile, "ICD-10 codes");
        persistIcd10Codes(icd10Data);
        loadedIcd10Codes = icd10Data.stream()
                .map(row -> row.get("code"))
                .filter(code -> code != null && !code.isEmpty())
                .collect(Collectors.toList());
        // Populate ICD-10 code to description mapping from CSV
        icd10CodeDisplays = icd10Data.stream()
                .filter(row -> row.get("code") != null && row.get("description") != null)
                .collect(Collectors.toMap(
                        row -> row.get("code"),
                        row -> row.get("description"),
                        (existing, replacement) -> existing
                ));
        log.info("Loaded {} ICD-10 codes from {}", loadedIcd10Codes.size(), icd10CodesFile);

        // Load and persist procedures from CSV
        List<Map<String, String>> proceduresData = CsvDataLoader.loadCsv(resourceLoader, proceduresFile, "procedures");
        persistProcedures(proceduresData);
        loadedProcedures = proceduresData.stream()
                .map(row -> row.get("name"))
                .filter(name -> name != null && !name.isEmpty())
                .collect(Collectors.toList());
        log.info("Loaded {} procedures from {}", loadedProcedures.size(), proceduresFile);

        // Validate that files were loaded successfully
        if (loadedMedicalSpecialties.isEmpty()) {
            throw new IllegalStateException("Failed to load medical specialties from " + medicalSpecialtiesFile);
        }
        if (loadedIcd10Codes.isEmpty()) {
            throw new IllegalStateException("Failed to load ICD-10 codes from " + icd10CodesFile);
        }
        if (loadedProcedures.isEmpty()) {
            throw new IllegalStateException("Failed to load procedures from " + proceduresFile);
        }

        // Load facility types from CSV
        List<Map<String, String>> facilityTypesData = CsvDataLoader.loadCsv(resourceLoader, facilityTypesFile, "facility types");
        loadedFacilityTypes = facilityTypesData.stream()
                .map(row -> row.get("name"))
                .filter(name -> name != null && !name.isEmpty())
                .collect(Collectors.toList());
        log.info("Loaded {} facility types from {}", loadedFacilityTypes.size(), facilityTypesFile);

        // Load complexity levels from CSV
        List<Map<String, String>> complexityLevelsData = CsvDataLoader.loadCsv(resourceLoader, complexityLevelsFile, "complexity levels");
        loadedComplexityLevels = complexityLevelsData.stream()
                .map(row -> row.get("name"))
                .filter(name -> name != null && !name.isEmpty())
                .collect(Collectors.toList());
        log.info("Loaded {} complexity levels from {}", loadedComplexityLevels.size(), complexityLevelsFile);

        // Load outcomes from CSV
        List<Map<String, String>> outcomesData = CsvDataLoader.loadCsv(resourceLoader, outcomesFile, "outcomes");
        loadedOutcomes = outcomesData.stream()
                .map(row -> row.get("name"))
                .filter(name -> name != null && !name.isEmpty())
                .collect(Collectors.toList());
        log.info("Loaded {} outcomes from {}", loadedOutcomes.size(), outcomesFile);

        // Load availability statuses from CSV
        List<Map<String, String>> availabilityStatusesData = CsvDataLoader.loadCsv(resourceLoader, availabilityStatusesFile, "availability statuses");
        loadedAvailabilityStatuses = availabilityStatusesData.stream()
                .map(row -> row.get("name"))
                .filter(name -> name != null && !name.isEmpty())
                .collect(Collectors.toList());
        log.info("Loaded {} availability statuses from {}", loadedAvailabilityStatuses.size(), availabilityStatusesFile);

        // Load severities from CSV
        List<Map<String, String>> severitiesData = CsvDataLoader.loadCsv(resourceLoader, severitiesFile, "severities");
        loadedSeverities = severitiesData.stream()
                .map(row -> row.get("name"))
                .filter(name -> name != null && !name.isEmpty())
                .collect(Collectors.toList());
        log.info("Loaded {} severities from {}", loadedSeverities.size(), severitiesFile);

        // Load symptoms from CSV
        List<Map<String, String>> symptomsData = CsvDataLoader.loadCsv(resourceLoader, symptomsFile, "symptoms");
        loadedSymptoms = symptomsData.stream()
                .map(row -> row.get("name"))
                .filter(name -> name != null && !name.isEmpty())
                .collect(Collectors.toList());
        log.info("Loaded {} symptoms from {}", loadedSymptoms.size(), symptomsFile);

        // Load encounter classes from CSV
        List<Map<String, String>> encounterClassesData = CsvDataLoader.loadCsv(resourceLoader, encounterClassesFile, "encounter classes");
        loadedEncounterClasses = encounterClassesData.stream()
                .map(row -> row.get("code"))
                .filter(code -> code != null && !code.isEmpty())
                .collect(Collectors.toList());
        // Populate encounter class code to display mapping from CSV
        encounterClassDisplays = encounterClassesData.stream()
                .filter(row -> row.get("code") != null && row.get("display") != null)
                .collect(Collectors.toMap(
                        row -> row.get("code"),
                        row -> row.get("display"),
                        (existing, replacement) -> existing
                ));
        log.info("Loaded {} encounter classes from {}", loadedEncounterClasses.size(), encounterClassesFile);

        // Load encounter types from CSV
        List<Map<String, String>> encounterTypesData = CsvDataLoader.loadCsv(resourceLoader, encounterTypesFile, "encounter types");
        loadedEncounterTypes = encounterTypesData.stream()
                .map(row -> row.get("name"))
                .filter(name -> name != null && !name.isEmpty())
                .collect(Collectors.toList());
        log.info("Loaded {} encounter types from {}", loadedEncounterTypes.size(), encounterTypesFile);

        // Load complications from CSV
        List<Map<String, String>> complicationsData = CsvDataLoader.loadCsv(resourceLoader, complicationsFile, "complications");
        loadedComplications = complicationsData.stream()
                .map(row -> row.get("name"))
                .filter(name -> name != null && !name.isEmpty())
                .collect(Collectors.toList());
        log.info("Loaded {} complications from {}", loadedComplications.size(), complicationsFile);

        // Load facility capabilities from CSV
        List<Map<String, String>> facilityCapabilitiesData = CsvDataLoader.loadCsv(resourceLoader, facilityCapabilitiesFile, "facility capabilities");
        loadedFacilityCapabilities = facilityCapabilitiesData.stream()
                .map(row -> row.get("name"))
                .filter(name -> name != null && !name.isEmpty())
                .collect(Collectors.toList());
        log.info("Loaded {} facility capabilities from {}", loadedFacilityCapabilities.size(), facilityCapabilitiesFile);

        // Validate that all critical files were loaded successfully
        if (loadedFacilityTypes.isEmpty()) {
            throw new IllegalStateException("Failed to load facility types from " + facilityTypesFile);
        }
        if (loadedComplexityLevels.isEmpty()) {
            throw new IllegalStateException("Failed to load complexity levels from " + complexityLevelsFile);
        }
        if (loadedOutcomes.isEmpty()) {
            throw new IllegalStateException("Failed to load outcomes from " + outcomesFile);
        }
        if (loadedSymptoms.isEmpty()) {
            throw new IllegalStateException("Failed to load symptoms from " + symptomsFile);
        }
        if (loadedFacilityCapabilities.isEmpty()) {
            throw new IllegalStateException("Failed to load facility capabilities from " + facilityCapabilitiesFile);
        }

        // Load specialty-procedures mapping from CSV
        List<Map<String, String>> specialtyProceduresData = CsvDataLoader.loadCsv(resourceLoader, specialtyProceduresFile, "specialty-procedures");
        for (Map<String, String> row : specialtyProceduresData) {
            String specialtyName = row.get("specialty_name");
            String procedureNamesStr = row.getOrDefault("procedure_names", "");
            if (specialtyName != null && !specialtyName.isEmpty() && !procedureNamesStr.isEmpty()) {
                List<String> procedures = CsvDataLoader.parseCommaSeparatedList(procedureNamesStr);
                specialtyProceduresMap.put(specialtyName, procedures);
            }
        }
        log.info("Loaded {} specialty-procedure mappings from {}", specialtyProceduresMap.size(), specialtyProceduresFile);

        // Load data size configurations from CSV
        List<Map<String, String>> dataSizesData = CsvDataLoader.loadCsv(resourceLoader, dataSizesFile, "data sizes");
        for (Map<String, String> row : dataSizesData) {
            String size = row.get("size");
            String doctorCountStr = row.get("doctor_count");
            String caseCountStr = row.get("case_count");
            String description = row.getOrDefault("description", "");
            String estimatedTimeStr = row.getOrDefault("estimated_time_minutes", "0");

            if (size != null && !size.isEmpty() && doctorCountStr != null && caseCountStr != null) {
                try {
                    int doctorCount = Integer.parseInt(doctorCountStr);
                    int caseCount = Integer.parseInt(caseCountStr);
                    int estimatedTimeMinutes = Integer.parseInt(estimatedTimeStr);

                    DataSizeConfig config = new DataSizeConfig(
                            size.toLowerCase(),
                            doctorCount,
                            caseCount,
                            description.isEmpty() ? size : description,
                            estimatedTimeMinutes
                    );
                    dataSizeConfigs.put(size.toLowerCase(), config);
                } catch (NumberFormatException e) {
                    log.warn("Skipping invalid data size row: size={}, doctor_count={}, case_count={}", size, doctorCountStr, caseCountStr);
                }
            }
        }
        log.info("Loaded {} data size configurations from {}", dataSizeConfigs.size(), dataSizesFile);

        // Validate that data sizes were loaded
        if (dataSizeConfigs.isEmpty()) {
            throw new IllegalStateException("Failed to load data sizes from " + dataSizesFile);
        }

        // Initialize extended lists with loaded data
        extendedIcd10Codes = new ArrayList<>(loadedIcd10Codes);
        extendedProcedures = new ArrayList<>(loadedProcedures);
    }

    /**
     * Persists medical specialties from CSV data to database.
     * Must be public for @Transactional to work (Spring AOP doesn't intercept private methods).
     */
    @Transactional
    public void persistMedicalSpecialties(List<Map<String, String>> specialtiesData) {
        for (Map<String, String> row : specialtiesData) {
            String name = row.get("name");
            if (name == null || name.isEmpty()) {
                continue;
            }

            // Check if specialty already exists
            Optional<MedicalSpecialty> existing = medicalSpecialtyRepository.findByName(name);
            if (existing.isPresent()) {
                continue; // Skip if already exists
            }

            String id = IdGenerator.generateId();
            String normalizedName = CsvDataLoader.normalize(name);
            String description = row.getOrDefault("description", "");
            List<String> icd10CodeRanges = CsvDataLoader.parseCommaSeparatedList(row.getOrDefault("icd10_code_ranges", ""));
            List<String> relatedSpecialtyNames = CsvDataLoader.parseCommaSeparatedList(row.getOrDefault("related_specialty_names", ""));

            // Resolve related specialty IDs by name
            List<String> relatedSpecialtyIds = new ArrayList<>();
            for (String relatedName : relatedSpecialtyNames) {
                medicalSpecialtyRepository.findByName(relatedName)
                        .map(MedicalSpecialty::id)
                        .ifPresent(relatedSpecialtyIds::add);
            }

            MedicalSpecialty specialty = new MedicalSpecialty(
                    id,
                    name,
                    normalizedName,
                    description,
                    icd10CodeRanges,
                    relatedSpecialtyIds
            );

            try {
                medicalSpecialtyRepository.insert(specialty);
            } catch (DataAccessException e) {
                log.warn("Failed to insert medical specialty {}: {}", name, e.getMessage());
            }
        }
    }

    /**
     * Persists ICD-10 codes from CSV data to database.
     * Must be public for @Transactional to work (Spring AOP doesn't intercept private methods).
     */
    @Transactional
    public void persistIcd10Codes(List<Map<String, String>> icd10Data) {
        for (Map<String, String> row : icd10Data) {
            String code = row.get("code");
            if (code == null || code.isEmpty()) {
                continue;
            }

            // Check if code already exists
            Optional<ICD10Code> existing = icd10CodeRepository.findByCode(code);
            if (existing.isPresent()) {
                continue; // Skip if already exists
            }

            String id = IdGenerator.generateId();
            String description = row.getOrDefault("description", "");
            String category = row.getOrDefault("category", "");
            String parentCode = row.getOrDefault("parent_code", null);
            List<String> relatedCodes = CsvDataLoader.parseCommaSeparatedList(row.getOrDefault("related_codes", ""));

            ICD10Code icd10Code = new ICD10Code(
                    id,
                    code,
                    description,
                    category,
                    parentCode,
                    relatedCodes
            );

            try {
                icd10CodeRepository.insert(icd10Code);
            } catch (DataAccessException e) {
                log.warn("Failed to insert ICD-10 code {}: {}", code, e.getMessage());
            }
        }
    }

    /**
     * Persists procedures from CSV data to database.
     * Must be public for @Transactional to work (Spring AOP doesn't intercept private methods).
     */
    @Transactional
    public void persistProcedures(List<Map<String, String>> proceduresData) {
        for (Map<String, String> row : proceduresData) {
            String name = row.get("name");
            if (name == null || name.isEmpty()) {
                continue;
            }

            // Check if procedure already exists
            Optional<Procedure> existing = procedureRepository.findByName(name);
            if (existing.isPresent()) {
                continue; // Skip if already exists
            }

            String id = IdGenerator.generateId();
            String normalizedName = CsvDataLoader.normalize(name);
            String description = row.getOrDefault("description", "");
            String category = row.getOrDefault("category", "");

            Procedure procedure = new Procedure(
                    id,
                    name,
                    normalizedName,
                    description,
                    category
            );

            try {
                procedureRepository.insert(procedure);
            } catch (DataAccessException e) {
                log.warn("Failed to insert procedure {}: {}", name, e.getMessage());
            }
        }
    }


    /**
     * Generates test data based on size parameter.
     *
     * @param size  Data size: "tiny", "small", "medium", "large", "huge"
     * @param clear Whether to clear existing data first
     */
    public void generateTestData(String size, boolean clear) {
        generateTestData(size, clear, null);
    }

    /**
     * Generates test data based on size parameter with progress tracking.
     * Transaction boundaries are managed at individual phase level for incremental commits.
     *
     * @param size  Data size: "tiny", "small", "medium", "large", "huge"
     * @param clear Whether to clear existing data first
     * @param jobId Optional job ID for progress tracking
     */
    public void generateTestData(String size, boolean clear, String jobId) {
        log.info("Generating synthetic data - size: {}, clear: {}, jobId: {}", size, clear, jobId);

        SyntheticDataGenerationProgress progress = jobId != null ?
                progressService.getProgress(jobId) : null;

        if (progress != null) {
            progress.addTraceEntry("INFO", "Initializing", "Starting synthetic data generation...");
            progress.updateProgress(0, "Initializing", "Starting synthetic data generation...");
        }

        // Check for cancellation
        if (progress != null && progress.isCancelled()) {
            log.info("Generation cancelled before starting - jobId: {}", jobId);
            return;
        }

        if (clear) {
            if (progress != null) {
                progress.updateProgress(2, "Clearing", "Clearing existing synthetic data...");
            }
            clearTestData();

            // Check for cancellation after clearing
            if (progress != null && progress.isCancelled()) {
                log.info("Generation cancelled after clearing - jobId: {}", jobId);
                return;
            }
        }

        DataSizeConfig config = dataSizeConfigs.get(size.toLowerCase());
        if (config == null) {
            log.warn("Unknown size: {}, using medium", size);
            config = dataSizeConfigs.get("medium");
            if (config == null) {
                throw new IllegalArgumentException("No data size configuration found for size: " + size + " and default 'medium' is not available");
            }
        }

        int doctorCount = config.doctorCount();
        int caseCount = config.caseCount();

        // Generate reference data first (needed by other entities)
        if (progress != null) {
            progress.updateProgress(5, "ICD-10 Codes", "Generating ICD-10 codes...");
        }
        try {
            generateIcd10Codes(size, progress);
        } catch (SyntheticDataGenerationException e) {
            log.error("Failed to generate ICD-10 codes - jobId: {}", jobId, e);
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("Invalid input for ICD-10 code generation - jobId: {}", jobId, e);
            throw e;
        } catch (RuntimeException e) {
            if (progress != null && progress.isCancelled()) {
                log.info("Generation cancelled during ICD-10 codes - jobId: {}", jobId);
                return;
            }
            throw e;
        }
        if (progress != null && progress.isCancelled()) {
            log.info("Generation cancelled during ICD-10 codes - jobId: {}", jobId);
            return;
        }

        if (progress != null) {
            progress.updateProgress(10, "Specialties", "Generating medical specialties...");
        }
        try {
            generateMedicalSpecialties(size, progress);
        } catch (SyntheticDataGenerationException e) {
            log.error("Failed to generate medical specialties - jobId: {}", jobId, e);
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("Invalid input for medical specialty generation - jobId: {}", jobId, e);
            throw e;
        } catch (RuntimeException e) {
            if (progress != null && progress.isCancelled()) {
                log.info("Generation cancelled during specialties - jobId: {}", jobId);
                return;
            }
            throw e;
        }
        if (progress != null && progress.isCancelled()) {
            log.info("Generation cancelled during specialties - jobId: {}", jobId);
            return;
        }

        if (progress != null) {
            progress.updateProgress(12, "Procedures", "Extending procedures list...");
        }
        try {
            generateAdditionalProcedures(size, progress);
        } catch (SyntheticDataGenerationException e) {
            log.error("Failed to extend procedures - jobId: {}", jobId, e);
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("Invalid input for procedure extension - jobId: {}", jobId, e);
            throw e;
        } catch (RuntimeException e) {
            if (progress != null && progress.isCancelled()) {
                log.info("Generation cancelled during procedures - jobId: {}", jobId);
                return;
            }
            throw e;
        }
        if (progress != null && progress.isCancelled()) {
            log.info("Generation cancelled during procedures - jobId: {}", jobId);
            return;
        }

        // Generate facilities (independent, but referenced by doctors)
        int facilityCount = calculateFacilityCount(doctorCount);
        if (progress != null) {
            progress.updateProgress(15, "Facilities", String.format("Generating %d facilities...", facilityCount));
        }
        try {
            facilityGeneratorService.generateFacilities(facilityCount, loadedFacilityTypes, loadedFacilityCapabilities);
        } catch (Exception e) {
            log.error("Failed to generate facilities - jobId: {}", jobId, e);
            throw new SyntheticDataGenerationException("Failed to generate facilities", e);
        }
        if (progress != null && progress.isCancelled()) {
            log.info("Generation cancelled during facilities - jobId: {}", jobId);
            return;
        }

        // Generate doctors (references facilities)
        if (progress != null) {
            progress.updateProgress(20, "Doctors", String.format("Generating %d doctors...", doctorCount));
        }
        try {
            doctorGeneratorService.generateDoctors(doctorCount, progress, loadedMedicalSpecialties, loadedAvailabilityStatuses);
        } catch (Exception e) {
            log.error("Failed to generate doctors - jobId: {}", jobId, e);
            throw new SyntheticDataGenerationException("Failed to generate doctors", e);
        }
        if (progress != null && progress.isCancelled()) {
            log.info("Generation cancelled during doctors - jobId: {}", jobId);
            return;
        }

        // Generate medical cases (references ICD-10 codes)
        if (progress != null) {
            progress.updateProgress(40, "Medical Cases", String.format("Generating %d medical cases...", caseCount));
        }
        try {
            medicalCaseGeneratorService.generateMedicalCases(caseCount, progress, loadedIcd10Codes, extendedIcd10Codes,
                    loadedSymptoms, loadedSeverities, loadedEncounterClasses, loadedEncounterTypes);
        } catch (Exception e) {
            log.error("Failed to generate medical cases - jobId: {}", jobId, e);
            throw new SyntheticDataGenerationException("Failed to generate medical cases", e);
        }
        if (progress != null && progress.isCancelled()) {
            log.info("Generation cancelled during medical cases - jobId: {}", jobId);
            return;
        }

        // Generate medical case descriptions (can continue with partial failures)
        if (progress != null) {
            progress.updateProgress(55, "Descriptions", "Generating medical case descriptions...");
        }
        try {
            generateMedicalCaseDescriptions(progress);
        } catch (Exception e) {
            log.error("Error during description generation - jobId: {} (continuing with partial results)", jobId, e);
            // Continue generation even if descriptions fail - cases already exist
        }
        if (progress != null && progress.isCancelled()) {
            log.info("Generation cancelled during descriptions - jobId: {}", jobId);
            return;
        }

        // Generate embeddings for medical cases (can continue with partial failures)
        if (progress != null) {
            progress.updateProgress(70, "Embeddings", "Generating embeddings for medical cases...");
        }
        try {
            generateEmbeddings(progress);
        } catch (Exception e) {
            log.error("Error during embedding generation - jobId: {} (continuing with partial results)", jobId, e);
            // Continue generation even if embeddings fail - cases already exist
        }
        if (progress != null && progress.isCancelled()) {
            log.info("Generation cancelled during embeddings - jobId: {}", jobId);
            return;
        }

        // Generate clinical experiences (link doctors to cases) - can continue with partial failures
        if (progress != null) {
            progress.updateProgress(90, "Clinical Experiences", "Linking doctors to cases...");
        }
        try {
            clinicalExperienceGeneratorService.generateClinicalExperiences(doctorCount, caseCount, progress,
                    loadedComplexityLevels, loadedOutcomes, loadedComplications, specialtyProceduresMap, extendedProcedures);
        } catch (Exception e) {
            log.error("Error during clinical experience generation - jobId: {} (continuing with partial results)", jobId, e);
            // Continue generation even if clinical experiences fail - core data already exists
        }
        if (progress != null && progress.isCancelled()) {
            log.info("Generation cancelled during clinical experiences - jobId: {}", jobId);
            return;
        }

        // Build graph relationships (can continue if graph building fails)
        if (progress != null) {
            progress.updateProgress(95, "Graph Building", "Building Apache AGE graph...");
        }
        try {
            buildGraph();
        } catch (Exception e) {
            log.error("Error during graph building - jobId: {} (continuing - graph building is optional)", jobId, e);
            // Continue - graph building is optional, core data already exists
        }
        if (progress != null && progress.isCancelled()) {
            log.info("Generation cancelled after graph building - jobId: {}", jobId);
            return;
        }

        if (progress != null) {
            if (progress.isCancelled()) {
                log.info("Generation was cancelled - jobId: {}", jobId);
                return;
            }
            progress.updateProgress(100, "Complete", "Synthetic data generation complete");
            progress.complete();
        }

        log.info("Synthetic data generation complete - {} doctors, {} cases, {} facilities", doctorCount, caseCount, facilityCount);
    }

    /**
     * Returns available data size configurations.
     *
     * @return Map of size name to DataSizeConfig (unmodifiable)
     */
    public Map<String, DataSizeConfig> getAvailableSizes() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(dataSizeConfigs));
    }

    /**
     * Generates facilities with realistic data.
     * Delegates to FacilityGeneratorService.
     *
     * @param count Number of facilities to generate
     */
    @Transactional
    public void generateFacilities(int count) {
        facilityGeneratorService.generateFacilities(count, loadedFacilityTypes, loadedFacilityCapabilities);
    }

    /**
     * Generates doctors (FHIR Practitioner resources converted to Doctor entities).
     * Delegates to DoctorGeneratorService.
     */
    @Transactional
    public void generateDoctors(int count) {
        generateDoctors(count, null);
    }

    /**
     * Generates doctors (FHIR Practitioner resources converted to Doctor entities).
     * Delegates to DoctorGeneratorService.
     *
     * @param count    Number of doctors to generate
     * @param progress Optional progress tracker
     */
    @Transactional
    public void generateDoctors(int count, SyntheticDataGenerationProgress progress) {
        doctorGeneratorService.generateDoctors(count, progress, loadedMedicalSpecialties, loadedAvailabilityStatuses);
    }

    /**
     * Generates medical cases from FHIR Bundles.
     * Delegates to MedicalCaseGeneratorService.
     */
    @Transactional
    public void generateMedicalCases(int count) {
        generateMedicalCases(count, null);
    }

    /**
     * Generates medical cases from FHIR Bundles.
     * Delegates to MedicalCaseGeneratorService.
     *
     * @param count    Number of cases to generate
     * @param progress Optional progress tracker
     */
    @Transactional
    public void generateMedicalCases(int count, SyntheticDataGenerationProgress progress) {
        medicalCaseGeneratorService.generateMedicalCases(count, progress, loadedIcd10Codes, extendedIcd10Codes,
                loadedSymptoms, loadedSeverities, loadedEncounterClasses, loadedEncounterTypes);
    }

    /**
     * Generates FHIR R5 compliant bundles containing Patient, Condition, Observation, and Encounter.
     * Delegates to MedicalCaseGeneratorService.
     */
    public List<Bundle> generateFhirBundles(int count) {
        return medicalCaseGeneratorService.generateFhirBundles(count, loadedIcd10Codes, extendedIcd10Codes,
                loadedSymptoms, loadedSeverities, loadedEncounterClasses, loadedEncounterTypes);
    }

    /**
     * Generates clinical experiences linking doctors to cases.
     * Delegates to ClinicalExperienceGeneratorService.
     */
    @Transactional
    public void generateClinicalExperiences(int doctorCount, int caseCount) {
        generateClinicalExperiences(doctorCount, caseCount, null);
    }

    /**
     * Generates clinical experiences linking doctors to cases.
     * Delegates to ClinicalExperienceGeneratorService.
     *
     * @param doctorCount Number of doctors
     * @param caseCount   Number of cases
     * @param progress    Optional progress tracker
     */
    @Transactional
    public void generateClinicalExperiences(int doctorCount, int caseCount, SyntheticDataGenerationProgress progress) {
        clinicalExperienceGeneratorService.generateClinicalExperiences(doctorCount, caseCount, progress,
                loadedComplexityLevels, loadedOutcomes, loadedComplications, specialtyProceduresMap, extendedProcedures);
    }


    /**
     * Calculates target ICD-10 code count based on size.
     *
     * @param size Test data size: "tiny", "small", "medium", "large", "huge"
     * @return Target count of ICD-10 codes
     */
    private int calculateTargetIcd10Count(String size) {
        int baseCodeCount = loadedIcd10Codes.size();
        return switch (size.toLowerCase()) {
            case "tiny" -> Math.max(baseCodeCount, 4);
            case "small" -> Math.max(baseCodeCount, 50);
            case "medium" -> Math.max(baseCodeCount, 100);
            case "large" -> Math.max(baseCodeCount, 200);
            case "huge" -> Math.max(baseCodeCount, 500);
            default -> baseCodeCount;
        };
    }

    /**
     * Calculates target medical specialty count based on size.
     *
     * @param size Test data size (e.g., "tiny", "small", "medium", "large", "huge", "micro", "mini", etc.)
     * @return Target count of medical specialties
     */
    private int calculateTargetSpecialtyCount(String size) {
        int baseSpecialtyCount = loadedMedicalSpecialties.size();
        DataSizeConfig config = dataSizeConfigs.get(size.toLowerCase());
        if (config != null) {
            // Scale specialty count based on doctor count: ~1 specialty per 10 doctors, minimum 3
            int targetCount = Math.max(3, Math.min(config.doctorCount() / 10, baseSpecialtyCount));
            return Math.max(baseSpecialtyCount, targetCount);
        }
        // Fallback for unknown sizes
        return switch (size.toLowerCase()) {
            case "tiny" -> Math.max(baseSpecialtyCount, 3);
            case "small" -> Math.max(baseSpecialtyCount, 25);
            case "medium" -> Math.max(baseSpecialtyCount, 50);
            case "large" -> Math.max(baseSpecialtyCount, 100);
            case "huge" -> Math.max(baseSpecialtyCount, 200);
            default -> baseSpecialtyCount;
        };
    }

    /**
     * Calculates target procedure count based on size.
     *
     * @param size Test data size (e.g., "tiny", "small", "medium", "large", "huge", "micro", "mini", etc.)
     * @return Target count of procedures
     */
    private int calculateTargetProcedureCount(String size) {
        int baseProcedureCount = loadedProcedures.size();
        DataSizeConfig config = dataSizeConfigs.get(size.toLowerCase());
        if (config != null) {
            // Scale procedure count based on doctor count: ~1 procedure per 5 doctors, minimum 3
            int targetCount = Math.max(3, Math.min(config.doctorCount() / 5, baseProcedureCount));
            return Math.max(baseProcedureCount, targetCount);
        }
        // Fallback for unknown sizes
        return switch (size.toLowerCase()) {
            case "tiny" -> Math.max(baseProcedureCount, 3);
            case "small" -> Math.max(baseProcedureCount, 30);
            case "medium" -> Math.max(baseProcedureCount, 50);
            case "large" -> Math.max(baseProcedureCount, 100);
            case "huge" -> Math.max(baseProcedureCount, 200);
            default -> baseProcedureCount;
        };
    }

    /**
     * Generates ICD-10 codes from the base list.
     * Uses codes from loaded ICD-10 codes list and generates synthetic codes following ICD-10 patterns (no LLM calls).
     *
     * @param count Number of codes to generate
     * @return List of ICD-10 codes from base list and synthetic patterns
     */
    private List<String> generateIcd10CodesFromBaseList(int count) {
        List<String> result = new ArrayList<>();
        Set<String> allIcd10Codes = new HashSet<>(loadedIcd10Codes);

        // Use loaded ICD-10 codes (up to requested count)
        int codesToTake = Math.min(count, loadedIcd10Codes.size());
        for (int i = 0; i < codesToTake; i++) {
            result.add(loadedIcd10Codes.get(i));
        }

        // If still need more, generate synthetic ones following ICD-10 patterns
        if (result.size() < count) {
            int categoryIndex = 0;
            String[] categories = {"I", "C", "G", "J", "E", "K", "M", "N", "R", "S", "T", "Z"};
            int subcategory = 10;
            int detail = 0;

            while (result.size() < count && categoryIndex < categories.length) {
                String syntheticCode;
                if (detail < 10) {
                    syntheticCode = String.format("%s%d.%d", categories[categoryIndex], subcategory, detail);
                } else {
                    syntheticCode = String.format("%s%d.9", categories[categoryIndex], subcategory);
                }

                if (!allIcd10Codes.contains(syntheticCode)) {
                    result.add(syntheticCode);
                }

                detail++;
                if (detail > 9) {
                    detail = 0;
                    subcategory++;
                    if (subcategory > 99) {
                        subcategory = 10;
                        categoryIndex++;
                    }
                }

                // Safety limit
                if (result.size() >= count * 2) {
                    break;
                }
            }
        }

        return result.stream().limit(count).toList();
    }

    /**
     * Generates ICD-10 codes from the predefined list, extended with fallback-generated codes based on size.
     *
     * @param size Test data size: "tiny", "small", "medium", "large", "huge"
     */
    @Transactional
    public void generateIcd10Codes(String size) {
        generateIcd10Codes(size, null);
    }

    /**
     * Generates ICD-10 codes from the predefined list, extended with fallback-generated codes based on size.
     *
     * @param size     Test data size: "tiny", "small", "medium", "large", "huge"
     * @param progress Optional progress tracker for cancellation checks
     */
    @Transactional
    public void generateIcd10Codes(String size, SyntheticDataGenerationProgress progress) {
        // Input validation
        if (size == null || size.isBlank()) {
            throw new IllegalArgumentException("Size must not be null or blank");
        }
        String normalizedSize = size.toLowerCase();
        if (!dataSizeConfigs.containsKey(normalizedSize)) {
            String validSizes = String.join(", ", dataSizeConfigs.keySet());
            throw new IllegalArgumentException(
                    String.format("Invalid size: %s. Must be one of: %s", size, validSizes));
        }

        log.info("Generating ICD-10 codes for size: {}", size);

        // Check database first - if sufficient codes exist, skip LLM generation
        try {
            // Load existing codes once and reuse for both count check and loading
            List<ICD10Code> existingCodes = icd10CodeRepository.findAll();
            int existingCount = existingCodes.size();
            int targetCount = calculateTargetIcd10Count(size);

            if (existingCount >= targetCount) {
                log.info("Sufficient ICD-10 codes exist in database ({} >= {}), loading existing codes",
                        existingCount, targetCount);
                // Reuse existingCodes instead of calling findAll() again
                extendedIcd10Codes = existingCodes.stream()
                        .map(ICD10Code::code)
                        .distinct()
                        .limit(targetCount)
                        .collect(java.util.stream.Collectors.toList());
                log.info("Loaded {} existing ICD-10 codes from database", extendedIcd10Codes.size());
                return;
            } else {
                log.debug("Database has {} ICD-10 codes, target is {}, will generate {} additional",
                        existingCount, targetCount, targetCount - existingCount);
            }
        } catch (DataAccessException e) {
            log.warn("Database error checking for existing ICD-10 codes, proceeding with generation", e);
        } catch (RuntimeException e) {
            log.warn("Unexpected error checking database for existing ICD-10 codes, proceeding with generation", e);
        }

        // Extend ICD-10 codes list with fallback-generated codes based on size
        int baseCodeCount = loadedIcd10Codes.size();
        int targetCodeCount = calculateTargetIcd10Count(size);
        int additionalCount = targetCodeCount - baseCodeCount;

        List<String> extendedCodes = new ArrayList<>();
        if (additionalCount > 0) {
            // Check cache first
            if (cachedIcd10Codes.containsKey(size)) {
                extendedCodes = cachedIcd10Codes.get(size);
            } else {
                // Generate fallback codes
                extendedCodes = generateIcd10CodesFromBaseList(additionalCount);
                cachedIcd10Codes.put(size, new ArrayList<>(extendedCodes));
            }
        }

        Set<String> extendedIcd10CodesSet = new LinkedHashSet<>(loadedIcd10Codes);
        // Add extended codes, filtering out any duplicates
        for (String code : extendedCodes) {
            extendedIcd10CodesSet.add(code);
        }
        extendedIcd10Codes = new ArrayList<>(extendedIcd10CodesSet);

        log.info("Total ICD-10 codes to generate: {} ({} base + {} unique extended)",
                extendedIcd10Codes.size(), loadedIcd10Codes.size(),
                extendedIcd10Codes.size() - loadedIcd10Codes.size());

        // Load existing ICD-10 codes from database to get category, parent_code, and related_codes
        List<ICD10Code> existingIcd10Codes = icd10CodeRepository.findAll();
        Map<String, String> categoryMap = new HashMap<>();
        Map<String, String> parentCodeMap = new HashMap<>();
        Map<String, List<String>> relatedCodesMap = new HashMap<>();

        // Populate maps from existing ICD-10 codes in database (loaded from CSV)
        for (ICD10Code existingCode : existingIcd10Codes) {
            categoryMap.put(existingCode.code(), existingCode.category());
            parentCodeMap.put(existingCode.code(), existingCode.parentCode());
            relatedCodesMap.put(existingCode.code(), existingCode.relatedCodes() != null ? existingCode.relatedCodes() : List.of());
        }

        // Add category mappings for extended codes not in CSV
        for (String code : extendedIcd10Codes) {
            if (!categoryMap.containsKey(code)) {
                categoryMap.put(code, determineIcd10Category(code));
            }
        }

        // Generate parent codes for extended codes not in CSV
        for (String code : extendedIcd10Codes) {
            if (!parentCodeMap.containsKey(code)) {
                parentCodeMap.put(code, extractParentCode(code));
            }
        }

        // Generate related codes for extended codes not in CSV (same category)
        // Group codes by category first (O(n)) to avoid O(n²) nested loops
        Map<String, List<String>> codesByCategory = new HashMap<>();
        for (String code : extendedIcd10Codes) {
            String category = categoryMap.get(code);
            codesByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(code);
        }

        // Assign related codes for codes not already in CSV (O(n))
        for (String code : extendedIcd10Codes) {
            if (!relatedCodesMap.containsKey(code)) {
                String category = categoryMap.get(code);
                List<String> related = codesByCategory.getOrDefault(category, List.of())
                        .stream()
                        .filter(c -> !c.equals(code))
                        .limit(3)
                        .toList();
                relatedCodesMap.put(code, related);
            }
        }

        // Batch check existence first to get existing IDs
        List<String> codes = extendedIcd10Codes;
        Set<String> existingCodeStrings = icd10CodeRepository.findExistingCodes(codes);
        Map<String, ICD10Code> existingCodesMap = icd10CodeRepository.findByCodes(codes).stream()
                .collect(Collectors.toMap(ICD10Code::code, Function.identity()));

        // Build all ICD-10 code entities, reusing existing IDs for updates
        // Track processed codes to prevent duplicates within extendedIcd10Codes list
        Set<String> processedCodes = new LinkedHashSet<>();
        List<ICD10Code> icd10Codes = new ArrayList<>();
        for (String code : extendedIcd10Codes) {
            // Skip if already processed (duplicate prevention)
            if (processedCodes.contains(code)) {
                log.debug("Skipping duplicate ICD-10 code: {}", code);
                continue;
            }
            processedCodes.add(code);

            String id;
            ICD10Code existingCode = existingCodesMap.get(code);
            if (existingCode != null) {
                // Reuse existing ID for updates
                id = existingCode.id();
            } else {
                // Generate new ID for inserts
                id = IdGenerator.generateId();
            }

            String description = getIcd10Display(code);
            if (description == null || description.startsWith("Condition: ")) {
                // Generate description for extended codes
                description = generateIcd10Description(code, categoryMap.get(code));
            }
            String category = categoryMap.getOrDefault(code, "Other");
            String parentCode = parentCodeMap.getOrDefault(code, null);
            List<String> relatedCodes = relatedCodesMap.getOrDefault(code, List.of());

            ICD10Code icd10Code =
                    new ICD10Code(
                            id,
                            code,
                            description,
                            category,
                            parentCode,
                            relatedCodes
                    );

            icd10Codes.add(icd10Code);
        }

        // Separate into insert/update batches based on existing code strings
        List<ICD10Code> toInsert = new ArrayList<>();
        List<ICD10Code> toUpdate = new ArrayList<>();
        for (ICD10Code icd10Code : icd10Codes) {
            if (existingCodeStrings.contains(icd10Code.code())) {
                toUpdate.add(icd10Code);
            } else {
                toInsert.add(icd10Code);
            }
        }

        // Execute batch operations in chunks using batch size
        for (int i = 0; i < toInsert.size(); i += batchSize) {
            int end = Math.min(i + batchSize, toInsert.size());
            List<ICD10Code> batch = toInsert.subList(i, end);
            icd10CodeRepository.insertBatch(batch);
        }

        for (int i = 0; i < toUpdate.size(); i += batchSize) {
            int end = Math.min(i + batchSize, toUpdate.size());
            List<ICD10Code> batch = toUpdate.subList(i, end);
            icd10CodeRepository.updateBatch(batch);
        }

        log.info("Generated {} ICD-10 codes ({} base + {} extended, {} inserted, {} updated)",
                extendedIcd10Codes.size(), loadedIcd10Codes.size(),
                extendedIcd10Codes.size() - loadedIcd10Codes.size(), toInsert.size(), toUpdate.size());
    }

    /**
     * Generates medical specialties from the base list.
     * Uses specialties from loaded medical specialties list (reuses CSV data, no LLM calls).
     *
     * @param count Number of specialties to generate
     * @return List of medical specialty names from base list
     */
    private List<String> generateSpecialtiesFromBaseList(int count) {
        List<String> result = new ArrayList<>();
        int specialtiesToTake = Math.min(count, loadedMedicalSpecialties.size());
        for (int i = 0; i < specialtiesToTake; i++) {
            result.add(loadedMedicalSpecialties.get(i));
        }
        return result;
    }

    /**
     * Generates medical specialties from the predefined list, extended with fallback-generated specialties based on size.
     *
     * @param size Test data size: "tiny", "small", "medium", "large", "huge"
     */
    @Transactional
    public void generateMedicalSpecialties(String size) {
        generateMedicalSpecialties(size, null);
    }

    /**
     * Generates medical specialties from the predefined list, extended with fallback-generated specialties based on size.
     *
     * @param size     Test data size: "tiny", "small", "medium", "large", "huge"
     * @param progress Optional progress tracker for cancellation checks
     */
    @Transactional
    public void generateMedicalSpecialties(String size, SyntheticDataGenerationProgress progress) {
        // Input validation
        if (size == null || size.isBlank()) {
            throw new IllegalArgumentException("Size must not be null or blank");
        }
        String normalizedSize = size.toLowerCase();
        if (!dataSizeConfigs.containsKey(normalizedSize)) {
            String validSizes = String.join(", ", dataSizeConfigs.keySet());
            throw new IllegalArgumentException(
                    String.format("Invalid size: %s. Must be one of: %s", size, validSizes));
        }

        log.info("Generating medical specialties for size: {}", size);

        // Check database first - if sufficient specialties exist, skip LLM generation
        try {
            int existingCount = medicalSpecialtyRepository.findAll().size();
            int targetCount = calculateTargetSpecialtyCount(size);

            if (existingCount >= targetCount) {
                log.info("Sufficient medical specialties exist in database ({} >= {}), skipping generation",
                        existingCount, targetCount);
                // Note: We still need to populate extendedSpecialties list for use in generation
                // But we don't need to call LLM or generate new ones
                return;
            } else {
                log.debug("Database has {} medical specialties, target is {}, will generate {} additional",
                        existingCount, targetCount, targetCount - existingCount);
            }
        } catch (DataAccessException e) {
            log.warn("Database error checking for existing medical specialties, proceeding with generation", e);
        } catch (RuntimeException e) {
            log.warn("Unexpected error checking database for existing medical specialties, proceeding with generation", e);
        }

        // Extend specialties list with fallback-generated specialties based on size
        int baseSpecialtyCount = loadedMedicalSpecialties.size();
        int targetSpecialtyCount = calculateTargetSpecialtyCount(size);
        int additionalCount = targetSpecialtyCount - baseSpecialtyCount;

        List<String> extendedSpecialtiesList = new ArrayList<>();
        if (additionalCount > 0) {
            // Check cache first
            if (cachedSpecialties.containsKey(size)) {
                extendedSpecialtiesList = cachedSpecialties.get(size);
            } else {
                // Generate fallback specialties
                extendedSpecialtiesList = generateSpecialtiesFromBaseList(additionalCount);
                cachedSpecialties.put(size, new ArrayList<>(extendedSpecialtiesList));
            }
        }

        // Load existing medical specialties from database to get ICD-10 ranges, descriptions, and related specialties
        List<MedicalSpecialty> existingSpecialties = medicalSpecialtyRepository.findAll();
        Map<String, List<String>> specialtyIcd10Ranges = new HashMap<>();
        Map<String, String> specialtyDescriptions = new HashMap<>();
        Map<String, List<String>> relatedSpecialtiesMap = new HashMap<>();

        // Populate maps from existing medical specialties in database (loaded from CSV)
        for (MedicalSpecialty existingSpecialty : existingSpecialties) {
            specialtyIcd10Ranges.put(existingSpecialty.name(), existingSpecialty.icd10CodeRanges());
            specialtyDescriptions.put(existingSpecialty.name(), existingSpecialty.description());

            // Resolve related specialty IDs to names
            List<String> relatedSpecialtyNames = new ArrayList<>();
            for (String relatedId : existingSpecialty.relatedSpecialtyIds()) {
                existingSpecialties.stream()
                        .filter(s -> s.id().equals(relatedId))
                        .findFirst()
                        .map(MedicalSpecialty::name)
                        .ifPresent(relatedSpecialtyNames::add);
            }
            relatedSpecialtiesMap.put(existingSpecialty.name(), relatedSpecialtyNames);
        }

        // Extend specialties list with fallback-generated specialties based on size
        Set<String> extendedSpecialtiesSet = new LinkedHashSet<>(loadedMedicalSpecialties);
        for (String specialty : extendedSpecialtiesList) {
            extendedSpecialtiesSet.add(specialty);
        }
        List<String> extendedSpecialties = new ArrayList<>(extendedSpecialtiesSet);

        log.info("Total specialties to generate: {} ({} base + {} unique extended)",
                extendedSpecialties.size(), loadedMedicalSpecialties.size(),
                extendedSpecialties.size() - loadedMedicalSpecialties.size());

        Map<String, String> specialtyIdMap = new HashMap<>();

        // First pass: create all specialties and collect IDs
        // Use LinkedHashSet to preserve order while ensuring uniqueness
        Set<String> processedSpecialties = new LinkedHashSet<>();
        for (String specialtyName : extendedSpecialties) {
            // Skip if already processed (duplicate prevention)
            if (processedSpecialties.contains(specialtyName)) {
                log.debug("Skipping duplicate specialty: {}", specialtyName);
                continue;
            }
            processedSpecialties.add(specialtyName);
            String id = IdGenerator.generateId();
            specialtyIdMap.put(specialtyName, id);

            String normalizedName = normalizeSpecialtyName(specialtyName);
            String description = specialtyDescriptions.getOrDefault(specialtyName,
                    generateSpecialtyDescription(specialtyName));
            List<String> icd10CodeRanges = specialtyIcd10Ranges.getOrDefault(specialtyName,
                    generateDefaultIcd10Ranges(specialtyName));

            // Resolve related specialty IDs from CSV data
            List<String> relatedSpecialtyIds = new ArrayList<>();
            List<String> relatedSpecialtyNames = relatedSpecialtiesMap.getOrDefault(specialtyName, List.of());
            for (String relatedName : relatedSpecialtyNames) {
                String relatedId = specialtyIdMap.get(relatedName);
                if (relatedId != null) {
                    relatedSpecialtyIds.add(relatedId);
                }
            }

            MedicalSpecialty specialty =
                    new MedicalSpecialty(
                            id,
                            specialtyName,
                            normalizedName,
                            description,
                            icd10CodeRanges,
                            relatedSpecialtyIds
                    );

            if (medicalSpecialtyRepository.findByName(specialtyName).isPresent()) {
                medicalSpecialtyRepository.update(specialty);
            } else {
                medicalSpecialtyRepository.insert(specialty);
            }
        }

        // Second pass: update with related specialty IDs
        // Use processedSpecialties to avoid processing duplicates
        for (String specialtyName : processedSpecialties) {
            String specialtyId = specialtyIdMap.get(specialtyName);
            if (specialtyId == null) {
                log.warn("Specialty ID not found for: {}, skipping related specialty update", specialtyName);
                continue;
            }
            List<String> relatedSpecialtyNames = relatedSpecialtiesMap.getOrDefault(specialtyName, List.of());
            List<String> relatedSpecialtyIds = relatedSpecialtyNames.stream()
                    .map(name -> specialtyIdMap.get(name))
                    .filter(id -> id != null)
                    .toList();

            Optional<MedicalSpecialty> existing =
                    medicalSpecialtyRepository.findById(specialtyId);
            if (existing.isPresent()) {
                MedicalSpecialty specialty = existing.get();
                MedicalSpecialty updated =
                        new MedicalSpecialty(
                                specialty.id(),
                                specialty.name(),
                                specialty.normalizedName(),
                                specialty.description(),
                                specialty.icd10CodeRanges(),
                                relatedSpecialtyIds
                        );
                medicalSpecialtyRepository.update(updated);
            }
        }

        log.info("Generated {} medical specialties ({} base + {} extended)",
                extendedSpecialties.size(), loadedMedicalSpecialties.size(), extendedSpecialtiesList.size());
    }

    /**
     * Normalizes specialty name for matching (lowercase, remove special chars).
     */
    private String normalizeSpecialtyName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }

    /**
     * Generates a default description for a medical specialty.
     *
     * @param specialtyName The name of the specialty
     * @return A default description
     */
    private String generateSpecialtyDescription(String specialtyName) {
        // Generate a simple description based on specialty name
        if (specialtyName.toLowerCase().contains("surgery")) {
            return "Surgical specialty focusing on " + specialtyName.toLowerCase().replace("surgery", "").trim();
        } else if (specialtyName.toLowerCase().contains("pediatric")) {
            return "Pediatric specialty for " + specialtyName.toLowerCase().replace("pediatric", "").trim();
        } else if (specialtyName.toLowerCase().contains("medicine")) {
            return "Medical specialty: " + specialtyName;
        } else {
            return "Medical specialty focusing on " + specialtyName.toLowerCase();
        }
    }

    /**
     * Determines ICD-10 category for a given code based on the code prefix.
     *
     * @param code ICD-10 code (e.g., "I21.9", "C50.9")
     * @return Category name
     */
    private String determineIcd10Category(String code) {
        if (code == null || code.isEmpty()) {
            return "Other";
        }

        char prefix = code.charAt(0);
        return switch (prefix) {
            case 'I' -> "Diseases of the circulatory system";
            case 'C' -> "Neoplasms";
            case 'G' -> "Diseases of the nervous system";
            case 'J' -> "Diseases of the respiratory system";
            case 'E' -> "Endocrine, nutritional and metabolic diseases";
            case 'M' -> "Diseases of the musculoskeletal system";
            case 'R' -> "Symptoms, signs and abnormal clinical and laboratory findings";
            case 'K' -> "Diseases of the digestive system";
            case 'N' -> "Diseases of the genitourinary system";
            case 'H' -> "Diseases of the eye and adnexa / Diseases of the ear and mastoid process";
            case 'L' -> "Diseases of the skin and subcutaneous tissue";
            case 'S', 'T' -> "Injury, poisoning and certain other consequences of external causes";
            case 'Z' -> "Factors influencing health status and contact with health services";
            case 'F' -> "Mental, behavioural and neurodevelopmental disorders";
            case 'D' ->
                    "Diseases of the blood and blood-forming organs and certain disorders involving the immune mechanism";
            case 'O' -> "Pregnancy, childbirth and the puerperium";
            case 'P' -> "Certain conditions originating in the perinatal period";
            case 'Q' -> "Congenital malformations, deformations and chromosomal abnormalities";
            case 'U' -> "Codes for special purposes";
            default -> "Other";
        };
    }

    /**
     * Extracts parent code from an ICD-10 code.
     * For example, "I21.9" -> "I21", "E11.9" -> "E11"
     *
     * @param code ICD-10 code
     * @return Parent code or null if code is already a parent
     */
    private String extractParentCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }

        int dotIndex = code.indexOf('.');
        if (dotIndex > 0) {
            return code.substring(0, dotIndex);
        }

        // If code has 3+ characters and no dot, it might be a parent code
        if (code.length() >= 3) {
            return null; // Already a parent code
        }

        return null;
    }

    /**
     * Generates default ICD-10 code ranges for a medical specialty based on common patterns.
     *
     * @param specialtyName The name of the specialty
     * @return List of default ICD-10 code ranges
     */
    private List<String> generateDefaultIcd10Ranges(String specialtyName) {
        String lowerName = specialtyName.toLowerCase();

        // Map specialty keywords to ICD-10 ranges
        if (lowerName.contains("cardiac") || lowerName.contains("cardio")) {
            return List.of("I00-I99");
        } else if (lowerName.contains("oncology") || lowerName.contains("cancer")) {
            return List.of("C00-D49");
        } else if (lowerName.contains("neuro")) {
            return List.of("G00-G99");
        } else if (lowerName.contains("pulmon") || lowerName.contains("respiratory")) {
            return List.of("J00-J99");
        } else if (lowerName.contains("gastro") || lowerName.contains("digestive")) {
            return List.of("K00-K95");
        } else if (lowerName.contains("rheumat") || lowerName.contains("orthopedic") || lowerName.contains("musculoskeletal")) {
            return List.of("M00-M99");
        } else if (lowerName.contains("endocrine") || lowerName.contains("diabetes") || lowerName.contains("metabolic")) {
            return List.of("E00-E90");
        } else if (lowerName.contains("psychiatry") || lowerName.contains("mental")) {
            return List.of("F01-F99");
        } else if (lowerName.contains("pediatric") || lowerName.contains("pediatric")) {
            return List.of("E00-E90", "J00-J99");
        } else if (lowerName.contains("surgery")) {
            return List.of("K00-K95", "M00-M99");
        } else if (lowerName.contains("emergency")) {
            return List.of("R00-R99", "I00-I99");
        } else if (lowerName.contains("dermatology") || lowerName.contains("skin")) {
            return List.of("L00-L99");
        } else if (lowerName.contains("urology") || lowerName.contains("genitourinary")) {
            return List.of("N00-N99");
        } else if (lowerName.contains("ophthalmology") || lowerName.contains("eye")) {
            return List.of("H00-H59");
        } else if (lowerName.contains("otolaryngology") || lowerName.contains("ent") || lowerName.contains("ear")) {
            return List.of("H60-H95");
        } else {
            // Default to general medicine ranges
            return List.of("E00-E90", "I00-I99", "R00-R99");
        }
    }

    /**
     * Calculates facility count based on doctor count.
     */
    private int calculateFacilityCount(int doctorCount) {
        // Generate approximately 1 facility per 10-20 doctors
        return Math.max(5, doctorCount / 15);
    }


    /**
     * Clears all test data including graph objects.
     */
    @Transactional
    public void clearTestData() {
        log.info("Clearing all test data");

        // Clear graph objects first (before deleting database records)
        log.info("Attempting to clear graph objects...");
        try {
            graphBuilderService.clearGraph();
            log.info("Graph objects cleared successfully");
        } catch (DataAccessException e) {
            log.error("Database error clearing graph objects: {}", e.getMessage(), e);
            // Continue with database cleanup even if graph clearing fails
        } catch (RuntimeException e) {
            log.error("Unexpected error clearing graph objects: {}", e.getMessage(), e);
            // Continue with database cleanup even if graph clearing fails
        }

        // Clear in dependency order
        clinicalExperienceRepository.deleteAll();
        medicalCaseRepository.deleteAll();
        doctorRepository.deleteAll();
        facilityRepository.deleteAll();
        medicalSpecialtyRepository.deleteAll();
        icd10CodeRepository.deleteAll();
        procedureRepository.deleteAll();

        // Clear result caches
        cachedIcd10Codes.clear();
        cachedSpecialties.clear();
        cachedProcedures.clear();
        log.debug("Cleared result caches");

        log.info("Synthetic data cleared");
    }

    /**
     * Generates descriptions for all medical cases that don't have descriptions.
     * Processes cases sequentially (LLM doesn't support parallel calls) but commits
     * incrementally in batches to preserve progress.
     *
     * @param progress Optional progress tracker
     */
    private void generateMedicalCaseDescriptions(SyntheticDataGenerationProgress progress) {
        List<com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase> cases = medicalCaseRepository.findWithoutDescriptions();

        int totalRecords = cases.size();
        log.info("Starting description generation for {} medical cases (batch commit size: {})",
                totalRecords, descriptionBatchCommitSize);

        if (totalRecords == 0) {
            log.info("No cases without descriptions found");
            return;
        }

        int processedCount = 0;
        int successCount = 0;
        int failedCount = 0;
        long startTime = System.currentTimeMillis();
        List<CaseDescriptionUpdate> batch = new ArrayList<>();

        for (com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase medicalCase : cases) {
            if (progress != null && progress.isCancelled()) {
                log.info("Generation cancelled during description generation");
                break;
            }

            try {
                String description = medicalCaseDescriptionService.generateDescription(medicalCase);
                if (description != null && !description.isBlank()) {
                    batch.add(new CaseDescriptionUpdate(medicalCase.id(), description));
                    successCount++;
                } else {
                    log.warn("Generated empty description for case: {}", medicalCase.id());
                    failedCount++;
                }
            } catch (Exception e) {
                log.error("Error generating description for case: {}", medicalCase.id(), e);
                failedCount++;
            }

            processedCount++;

            // Commit batch when it reaches the configured size
            if (batch.size() >= descriptionBatchCommitSize) {
                commitDescriptionBatch(batch);
                batch.clear();
                log.info("Committed batch of {} descriptions (processed: {}/{})",
                        descriptionBatchCommitSize, processedCount, totalRecords);
            }

            // Update progress
            if (progress != null && (processedCount % 10 == 0 || processedCount == totalRecords)) {
                int progressPercent = totalRecords > 0 ? (processedCount * 100 / totalRecords) : 0;
                int descriptionProgress = 55 + (progressPercent * 10 / 100);
                progress.updateProgress(descriptionProgress, "Descriptions",
                        String.format("Generating descriptions: %d/%d (%d%%)", processedCount, totalRecords, progressPercent));
            }
        }

        // Commit remaining batch
        if (!batch.isEmpty()) {
            commitDescriptionBatch(batch);
            log.info("Committed final batch of {} descriptions", batch.size());
        }

        long endTime = System.currentTimeMillis();
        long totalElapsedTime = endTime - startTime;
        double totalItemsPerSecond = totalElapsedTime > 0 ? (processedCount * 1000.0) / totalElapsedTime : 0;

        log.info(String.format("Description generation completed. Total: %d, Success: %d, Failed: %d, " +
                        "Total time: %.3fs, Overall rate: %.2f items/sec",
                processedCount, successCount, failedCount,
                totalElapsedTime / 1000.0, totalItemsPerSecond));
    }

    /**
     * Commits a batch of description updates to the database in a single transaction.
     * Must be public for @Transactional to work (Spring AOP doesn't intercept private methods).
     *
     * @param batch List of case description updates to commit
     */
    @Transactional
    public void commitDescriptionBatch(List<CaseDescriptionUpdate> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }

        int committedCount = 0;
        for (CaseDescriptionUpdate update : batch) {
            try {
                medicalCaseRepository.updateAbstract(update.caseId(), update.description());
                committedCount++;
            } catch (Exception e) {
                log.error("Error committing description for case: {}", update.caseId(), e);
            }
        }

        log.info("Committed {} description updates to database (batch size: {})", committedCount, batch.size());
    }

    /**
     * Generates embeddings for all medical cases that don't have embeddings.
     */
    public void generateEmbeddings() {
        generateEmbeddings(null);
    }

    /**
     * Generates embeddings for all medical cases that don't have embeddings.
     * Delegates to EmbeddingGeneratorService.
     *
     * @param progress Optional progress tracker
     */
    public void generateEmbeddings(SyntheticDataGenerationProgress progress) {
        embeddingGeneratorService.generateEmbeddings(progress);
    }

    /**
     * Builds graph relationships from generated data using MedicalGraphBuilderService.
     */
    public void buildGraph() {
        log.info("Building Apache AGE graph from generated data...");
        try {
            graphBuilderService.buildGraph();
            log.info("Graph building completed successfully");
        } catch (DataAccessException e) {
            log.error("Database error building graph: {}", e.getMessage(), e);
            // Don't throw - graph building is optional for data generation
        } catch (RuntimeException e) {
            log.error("Unexpected error building graph: {}", e.getMessage(), e);
            // Don't throw - graph building is optional for data generation
        }
    }

    /**
     * Gets display text for ICD-10 code from CSV data.
     */
    private String getIcd10Display(String code) {
        return icd10CodeDisplays.get(code);
    }

    /**
     * Extends procedures list using fallback method based on test database size.
     * Generates additional medical procedures using fallback method (reuses CSV data) to scale with dataset size.
     *
     * @param size Test data size: "tiny", "small", "medium", "large", "huge"
     */
    private void generateAdditionalProcedures(String size, SyntheticDataGenerationProgress progress) {
        // Check cache first
        if (cachedProcedures.containsKey(size)) {
            log.debug("Using cached procedures for size: {}", size);
            Set<String> extendedProceduresSet = new LinkedHashSet<>(loadedProcedures);
            // Add cached procedures, filtering out any duplicates
            for (String procedure : cachedProcedures.get(size)) {
                extendedProceduresSet.add(procedure);
            }
            extendedProcedures = new ArrayList<>(extendedProceduresSet);
            log.info("Total procedures available: {} ({} base + {} unique cached)",
                    extendedProcedures.size(), loadedProcedures.size(),
                    extendedProcedures.size() - loadedProcedures.size());
            return;
        }

        // Check database first - if sufficient procedures exist, skip LLM generation
        try {
            // Load existing procedures once and reuse for both count check and loading
            List<Procedure> existingProcedures = procedureRepository.findAll();
            int existingCount = existingProcedures.size();
            int targetCount = calculateTargetProcedureCount(size);

            if (existingCount >= targetCount) {
                log.info("Sufficient procedures exist in database ({} >= {}), loading existing procedures",
                        existingCount, targetCount);
                // Reuse existingProcedures instead of calling findAll() again
                List<String> procedureNames = existingProcedures.stream()
                        .map(Procedure::name)
                        .distinct()
                        .limit(targetCount)
                        .collect(java.util.stream.Collectors.toList());

                // Cache the loaded procedures
                int baseProcedureCount = loadedProcedures.size();
                List<String> additionalProcedures = procedureNames.stream()
                        .filter(proc -> !loadedProcedures.contains(proc))
                        .limit(targetCount - baseProcedureCount)
                        .collect(java.util.stream.Collectors.toList());
                cachedProcedures.put(size, new ArrayList<>(additionalProcedures));

                Set<String> extendedProceduresSet = new LinkedHashSet<>(loadedProcedures);
                // Add database procedures, filtering out any duplicates
                for (String procedure : additionalProcedures) {
                    extendedProceduresSet.add(procedure);
                }
                extendedProcedures = new ArrayList<>(extendedProceduresSet);
                log.info("Loaded {} existing procedures from database ({} base + {} unique from DB)",
                        extendedProcedures.size(), loadedProcedures.size(),
                        extendedProcedures.size() - loadedProcedures.size());
                return;
            }
        } catch (DataAccessException e) {
            log.warn("Database error checking for existing procedures, proceeding with generation", e);
        } catch (RuntimeException e) {
            log.warn("Unexpected error checking database for existing procedures, proceeding with generation", e);
        }

        // Check hierarchical reuse: if smaller size, try to use subset from "huge"
        if (!size.equals("huge") && cachedProcedures.containsKey("huge")) {
            log.debug("Using subset of cached 'huge' procedures for size: {}", size);
            List<String> hugeProcedures = cachedProcedures.get("huge");
            int targetCount = calculateTargetProcedureCount(size);
            int baseProcedureCount = loadedProcedures.size();
            int neededCount = targetCount - baseProcedureCount;
            if (neededCount > 0 && hugeProcedures.size() >= neededCount) {
                List<String> subset = hugeProcedures.stream().limit(neededCount).toList();
                // Cache the subset for this size as well
                cachedProcedures.put(size, new ArrayList<>(subset));
                Set<String> extendedProceduresSet = new LinkedHashSet<>(loadedProcedures);
                // Add subset procedures, filtering out any duplicates
                for (String procedure : subset) {
                    extendedProceduresSet.add(procedure);
                }
                extendedProcedures = new ArrayList<>(extendedProceduresSet);
                log.info("Total procedures available: {} ({} base + {} unique from cache)",
                        extendedProcedures.size(), loadedProcedures.size(),
                        extendedProcedures.size() - loadedProcedures.size());
                return;
            }
        }

        int baseProcedureCount = loadedProcedures.size();
        int targetProcedureCount = calculateTargetProcedureCount(size);
        int additionalCount = targetProcedureCount - baseProcedureCount;

        if (additionalCount <= 0) {
            log.debug("No additional procedures needed for size: {}", size);
            extendedProcedures = new ArrayList<>(loadedProcedures);
            return;
        }

        log.info("Generating {} additional procedures using fallback method for size: {}", additionalCount, size);

        List<String> extendedProceduresList = generateProceduresFromBaseList(additionalCount);

        // Cache the result
        cachedProcedures.put(size, new ArrayList<>(extendedProceduresList));

        // Persist extended procedures to database
        persistProceduresToDatabase(extendedProceduresList);

        // Combine base and extended procedures, filtering duplicates
        Set<String> extendedProceduresSet = new LinkedHashSet<>(loadedProcedures);
        // Add extended procedures, filtering out any duplicates
        for (String procedure : extendedProceduresList) {
            extendedProceduresSet.add(procedure);
        }
        extendedProcedures = new ArrayList<>(extendedProceduresSet);

        log.info("Total procedures available: {} ({} base + {} unique extended)",
                extendedProcedures.size(), loadedProcedures.size(),
                extendedProcedures.size() - loadedProcedures.size());
    }

    /**
     * Generates medical procedures from the base list (reuses loaded CSV data, no LLM calls).
     * Uses procedures from loaded procedures list.
     *
     * @param count Number of procedures to generate
     * @return List of procedure names from base list
     */
    private List<String> generateProceduresFromBaseList(int count) {
        List<String> result = new ArrayList<>();
        int proceduresToTake = Math.min(count, loadedProcedures.size());
        for (int i = 0; i < proceduresToTake; i++) {
            result.add(loadedProcedures.get(i));
        }
        return result;
    }

    /**
     * Generates a description for an ICD-10 code based on the code pattern and category.
     *
     * @param code     ICD-10 code
     * @param category Category name
     * @return Generated description
     */
    private String generateIcd10Description(String code, String category) {
        if (code == null || code.isEmpty()) {
            return "Condition: " + code;
        }

        // Extract code parts
        String prefix = code.substring(0, 1);
        boolean hasSubcode = code.contains(".");

        // Generate description based on category and code pattern
        String baseDescription = switch (prefix) {
            case "I" -> "Disease of the circulatory system";
            case "C" -> "Malignant neoplasm";
            case "G" -> "Disease of the nervous system";
            case "J" -> "Disease of the respiratory system";
            case "E" -> "Endocrine, nutritional or metabolic disease";
            case "M" -> "Disease of the musculoskeletal system";
            case "R" -> "Symptom, sign or abnormal clinical finding";
            case "K" -> "Disease of the digestive system";
            case "N" -> "Disease of the genitourinary system";
            case "H" -> "Disease of the eye or ear";
            case "L" -> "Disease of the skin";
            case "S", "T" -> "Injury or poisoning";
            case "Z" -> "Factor influencing health status";
            case "F" -> "Mental or behavioural disorder";
            default -> "Medical condition";
        };

        if (hasSubcode) {
            return baseDescription + ", unspecified";
        } else {
            return baseDescription;
        }
    }

    /**
     * Gets display text for encounter class code from CSV data.
     */
    private String getEncounterClassDisplay(String code) {
        return encounterClassDisplays.getOrDefault(code, code);
    }

    /**
     * Normalizes procedure name for matching (lowercase, remove special chars).
     */
    private String normalizeProcedureName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }

    /**
     * Persists extended procedures to the database.
     * Must be public for @Transactional to work (Spring AOP doesn't intercept private methods).
     *
     * @param procedureNames List of procedure names to persist
     */
    @Transactional
    public void persistProceduresToDatabase(List<String> procedureNames) {
        if (procedureNames == null || procedureNames.isEmpty()) {
            return;
        }

        // Remove duplicates from procedure names list before processing
        Set<String> uniqueProcedureNames = new LinkedHashSet<>(procedureNames);
        if (uniqueProcedureNames.size() < procedureNames.size()) {
            log.debug("Filtered {} duplicate procedures before persistence",
                    procedureNames.size() - uniqueProcedureNames.size());
        }

        log.debug("Persisting {} unique procedures to database", uniqueProcedureNames.size());
        int persistedCount = 0;
        int skippedCount = 0;

        for (String procedureName : uniqueProcedureNames) {
            if (procedureName == null || procedureName.isBlank()) {
                continue;
            }

            try {
                // Check if procedure already exists
                String normalizedName = normalizeProcedureName(procedureName);
                Optional<Procedure> existing = procedureRepository.findByNormalizedName(normalizedName);

                if (existing.isPresent()) {
                    skippedCount++;
                    continue;
                }

                // Create new procedure entity
                String id = IdGenerator.generateId();
                Procedure procedure = new Procedure(
                        id,
                        procedureName,
                        normalizedName,
                        null, // Description can be added later if needed
                        null  // Category can be added later if needed
                );

                procedureRepository.insert(procedure);
                persistedCount++;
            } catch (DataAccessException e) {
                log.warn("Database error persisting procedure '{}' to database", procedureName, e);
            } catch (RuntimeException e) {
                log.warn("Unexpected error persisting procedure '{}' to database", procedureName, e);
            }
        }

        log.info("Persisted {} procedures to database ({} skipped as duplicates)", persistedCount, skippedCount);
    }

    /**
     * Configuration for data size presets.
     */
    public record DataSizeConfig(
            String size,
            int doctorCount,
            int caseCount,
            String description,
            int estimatedTimeMinutes
    ) {
    }
}
