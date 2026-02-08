package com.berdachuk.medexpertmatch.ingestion.service.impl;

import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.ingestion.adapter.FhirBundleAdapter;
import com.berdachuk.medexpertmatch.ingestion.exception.SyntheticDataGenerationException;
import com.berdachuk.medexpertmatch.ingestion.service.MedicalCaseGeneratorService;
import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataGenerationProgress;
import com.berdachuk.medexpertmatch.ingestion.util.CsvDataLoader;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.medicalcoding.repository.ICD10CodeRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r5.model.*;
import org.hl7.fhir.r5.model.Enumerations.EncounterStatus;
import org.hl7.fhir.r5.model.Enumerations.ObservationStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service implementation for generating medical cases from FHIR bundles.
 */
@Slf4j
@Service
public class MedicalCaseGeneratorServiceImpl implements MedicalCaseGeneratorService {

    private static final int MAX_CASES_PER_BATCH = 1000000;
    private static final int MIN_OBSERVATIONS_PER_CASE = 1;
    private static final int MAX_OBSERVATIONS_PER_CASE = 3;
    private static final int CASE_GENERATION_PROGRESS_START = 40;
    private static final int CASE_GENERATION_PROGRESS_END = 65;
    private static final int CASE_GENERATION_PROGRESS_RANGE = CASE_GENERATION_PROGRESS_END - CASE_GENERATION_PROGRESS_START;
    private static final int CASE_GENERATION_UPDATE_DIVISOR = 10;
    private static final String FHIR_ICD10_SYSTEM = "http://hl7.org/fhir/sid/icd-10";
    private final MedicalCaseRepository medicalCaseRepository;
    private final FhirBundleAdapter fhirBundleAdapter;
    private final ICD10CodeRepository icd10CodeRepository;
    private final ResourceLoader resourceLoader;
    private final Random random = new Random();
    private final Counter casesGeneratedCounter;
    // Encounter class displays loaded from CSV
    private final Map<String, String> encounterClassDisplays = new HashMap<>();
    @Value("${medexpertmatch.synthetic-data.data-files.encounter-classes:classpath:/data/encounter-classes.csv}")
    private String encounterClassesFile;

    public MedicalCaseGeneratorServiceImpl(
            MedicalCaseRepository medicalCaseRepository,
            FhirBundleAdapter fhirBundleAdapter,
            ICD10CodeRepository icd10CodeRepository,
            ResourceLoader resourceLoader,
            MeterRegistry meterRegistry) {
        this.medicalCaseRepository = medicalCaseRepository;
        this.fhirBundleAdapter = fhirBundleAdapter;
        this.icd10CodeRepository = icd10CodeRepository;
        this.resourceLoader = resourceLoader;
        this.casesGeneratedCounter = Counter.builder("synthetic.data.cases.generated")
                .description("Total number of medical cases generated")
                .register(meterRegistry);
    }

    /**
     * Loads encounter class displays from CSV file.
     */
    @PostConstruct
    public void loadEncounterClassDisplays() {
        try {
            List<Map<String, String>> encounterClassesData = CsvDataLoader.loadCsv(resourceLoader, encounterClassesFile, "encounter classes");
            for (Map<String, String> row : encounterClassesData) {
                String code = row.get("code");
                String display = row.get("display");
                if (code != null && !code.isEmpty() && display != null && !display.isEmpty()) {
                    encounterClassDisplays.put(code, display);
                }
            }
            log.info("Loaded {} encounter class displays from {}", encounterClassDisplays.size(), encounterClassesFile);
        } catch (Exception e) {
            throw new SyntheticDataGenerationException(
                    "Failed to load encounter class displays from " + encounterClassesFile + ". Ensure the file exists and is valid.", e);
        }
    }

