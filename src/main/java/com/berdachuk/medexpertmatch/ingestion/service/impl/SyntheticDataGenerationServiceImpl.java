package com.berdachuk.medexpertmatch.ingestion.service.impl;

import com.berdachuk.medexpertmatch.ingestion.service.ClinicalExperienceGeneratorService;
import com.berdachuk.medexpertmatch.doctor.domain.MedicalSpecialty;
import com.berdachuk.medexpertmatch.doctor.repository.MedicalSpecialtyRepository;
import com.berdachuk.medexpertmatch.ingestion.service.DoctorGeneratorService;
import com.berdachuk.medexpertmatch.ingestion.service.FacilityGeneratorService;
import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataCatalogState;
import com.berdachuk.medexpertmatch.ingestion.exception.SyntheticDataGenerationException;
import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataGenerationProgress;
import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataGenerationService;
import com.berdachuk.medexpertmatch.ingestion.service.MedicalCaseGeneratorService;
import com.berdachuk.medexpertmatch.medicalcoding.domain.ICD10Code;
import com.berdachuk.medexpertmatch.medicalcoding.domain.Procedure;
import com.berdachuk.medexpertmatch.medicalcoding.repository.ICD10CodeRepository;
import com.berdachuk.medexpertmatch.medicalcoding.repository.ProcedureRepository;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r5.model.Bundle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.Function;

/**
 * Encapsulates the heavy-lifting logic for synthetic data generation so the facade can stay thin.
 */
@Slf4j
@Service
public class SyntheticDataGenerationServiceImpl implements SyntheticDataGenerationService {

    private final FacilityGeneratorService facilityGeneratorService;
    private final DoctorGeneratorService doctorGeneratorService;
    private final MedicalCaseGeneratorService medicalCaseGeneratorService;
    private final ClinicalExperienceGeneratorService clinicalExperienceGeneratorService;
    private final MedicalSpecialtyRepository medicalSpecialtyRepository;
    private final ICD10CodeRepository icd10CodeRepository;
    private final ProcedureRepository procedureRepository;
    private final SyntheticDataCatalogState catalogState;

    @Value("${medexpertmatch.synthetic-data.batch-size:5000}")
    private int batchSize;

    public SyntheticDataGenerationServiceImpl(
            FacilityGeneratorService facilityGeneratorService,
            DoctorGeneratorService doctorGeneratorService,
            MedicalCaseGeneratorService medicalCaseGeneratorService,
            ClinicalExperienceGeneratorService clinicalExperienceGeneratorService,
            MedicalSpecialtyRepository medicalSpecialtyRepository,
            ICD10CodeRepository icd10CodeRepository,
            ProcedureRepository procedureRepository,
            SyntheticDataCatalogState catalogState) {
        this.facilityGeneratorService = facilityGeneratorService;
        this.doctorGeneratorService = doctorGeneratorService;
        this.medicalCaseGeneratorService = medicalCaseGeneratorService;
        this.clinicalExperienceGeneratorService = clinicalExperienceGeneratorService;
        this.medicalSpecialtyRepository = medicalSpecialtyRepository;
        this.icd10CodeRepository = icd10CodeRepository;
        this.procedureRepository = procedureRepository;
        this.catalogState = catalogState;
    }

