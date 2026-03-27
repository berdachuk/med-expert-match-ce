package com.berdachuk.medexpertmatch.ingestion.service.impl;

import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.doctor.domain.MedicalSpecialty;
import com.berdachuk.medexpertmatch.doctor.repository.MedicalSpecialtyRepository;
import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataBootstrapService;
import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataCatalogState;
import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataGenerationService;
import com.berdachuk.medexpertmatch.ingestion.util.CsvDataLoader;
import com.berdachuk.medexpertmatch.medicalcoding.domain.ICD10Code;
import com.berdachuk.medexpertmatch.medicalcoding.domain.Procedure;
import com.berdachuk.medexpertmatch.medicalcoding.repository.ICD10CodeRepository;
import com.berdachuk.medexpertmatch.medicalcoding.repository.ProcedureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Loads reference synthetic data from CSV files and persists it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyntheticDataBootstrapServiceImpl implements SyntheticDataBootstrapService {

    private final ResourceLoader resourceLoader;
    private final MedicalSpecialtyRepository medicalSpecialtyRepository;
    private final ICD10CodeRepository icd10CodeRepository;
    private final ProcedureRepository procedureRepository;
    private final SyntheticDataCatalogState catalogState;

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

    @Override
    public void loadDataFromFiles() {
        List<Map<String, String>> specialtiesData = CsvDataLoader.loadCsv(resourceLoader, medicalSpecialtiesFile, "medical specialties");
        persistMedicalSpecialties(specialtiesData);
        catalogState.setLoadedMedicalSpecialties(specialtiesData.stream()
                .map(row -> row.get("name"))
                .filter(name -> name != null && !name.isEmpty())
                .collect(Collectors.toList()));
        log.info("Loaded {} medical specialties from {}", catalogState.getLoadedMedicalSpecialties().size(), medicalSpecialtiesFile);

        List<Map<String, String>> icd10Data = CsvDataLoader.loadCsv(resourceLoader, icd10CodesFile, "ICD-10 codes");
        persistIcd10Codes(icd10Data);
        catalogState.setLoadedIcd10Codes(icd10Data.stream()
                .map(row -> row.get("code"))
                .filter(code -> code != null && !code.isEmpty())
                .collect(Collectors.toList()));
        catalogState.setIcd10CodeDisplays(icd10Data.stream()
                .filter(row -> row.get("code") != null && row.get("description") != null)
                .collect(Collectors.toMap(
                        row -> row.get("code"),
                        row -> row.get("description"),
                        (existing, replacement) -> existing
                )));
        log.info("Loaded {} ICD-10 codes from {}", catalogState.getLoadedIcd10Codes().size(), icd10CodesFile);

        List<Map<String, String>> proceduresData = CsvDataLoader.loadCsv(resourceLoader, proceduresFile, "procedures");
        persistProcedures(proceduresData);
        catalogState.setLoadedProcedures(proceduresData.stream()
                .map(row -> row.get("name"))
                .filter(name -> name != null && !name.isEmpty())
                .collect(Collectors.toList()));
        log.info("Loaded {} procedures from {}", catalogState.getLoadedProcedures().size(), proceduresFile);

        requireLoaded(catalogState.getLoadedMedicalSpecialties(), "medical specialties", medicalSpecialtiesFile);
        requireLoaded(catalogState.getLoadedIcd10Codes(), "ICD-10 codes", icd10CodesFile);
        requireLoaded(catalogState.getLoadedProcedures(), "procedures", proceduresFile);

        catalogState.setLoadedFacilityTypes(loadNameList(facilityTypesFile, "facility types"));
        catalogState.setLoadedComplexityLevels(loadNameList(complexityLevelsFile, "complexity levels"));
        catalogState.setLoadedOutcomes(loadNameList(outcomesFile, "outcomes"));
        catalogState.setLoadedAvailabilityStatuses(loadNameList(availabilityStatusesFile, "availability statuses"));
        catalogState.setLoadedSeverities(loadNameList(severitiesFile, "severities"));
        catalogState.setLoadedSymptoms(loadNameList(symptomsFile, "symptoms"));
        catalogState.setLoadedEncounterTypes(loadNameList(encounterTypesFile, "encounter types"));
        catalogState.setLoadedComplications(loadNameList(complicationsFile, "complications"));
        catalogState.setLoadedFacilityCapabilities(loadNameList(facilityCapabilitiesFile, "facility capabilities"));

        List<Map<String, String>> encounterClassesData = CsvDataLoader.loadCsv(resourceLoader, encounterClassesFile, "encounter classes");
        catalogState.setLoadedEncounterClasses(encounterClassesData.stream()
                .map(row -> row.get("code"))
                .filter(code -> code != null && !code.isEmpty())
                .collect(Collectors.toList()));
        catalogState.setEncounterClassDisplays(encounterClassesData.stream()
                .filter(row -> row.get("code") != null && row.get("display") != null)
                .collect(Collectors.toMap(
                        row -> row.get("code"),
                        row -> row.get("display"),
                        (existing, replacement) -> existing
                )));
        log.info("Loaded {} encounter classes from {}", catalogState.getLoadedEncounterClasses().size(), encounterClassesFile);

        requireLoaded(catalogState.getLoadedFacilityTypes(), "facility types", facilityTypesFile);
        requireLoaded(catalogState.getLoadedComplexityLevels(), "complexity levels", complexityLevelsFile);
        requireLoaded(catalogState.getLoadedOutcomes(), "outcomes", outcomesFile);
        requireLoaded(catalogState.getLoadedSymptoms(), "symptoms", symptomsFile);
        requireLoaded(catalogState.getLoadedFacilityCapabilities(), "facility capabilities", facilityCapabilitiesFile);

        catalogState.getSpecialtyProceduresMap().clear();
        List<Map<String, String>> specialtyProceduresData = CsvDataLoader.loadCsv(resourceLoader, specialtyProceduresFile, "specialty-procedures");
        for (Map<String, String> row : specialtyProceduresData) {
            String specialtyName = row.get("specialty_name");
            String procedureNames = row.getOrDefault("procedure_names", "");
            if (specialtyName != null && !specialtyName.isEmpty() && !procedureNames.isEmpty()) {
                catalogState.getSpecialtyProceduresMap().put(specialtyName, CsvDataLoader.parseCommaSeparatedList(procedureNames));
            }
        }
        log.info("Loaded {} specialty-procedure mappings from {}", catalogState.getSpecialtyProceduresMap().size(), specialtyProceduresFile);

        catalogState.getDataSizeConfigs().clear();
        List<Map<String, String>> dataSizesData = CsvDataLoader.loadCsv(resourceLoader, dataSizesFile, "data sizes");
        for (Map<String, String> row : dataSizesData) {
            String size = row.get("size");
            String doctorCount = row.get("doctor_count");
            String caseCount = row.get("case_count");
            String description = row.getOrDefault("description", "");
            String estimatedTime = row.getOrDefault("estimated_time_minutes", "0");
            if (size == null || size.isEmpty() || doctorCount == null || caseCount == null) {
                continue;
            }
            try {
                SyntheticDataGenerationService.DataSizeConfig config = new SyntheticDataGenerationService.DataSizeConfig(
                        size.toLowerCase(),
                        Integer.parseInt(doctorCount),
                        Integer.parseInt(caseCount),
                        description.isEmpty() ? size : description,
                        Integer.parseInt(estimatedTime)
                );
                catalogState.getDataSizeConfigs().put(size.toLowerCase(), config);
            } catch (NumberFormatException e) {
                log.warn("Skipping invalid data size row: size={}, doctor_count={}, case_count={}", size, doctorCount, caseCount);
            }
        }
        if (catalogState.getDataSizeConfigs().isEmpty()) {
            throw new IllegalStateException("Failed to load data sizes from " + dataSizesFile);
        }

        catalogState.setExtendedIcd10Codes(new ArrayList<>(catalogState.getLoadedIcd10Codes()));
        catalogState.setExtendedProcedures(new ArrayList<>(catalogState.getLoadedProcedures()));
    }

    @Override
    @Transactional
    public void persistMedicalSpecialties(List<Map<String, String>> specialtiesData) {
        for (Map<String, String> row : specialtiesData) {
            String name = row.get("name");
            if (name == null || name.isEmpty()) {
                continue;
            }
            Optional<MedicalSpecialty> existing = medicalSpecialtyRepository.findByName(name);
            if (existing.isPresent()) {
                continue;
            }

            String id = IdGenerator.generateId();
            String normalizedName = CsvDataLoader.normalize(name);
            String description = row.getOrDefault("description", "");
            List<String> icd10CodeRanges = CsvDataLoader.parseCommaSeparatedList(row.getOrDefault("icd10_code_ranges", ""));
            List<String> relatedSpecialtyNames = CsvDataLoader.parseCommaSeparatedList(row.getOrDefault("related_specialty_names", ""));
            List<String> relatedSpecialtyIds = new ArrayList<>();
            for (String relatedName : relatedSpecialtyNames) {
                medicalSpecialtyRepository.findByName(relatedName)
                        .map(MedicalSpecialty::id)
                        .ifPresent(relatedSpecialtyIds::add);
            }

            try {
                medicalSpecialtyRepository.insert(new MedicalSpecialty(
                        id,
                        name,
                        normalizedName,
                        description,
                        icd10CodeRanges,
                        relatedSpecialtyIds
                ));
            } catch (DataAccessException e) {
                log.warn("Failed to insert medical specialty {}: {}", name, e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void persistIcd10Codes(List<Map<String, String>> icd10Data) {
        for (Map<String, String> row : icd10Data) {
            String code = row.get("code");
            if (code == null || code.isEmpty()) {
                continue;
            }
            if (icd10CodeRepository.findByCode(code).isPresent()) {
                continue;
            }

            try {
                icd10CodeRepository.insert(new ICD10Code(
                        IdGenerator.generateId(),
                        code,
                        row.getOrDefault("description", ""),
                        row.getOrDefault("category", ""),
                        row.getOrDefault("parent_code", null),
                        CsvDataLoader.parseCommaSeparatedList(row.getOrDefault("related_codes", ""))
                ));
            } catch (DataAccessException e) {
                log.warn("Failed to insert ICD-10 code {}: {}", code, e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void persistProcedures(List<Map<String, String>> proceduresData) {
        for (Map<String, String> row : proceduresData) {
            String name = row.get("name");
            if (name == null || name.isEmpty()) {
                continue;
            }
            if (procedureRepository.findByName(name).isPresent()) {
                continue;
            }

            try {
                procedureRepository.insert(new Procedure(
                        IdGenerator.generateId(),
                        name,
                        CsvDataLoader.normalize(name),
                        row.getOrDefault("description", ""),
                        row.getOrDefault("category", "")
                ));
            } catch (DataAccessException e) {
                log.warn("Failed to insert procedure {}: {}", name, e.getMessage());
            }
        }
    }

    private List<String> loadNameList(String file, String label) {
        List<Map<String, String>> rows = CsvDataLoader.loadCsv(resourceLoader, file, label);
        List<String> values = rows.stream()
                .map(row -> row.get("name"))
                .filter(name -> name != null && !name.isEmpty())
                .collect(Collectors.toList());
        log.info("Loaded {} {} from {}", values.size(), label, file);
        return values;
    }

    private void requireLoaded(List<String> values, String label, String file) {
        if (values.isEmpty()) {
            throw new IllegalStateException("Failed to load " + label + " from " + file);
        }
    }
}