    @Override
    public List<Bundle> generateFhirBundles(int count,
                                            List<String> icd10Codes, List<String> extendedIcd10Codes,
                                            List<String> symptoms, List<String> severities,
                                            List<String> encounterClasses, List<String> encounterTypes) {
        log.info("Generating {} FHIR R5 bundles", count);

        List<String> loadedIcd10Codes = icd10Codes != null && !icd10Codes.isEmpty() ? icd10Codes : List.of("I21.9");
        List<String> loadedExtendedIcd10Codes = extendedIcd10Codes != null && !extendedIcd10Codes.isEmpty()
                ? extendedIcd10Codes : loadedIcd10Codes;
        List<String> loadedSymptoms = symptoms != null && !symptoms.isEmpty() ? symptoms : List.of("Symptom");
        List<String> loadedSeverities = severities != null ? severities : List.of();
        List<String> loadedEncounterClasses = encounterClasses != null && !encounterClasses.isEmpty()
                ? encounterClasses : List.of("AMB");
        List<String> loadedEncounterTypes = encounterTypes != null && !encounterTypes.isEmpty()
                ? encounterTypes : List.of("Outpatient Visit");

        List<Bundle> bundles = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Bundle bundle = new Bundle();
            bundle.setType(Bundle.BundleType.COLLECTION);
            bundle.setId(IdGenerator.generateId());

            Patient patient = createAnonymizedPatient();
            bundle.addEntry().setResource(patient);

            Condition condition = createCondition(patient.getId(), loadedIcd10Codes, loadedExtendedIcd10Codes, loadedSeverities);
            bundle.addEntry().setResource(condition);

            int observationCount = random.nextInt(MAX_OBSERVATIONS_PER_CASE - MIN_OBSERVATIONS_PER_CASE + 1) + MIN_OBSERVATIONS_PER_CASE;
            for (int j = 0; j < observationCount; j++) {
                Observation observation = createObservation(patient.getId(), loadedSymptoms);
                bundle.addEntry().setResource(observation);
            }

            Encounter encounter = createEncounter(patient.getId(), loadedEncounterClasses, loadedEncounterTypes);
            bundle.addEntry().setResource(encounter);

            bundles.add(bundle);
        }