    @Override
    public void generateIcd10Codes(String size, SyntheticDataGenerationProgress progress) {
        validateSize(size);

        log.info("Generating ICD-10 codes for size: {}", size);

        try {
            List<ICD10Code> existingCodes = icd10CodeRepository.findAll();
            int existingCount = existingCodes.size();
            int targetCount = calculateTargetIcd10Count(size);

            if (existingCount >= targetCount) {
                log.info("Sufficient ICD-10 codes exist in database ({} >= {}), loading existing codes",
                        existingCount, targetCount);
                catalogState.setExtendedIcd10Codes(existingCodes.stream()
                        .map(ICD10Code::code)
                        .distinct()
                        .limit(targetCount)
                        .collect(Collectors.toList()));
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

        List<String> baseCodes = catalogState.getLoadedIcd10Codes();
        int baseCodeCount = baseCodes.size();
        int targetCodeCount = calculateTargetIcd10Count(size);
        int additionalCount = targetCodeCount - baseCodeCount;
        List<String> extendedCodes = new ArrayList<>();

        if (additionalCount > 0) {
            extendedCodes = catalogState.getCachedIcd10Codes().getOrDefault(size, new ArrayList<>());
            if (extendedCodes.isEmpty()) {
                extendedCodes = generateIcd10CodesFromBaseList(additionalCount);
                catalogState.getCachedIcd10Codes().put(size, new ArrayList<>(extendedCodes));
            }
        }

        Set<String> extendedIcd10CodesSet = new LinkedHashSet<>(baseCodes);
        extendedIcd10CodesSet.addAll(extendedCodes);
        catalogState.setExtendedIcd10Codes(new ArrayList<>(extendedIcd10CodesSet));

        log.info("Total ICD-10 codes to generate: {} ({} base + {} unique extended)",
                catalogState.getExtendedIcd10Codes().size(), baseCodes.size(),
                catalogState.getExtendedIcd10Codes().size() - baseCodes.size());

        List<ICD10Code> existingIcd10Codes = icd10CodeRepository.findAll();
        Map<String, String> categoryMap = new HashMap<>();
        Map<String, String> parentCodeMap = new HashMap<>();
        Map<String, List<String>> relatedCodesMap = new HashMap<>();

        for (ICD10Code existingCode : existingIcd10Codes) {
            categoryMap.put(existingCode.code(), existingCode.category());
            parentCodeMap.put(existingCode.code(), existingCode.parentCode());
            relatedCodesMap.put(existingCode.code(), existingCode.relatedCodes() != null ? existingCode.relatedCodes() : List.of());
        }

        for (String code : catalogState.getExtendedIcd10Codes()) {
            categoryMap.putIfAbsent(code, determineIcd10Category(code));
            parentCodeMap.putIfAbsent(code, extractParentCode(code));
        }

        Map<String, List<String>> codesByCategory = new HashMap<>();
        for (String code : catalogState.getExtendedIcd10Codes()) {
            String category = categoryMap.get(code);
            codesByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(code);
        }

        for (String code : catalogState.getExtendedIcd10Codes()) {
            if (!relatedCodesMap.containsKey(code)) {
                String category = categoryMap.get(code);
                List<String> related = codesByCategory.getOrDefault(category, List.of()).stream()
                        .filter(c -> !c.equals(code))
                        .limit(3)
                        .toList();
                relatedCodesMap.put(code, related);
            }
        }

        List<String> codes = catalogState.getExtendedIcd10Codes();
        Set<String> existingCodeStrings = icd10CodeRepository.findExistingCodes(codes);
        Map<String, ICD10Code> existingCodesMap = icd10CodeRepository.findByCodes(codes).stream()
                .collect(Collectors.toMap(ICD10Code::code, Function.identity()));

        Set<String> processedCodes = new LinkedHashSet<>();
        List<ICD10Code> icd10Codes = new ArrayList<>();
        for (String code : catalogState.getExtendedIcd10Codes()) {
            if (!processedCodes.add(code)) {
                log.debug("Skipping duplicate ICD-10 code: {}", code);
                continue;
            }

            String id = Optional.ofNullable(existingCodesMap.get(code))
                    .map(ICD10Code::id)
                    .orElseGet(IdGenerator::generateId);

            String description = Optional.ofNullable(getIcd10Display(code))
                    .filter(desc -> !desc.startsWith("Condition: "))
                    .orElseGet(() -> generateIcd10Description(code, categoryMap.get(code)));
            String category = categoryMap.getOrDefault(code, "Other");
            String parentCode = parentCodeMap.getOrDefault(code, null);
            List<String> relatedCodes = relatedCodesMap.getOrDefault(code, List.of());

            ICD10Code icd10Code = new ICD10Code(id, code, description, category, parentCode, relatedCodes);
            icd10Codes.add(icd10Code);
        }

        List<ICD10Code> toInsert = new ArrayList<>();
        List<ICD10Code> toUpdate = new ArrayList<>();
        for (ICD10Code icd10Code : icd10Codes) {
            if (existingCodeStrings.contains(icd10Code.code())) {
                toUpdate.add(icd10Code);
            } else {
                toInsert.add(icd10Code);
            }
        }

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
                catalogState.getExtendedIcd10Codes().size(), catalogState.getLoadedIcd10Codes().size(),
                catalogState.getExtendedIcd10Codes().size() - catalogState.getLoadedIcd10Codes().size(),
                toInsert.size(), toUpdate.size());
    }

    @Override
    public void generateMedicalSpecialties(String size, SyntheticDataGenerationProgress progress) {
        validateSize(size);

        log.info("Generating medical specialties for size: {}", size);

        try {
            int existingCount = medicalSpecialtyRepository.findAll().size();
            int targetCount = calculateTargetSpecialtyCount(size);

            if (existingCount >= targetCount) {
                log.info("Sufficient medical specialties exist in database ({} >= {}), skipping generation",
                        existingCount, targetCount);
                return;
            }
        } catch (DataAccessException e) {
            log.warn("Database error checking for existing medical specialties, proceeding with generation", e);
        } catch (RuntimeException e) {
            log.warn("Unexpected error checking database for existing medical specialties, proceeding with generation", e);
        }

        List<String> baseSpecialties = catalogState.getLoadedMedicalSpecialties();
        int baseSpecialtyCount = baseSpecialties.size();
        int targetSpecialtyCount = calculateTargetSpecialtyCount(size);
        int additionalCount = targetSpecialtyCount - baseSpecialtyCount;
        List<String> extendedSpecialtiesList = new ArrayList<>();

        if (additionalCount > 0) {
            extendedSpecialtiesList = catalogState.getCachedSpecialties().getOrDefault(size, new ArrayList<>());
            if (extendedSpecialtiesList.isEmpty()) {
                extendedSpecialtiesList = generateSpecialtiesFromBaseList(additionalCount);
                catalogState.getCachedSpecialties().put(size, new ArrayList<>(extendedSpecialtiesList));
            }
        }

        Set<String> extendedSpecialtiesSet = new LinkedHashSet<>(baseSpecialties);
        extendedSpecialtiesSet.addAll(extendedSpecialtiesList);
        List<String> extendedSpecialties = new ArrayList<>(extendedSpecialtiesSet);

        log.info("Total specialties to generate: {} ({} base + {} unique extended)",
                extendedSpecialties.size(), baseSpecialties.size(), extendedSpecialtiesList.size());

        List<MedicalSpecialty> existingSpecialties = medicalSpecialtyRepository.findAll();
        Map<String, List<String>> specialtyIcd10Ranges = new HashMap<>();
        Map<String, String> specialtyDescriptions = new HashMap<>();
        Map<String, List<String>> relatedSpecialtiesMap = new HashMap<>();

        for (MedicalSpecialty existingSpecialty : existingSpecialties) {
            specialtyIcd10Ranges.put(existingSpecialty.name(), existingSpecialty.icd10CodeRanges());
            specialtyDescriptions.put(existingSpecialty.name(), existingSpecialty.description());

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

        Map<String, String> specialtyIdMap = new HashMap<>();
        Set<String> processedSpecialties = new LinkedHashSet<>();

        for (String specialtyName : extendedSpecialties) {
            if (!processedSpecialties.add(specialtyName)) {
                log.debug("Skipping duplicate specialty: {}", specialtyName);
                continue;
            }

            String id = IdGenerator.generateId();
            specialtyIdMap.put(specialtyName, id);
            String normalizedName = normalizeSpecialtyName(specialtyName);
            String description = specialtyDescriptions.getOrDefault(specialtyName, generateSpecialtyDescription(specialtyName));
            List<String> icd10CodeRanges = specialtyIcd10Ranges.getOrDefault(specialtyName,
                    generateDefaultIcd10Ranges(specialtyName));
            List<String> relatedSpecialtyIds = relatedSpecialtiesMap.getOrDefault(specialtyName, List.of()).stream()
                    .map(specialtyIdMap::get)
                    .filter(idValue -> idValue != null)
                    .toList();

            MedicalSpecialty specialty = new MedicalSpecialty(id, specialtyName, normalizedName, description,
                    icd10CodeRanges, relatedSpecialtyIds);

            if (medicalSpecialtyRepository.findByName(specialtyName).isPresent()) {
                medicalSpecialtyRepository.update(specialty);
            } else {
                medicalSpecialtyRepository.insert(specialty);
            }
        }

        for (String specialtyName : processedSpecialties) {
            String specialtyId = specialtyIdMap.get(specialtyName);
            if (specialtyId == null) {
                log.warn("Specialty ID not found for: {}, skipping related specialty update", specialtyName);
                continue;
            }
            List<String> relatedSpecialtyIds = relatedSpecialtiesMap.getOrDefault(specialtyName, List.of()).stream()
                    .map(specialtyIdMap::get)
                    .filter(idValue -> idValue != null)
                    .toList();

            Optional<MedicalSpecialty> existing = medicalSpecialtyRepository.findById(specialtyId);
            if (existing.isPresent()) {
                MedicalSpecialty specialty = existing.get();
                MedicalSpecialty updated = new MedicalSpecialty(specialty.id(), specialty.name(), specialty.normalizedName(),
                        specialty.description(), specialty.icd10CodeRanges(), relatedSpecialtyIds);
                medicalSpecialtyRepository.update(updated);
            }
        }

        log.info("Generated {} medical specialties ({} base + {} extended)",
                extendedSpecialties.size(), baseSpecialties.size(), extendedSpecialtiesList.size());
    }

    @Override
    public void generateAdditionalProcedures(String size, SyntheticDataGenerationProgress progress) {
        List<String> cached = catalogState.getCachedProcedures().get(size);
        if (cached != null && !cached.isEmpty()) {
            Set<String> extendedProceduresSet = new LinkedHashSet<>(catalogState.getLoadedProcedures());
            extendedProceduresSet.addAll(cached);
            catalogState.setExtendedProcedures(new ArrayList<>(extendedProceduresSet));
            log.info("Total procedures available: {} ({} base + {} cached)",
                    catalogState.getExtendedProcedures().size(), catalogState.getLoadedProcedures().size(),
                    catalogState.getExtendedProcedures().size() - catalogState.getLoadedProcedures().size());
            return;
        }

        try {
            List<Procedure> existingProcedures = procedureRepository.findAll();
            int existingCount = existingProcedures.size();
            int targetCount = calculateTargetProcedureCount(size);

            if (existingCount >= targetCount) {
                log.info("Sufficient procedures exist in database ({} >= {}), loading existing procedures",
                        existingCount, targetCount);
                List<String> procedureNames = existingProcedures.stream()
                        .map(Procedure::name)
                        .distinct()
                        .limit(targetCount)
                        .collect(Collectors.toList());
                List<String> additionalProcedures = procedureNames.stream()
                        .filter(proc -> !catalogState.getLoadedProcedures().contains(proc))
                        .limit(targetCount - catalogState.getLoadedProcedures().size())
                        .collect(Collectors.toList());
                catalogState.getCachedProcedures().put(size, new ArrayList<>(additionalProcedures));
                Set<String> extendedProceduresSet = new LinkedHashSet<>(catalogState.getLoadedProcedures());
                extendedProceduresSet.addAll(additionalProcedures);
                catalogState.setExtendedProcedures(new ArrayList<>(extendedProceduresSet));
                log.info("Loaded {} existing procedures from database ({} base + {} unique from DB)",
                        catalogState.getExtendedProcedures().size(), catalogState.getLoadedProcedures().size(),
                        catalogState.getExtendedProcedures().size() - catalogState.getLoadedProcedures().size());
                return;
            }
        } catch (DataAccessException e) {
            log.warn("Database error checking for existing procedures, proceeding with generation", e);
        } catch (RuntimeException e) {
            log.warn("Unexpected error checking database for existing procedures, proceeding with generation", e);
        }

        if (!"huge".equals(size)) {
            List<String> hugeCache = catalogState.getCachedProcedures().get("huge");
            if (hugeCache != null && hugeCache.size() >= 1) {
                int targetCount = calculateTargetProcedureCount(size);
                int baseProcedureCount = catalogState.getLoadedProcedures().size();
                int needed = Math.max(0, targetCount - baseProcedureCount);
                if (needed > 0 && hugeCache.size() >= needed) {
                    List<String> subset = hugeCache.stream().limit(needed).toList();
                    catalogState.getCachedProcedures().put(size, new ArrayList<>(subset));
                    Set<String> extendedProceduresSet = new LinkedHashSet<>(catalogState.getLoadedProcedures());
                    extendedProceduresSet.addAll(subset);
                    catalogState.setExtendedProcedures(new ArrayList<>(extendedProceduresSet));
                    log.info("Total procedures available: {} ({} base + {} cached)",
                            catalogState.getExtendedProcedures().size(), catalogState.getLoadedProcedures().size(),
                            catalogState.getExtendedProcedures().size() - catalogState.getLoadedProcedures().size());
                    return;
                }
            }
        }

        int baseProcedureCount = catalogState.getLoadedProcedures().size();
        int targetProcedureCount = calculateTargetProcedureCount(size);
        int additionalCount = Math.max(0, targetProcedureCount - baseProcedureCount);

        if (additionalCount <= 0) {
            catalogState.setExtendedProcedures(new ArrayList<>(catalogState.getLoadedProcedures()));
            return;
        }

        log.info("Generating {} additional procedures using fallback method for size: {}", additionalCount, size);
        List<String> extendedProceduresList = generateProceduresFromBaseList(additionalCount);
        catalogState.getCachedProcedures().put(size, new ArrayList<>(extendedProceduresList));
        persistProceduresToDatabase(extendedProceduresList);
        Set<String> extendedProceduresSet = new LinkedHashSet<>(catalogState.getLoadedProcedures());
        extendedProceduresSet.addAll(extendedProceduresList);
        catalogState.setExtendedProcedures(new ArrayList<>(extendedProceduresSet));
        log.info("Total procedures available: {} ({} base + {} extended)",
                catalogState.getExtendedProcedures().size(), catalogState.getLoadedProcedures().size(),
                catalogState.getExtendedProcedures().size() - catalogState.getLoadedProcedures().size());
    }

    @Override
    public void generateFacilities(int count) {
        facilityGeneratorService.generateFacilities(count,
                catalogState.getLoadedFacilityTypes(), catalogState.getLoadedFacilityCapabilities());
    }

    @Override
    public void generateDoctors(int count, SyntheticDataGenerationProgress progress) {
        doctorGeneratorService.generateDoctors(count, progress,
                catalogState.getLoadedMedicalSpecialties(), catalogState.getLoadedAvailabilityStatuses());
    }

    @Override
    public void generateMedicalCases(int count, SyntheticDataGenerationProgress progress) {
        medicalCaseGeneratorService.generateMedicalCases(count, progress,
                catalogState.getLoadedIcd10Codes(), catalogState.getExtendedIcd10Codes(),
                catalogState.getLoadedSymptoms(), catalogState.getLoadedSeverities(),
                catalogState.getLoadedEncounterClasses(), catalogState.getLoadedEncounterTypes());
    }

    @Override
    public List<Bundle> generateFhirBundles(int count) {
        return medicalCaseGeneratorService.generateFhirBundles(count,
                catalogState.getLoadedIcd10Codes(), catalogState.getExtendedIcd10Codes(),
                catalogState.getLoadedSymptoms(), catalogState.getLoadedSeverities(),
                catalogState.getLoadedEncounterClasses(), catalogState.getLoadedEncounterTypes());
    }

    @Override
    public void generateClinicalExperiences(int doctorCount, int caseCount, SyntheticDataGenerationProgress progress) {
        clinicalExperienceGeneratorService.generateClinicalExperiences(doctorCount, caseCount, progress,
                catalogState.getLoadedComplexityLevels(), catalogState.getLoadedOutcomes(),
                catalogState.getLoadedComplications(), catalogState.getSpecialtyProceduresMap(),
                catalogState.getExtendedProcedures());
    }

    @Override
    public Map<String, DataSizeConfig> getAvailableSizes() {
        return catalogState.getDataSizeConfigs().isEmpty()
                ? Map.of()
                : Map.copyOf(catalogState.getDataSizeConfigs());
    }

    @Override
    public int calculateFacilityCount(int doctorCount) {
        return Math.max(5, doctorCount / 15);
    }

    private void validateSize(String size) {
        if (size == null || size.isBlank()) {
            throw new IllegalArgumentException("Size must not be null or blank");
        }
        if (!catalogState.getDataSizeConfigs().containsKey(size.toLowerCase())) {
            throw new IllegalArgumentException("Invalid size: " + size);
        }
    }

    private int calculateTargetIcd10Count(String size) {
        int baseCodeCount = catalogState.getLoadedIcd10Codes().size();
        return switch (size.toLowerCase()) {
            case "tiny" -> Math.max(baseCodeCount, 4);
            case "small" -> Math.max(baseCodeCount, 50);
            case "medium" -> Math.max(baseCodeCount, 100);
            case "large" -> Math.max(baseCodeCount, 200);
            case "huge" -> Math.max(baseCodeCount, 500);
            default -> baseCodeCount;
        };
    }

    private int calculateTargetSpecialtyCount(String size) {
        int baseSpecialtyCount = catalogState.getLoadedMedicalSpecialties().size();
        DataSizeConfig config = catalogState.getDataSizeConfigs().get(size.toLowerCase());
        if (config != null) {
            int targetCount = Math.max(3, Math.min(config.doctorCount() / 10, baseSpecialtyCount));
            return Math.max(baseSpecialtyCount, targetCount);
        }
        return switch (size.toLowerCase()) {
            case "tiny" -> Math.max(baseSpecialtyCount, 3);
            case "small" -> Math.max(baseSpecialtyCount, 25);
            case "medium" -> Math.max(baseSpecialtyCount, 50);
            case "large" -> Math.max(baseSpecialtyCount, 100);
            case "huge" -> Math.max(baseSpecialtyCount, 200);
            default -> baseSpecialtyCount;
        };
    }

    private int calculateTargetProcedureCount(String size) {
        int baseProcedureCount = catalogState.getLoadedProcedures().size();
        DataSizeConfig config = catalogState.getDataSizeConfigs().get(size.toLowerCase());
        if (config != null) {
            int targetCount = Math.max(3, Math.min(config.doctorCount() / 5, baseProcedureCount));
            return Math.max(baseProcedureCount, targetCount);
        }
        return switch (size.toLowerCase()) {
            case "tiny" -> Math.max(baseProcedureCount, 3);
            case "small" -> Math.max(baseProcedureCount, 30);
            case "medium" -> Math.max(baseProcedureCount, 50);
            case "large" -> Math.max(baseProcedureCount, 100);
            case "huge" -> Math.max(baseProcedureCount, 200);
            default -> baseProcedureCount;
        };
    }

    private List<String> generateIcd10CodesFromBaseList(int count) {
        List<String> result = new ArrayList<>();
        Set<String> allIcd10Codes = new HashSet<>(catalogState.getLoadedIcd10Codes());
        int codesToTake = Math.min(count, catalogState.getLoadedIcd10Codes().size());
        for (int i = 0; i < codesToTake; i++) {
            result.add(catalogState.getLoadedIcd10Codes().get(i));
        }
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
                if (result.size() >= count * 2) {
                    break;
                }
            }
        }
        return result.stream().limit(count).toList();
    }