        return bundles;
    }

    @Override
    @Transactional
    public void generateMedicalCases(int count, SyntheticDataGenerationProgress progress,
                                     List<String> icd10Codes, List<String> extendedIcd10Codes,
                                     List<String> symptoms, List<String> severities,
                                     List<String> encounterClasses, List<String> encounterTypes) {
        if (count < 0) {
            throw new IllegalArgumentException("Count must be non-negative, got: " + count);
        }
        if (count > MAX_CASES_PER_BATCH) {
            throw new IllegalArgumentException(
                    String.format("Count exceeds maximum: %d (max: %d)", count, MAX_CASES_PER_BATCH));
        }

        log.info("Generating {} medical cases", count);

        List<Bundle> bundles = generateFhirBundles(count, icd10Codes, extendedIcd10Codes,
                symptoms, severities, encounterClasses, encounterTypes);

        List<MedicalCase> medicalCases = new ArrayList<>();
        for (int i = 0; i < bundles.size(); i++) {
            if (progress != null && bundles.size() > 0 && (i + 1) % Math.max(1, bundles.size() / CASE_GENERATION_UPDATE_DIVISOR) == 0) {
                int caseProgress = CASE_GENERATION_PROGRESS_START + ((i + 1) * CASE_GENERATION_PROGRESS_RANGE / bundles.size());
                progress.updateProgress(caseProgress, "Medical Cases",
                        String.format("Generating medical cases: %d/%d", i + 1, bundles.size()));
            }

            Bundle bundle = bundles.get(i);
            try {
                MedicalCase medicalCase = fhirBundleAdapter.convertBundleToMedicalCase(bundle);
                medicalCases.add(medicalCase);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid FHIR Bundle data, skipping case: {}", e.getMessage());
            } catch (RuntimeException e) {
                log.error("Failed to convert FHIR Bundle to MedicalCase", e);
            }
        }

        batchProcess(
                medicalCases,
                MedicalCase::id,
                (ids) -> medicalCaseRepository.findByIds(ids).stream()
                        .collect(Collectors.toMap(MedicalCase::id, Function.identity())),
                medicalCaseRepository::insertBatch,
                medicalCaseRepository::updateBatch,
                "medical cases"
        );

        casesGeneratedCounter.increment(medicalCases.size());
        log.info("Generated {} medical cases", medicalCases.size());
    }

    private Patient createAnonymizedPatient() {
        Patient patient = new Patient();
        patient.setId(IdGenerator.generateId());

        int age = random.nextInt(73) + 18;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -age);
        patient.setBirthDate(cal.getTime());

        return patient;
    }

    private Condition createCondition(String patientId, List<String> icd10Codes,
                                      List<String> extendedIcd10Codes, List<String> severities) {
        Condition condition = new Condition();
        condition.setId(IdGenerator.generateId());
        condition.setSubject(new Reference("Patient/" + patientId));

        CodeableConcept code = new CodeableConcept();
        List<String> availableCodes = extendedIcd10Codes.isEmpty() ? icd10Codes : extendedIcd10Codes;
        String icd10Code = availableCodes.get(random.nextInt(availableCodes.size()));
        Coding icd10Coding = new Coding();
        icd10Coding.setSystem(FHIR_ICD10_SYSTEM);
        icd10Coding.setCode(icd10Code);
        icd10Coding.setDisplay(getIcd10Display(icd10Code));
        code.addCoding(icd10Coding);
        code.setText(getIcd10Display(icd10Code));
        condition.setCode(code);

        CodeableConcept clinicalStatus = new CodeableConcept();
        clinicalStatus.addCoding(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                .setCode("active"));
        condition.setClinicalStatus(clinicalStatus);

        if (random.nextBoolean() && severities != null && !severities.isEmpty()) {
            CodeableConcept severity = new CodeableConcept();
            String severityValue = severities.get(random.nextInt(severities.size()));
            severity.addCoding(new Coding()
                    .setSystem("http://snomed.info/sct")
                    .setCode(severityValue));
            condition.setSeverity(severity);
        }

        return condition;
    }

    private Observation createObservation(String patientId, List<String> symptoms) {
        Observation observation = new Observation();
        observation.setId(IdGenerator.generateId());
        observation.setSubject(new Reference("Patient/" + patientId));
        observation.setStatus(ObservationStatus.FINAL);

        CodeableConcept code = new CodeableConcept();
        String symptom = symptoms.get(random.nextInt(symptoms.size()));
        code.setText(symptom);
        observation.setCode(code);
        observation.setValue(new StringType(symptom));

        return observation;
    }

    private Encounter createEncounter(String patientId, List<String> encounterClasses, List<String> encounterTypes) {
        Encounter encounter = new Encounter();
        encounter.setId(IdGenerator.generateId());

        EncounterStatus[] statuses = {
                EncounterStatus.COMPLETED,
                EncounterStatus.INPROGRESS,
                EncounterStatus.PLANNED
        };
        encounter.setStatus(statuses[random.nextInt(statuses.length)]);

        String encounterClass = encounterClasses.get(random.nextInt(encounterClasses.size()));
        CodeableConcept classConcept = new CodeableConcept();
        classConcept.addCoding(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
                .setCode(encounterClass)
                .setDisplay(getEncounterClassDisplay(encounterClass)));
        encounter.addClass_(classConcept);

        CodeableConcept type = new CodeableConcept();
        String encounterType = encounterTypes.get(random.nextInt(encounterTypes.size()));
        type.setText(encounterType);
        encounter.addType(type);

        return encounter;
    }

    /**
     * Gets display text for ICD-10 code from database (loaded from CSV).
     * Falls back to generic description if code not found in database.
     */
    private String getIcd10Display(String code) {
        if (code == null || code.isEmpty()) {
            return "Condition, unspecified";
        }

        return icd10CodeRepository.findByCode(code)
                .map(icd10Code -> icd10Code.description() != null && !icd10Code.description().isEmpty()
                        ? icd10Code.description()
                        : "Condition, unspecified")
                .orElse("Condition, unspecified");
    }

    /**
     * Gets display text for encounter class code from CSV data.
     * Falls back to lowercase code if not found in CSV.
     */
    private String getEncounterClassDisplay(String code) {
        if (code == null || code.isEmpty()) {
            return "";
        }
        return encounterClassDisplays.getOrDefault(code, code);
    }

    private <T> void batchProcess(
            List<T> items,
            Function<T, String> getId,
            Function<List<String>, Map<String, T>> getExistingItems,
            java.util.function.Consumer<List<T>> insertBatch,
            java.util.function.Consumer<List<T>> updateBatch,
            String entityName) {

        if (items.isEmpty()) {
            log.debug("No {} to process", entityName);
            return;
        }

        List<String> ids = items.stream().map(getId).toList();
        Map<String, T> existingItems = getExistingItems.apply(ids);
        Set<String> existingIds = existingItems.keySet();

        List<T> toInsert = new ArrayList<>();
        List<T> toUpdate = new ArrayList<>();

        for (T item : items) {
            String id = getId.apply(item);
            if (existingIds.contains(id)) {
                toUpdate.add(item);
            } else {
                toInsert.add(item);
            }
        }

        if (!toInsert.isEmpty()) {
            insertBatch.accept(toInsert);
            log.debug("Inserted {} new {}", toInsert.size(), entityName);
        }

        if (!toUpdate.isEmpty()) {
            updateBatch.accept(toUpdate);
            log.debug("Updated {} existing {}", toUpdate.size(), entityName);
        }
    }
}