    private List<String> generateSpecialtiesFromBaseList(int count) {
        List<String> result = new ArrayList<>();
        int specialtiesToTake = Math.min(count, catalogState.getLoadedMedicalSpecialties().size());
        for (int i = 0; i < specialtiesToTake; i++) {
            result.add(catalogState.getLoadedMedicalSpecialties().get(i));
        }
        return result;
    }

    private List<String> generateProceduresFromBaseList(int count) {
        List<String> result = new ArrayList<>();
        int proceduresToTake = Math.min(count, catalogState.getLoadedProcedures().size());
        for (int i = 0; i < proceduresToTake; i++) {
            result.add(catalogState.getLoadedProcedures().get(i));
        }
        return result;
    }

    private String getIcd10Display(String code) {
        return catalogState.getIcd10CodeDisplays().get(code);
    }

    private String getEncounterClassDisplay(String code) {
        return catalogState.getEncounterClassDisplays().getOrDefault(code, code);
    }

    private String normalizeSpecialtyName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        return name.toLowerCase().replaceAll("[^a-z0-9]", "").trim();
    }

    private String generateSpecialtyDescription(String specialtyName) {
        String lowerName = specialtyName.toLowerCase();
        if (lowerName.contains("surgery")) {
            return "Surgical specialty focusing on " + lowerName.replace("surgery", "").trim();
        } else if (lowerName.contains("pediatric")) {
            return "Pediatric specialty for " + lowerName.replace("pediatric", "").trim();
        } else if (lowerName.contains("medicine")) {
            return "Medical specialty: " + specialtyName;
        } else {
            return "Medical specialty focusing on " + lowerName;
        }
    }

    private String determineIcd10Category(String code) {
        if (code == null || code.isEmpty()) {
            return "Other";
        }
        return switch (code.charAt(0)) {
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
            case 'D' -> "Diseases of the blood and blood-forming organs and certain disorders involving the immune mechanism";
            case 'O' -> "Pregnancy, childbirth and the puerperium";
            case 'P' -> "Certain conditions originating in the perinatal period";
            case 'Q' -> "Congenital malformations, deformations and chromosomal abnormalities";
            case 'U' -> "Codes for special purposes";
            default -> "Other";
        };
    }

    private String extractParentCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        int dotIndex = code.indexOf('.');
        if (dotIndex > 0) {
            return code.substring(0, dotIndex);
        }
        if (code.length() >= 3) {
            return null;
        }
        return null;
    }

    private List<String> generateDefaultIcd10Ranges(String specialtyName) {
        String lowerName = specialtyName.toLowerCase();
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
        } else if (lowerName.contains("pediatric")) {
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
        }
        return List.of("E00-E90", "I00-I99", "R00-R99");
    }

    private String generateIcd10Description(String code, String category) {
        if (code == null || code.isEmpty()) {
            return "Condition: " + code;
        }
        String prefix = code.substring(0, 1);
        boolean hasSubcode = code.contains(".");
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
        return hasSubcode ? baseDescription + ", unspecified" : baseDescription;
    }

    private String normalizeProcedureName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        return name.toLowerCase().replaceAll("[^a-z0-9]", "").trim();
    }

    @Transactional
    public void persistProceduresToDatabase(List<String> procedureNames) {
        if (procedureNames == null || procedureNames.isEmpty()) {
            return;
        }
        Set<String> uniqueProcedureNames = new LinkedHashSet<>(procedureNames);
        log.debug("Persisting {} unique procedures to database", uniqueProcedureNames.size());
        int persistedCount = 0;
        int skippedCount = 0;

        for (String procedureName : uniqueProcedureNames) {
            if (procedureName == null || procedureName.isBlank()) {
                continue;
            }
            try {
                String normalizedName = normalizeProcedureName(procedureName);
                Optional<Procedure> existing = procedureRepository.findByNormalizedName(normalizedName);
                if (existing.isPresent()) {
                    skippedCount++;
                    continue;
                }
                Procedure procedure = new Procedure(IdGenerator.generateId(), procedureName, normalizedName, null, null);
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

}
