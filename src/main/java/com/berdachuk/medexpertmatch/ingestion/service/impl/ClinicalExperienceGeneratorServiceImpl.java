package com.berdachuk.medexpertmatch.ingestion.service.impl;

import com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience;
import com.berdachuk.medexpertmatch.clinicalexperience.repository.ClinicalExperienceRepository;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.doctor.domain.MedicalSpecialty;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.doctor.repository.MedicalSpecialtyRepository;
import com.berdachuk.medexpertmatch.ingestion.service.ClinicalExperienceGeneratorService;
import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataGenerationProgress;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service implementation for generating clinical experiences linking doctors to cases.
 *
 * <p>This implementation uses a doctor-centric approach with two phases:
 * <ol>
 *   <li><b>Phase 1</b>: Guarantees each doctor gets at least one case by
 *       first assigning matching cases, then falling back to any unassigned case</li>
 *   <li><b>Phase 2</b>: Distributes remaining unassigned cases among all doctors
 *       using specialty-based matching when possible</li>
 * </ol>
 *
 * <p>This ensures that every doctor has at least one medical case in the system,
 * while still prioritizing specialty-based matching.
 */
@Slf4j
@Service
public class ClinicalExperienceGeneratorServiceImpl implements ClinicalExperienceGeneratorService {

    private static final double COMPLICATION_PROBABILITY = 0.3;
    private static final int MIN_COMPLICATIONS = 1;
    private static final int MAX_COMPLICATIONS = 2;
    private static final int MAX_TIME_TO_RESOLUTION_DAYS = 30;
    private static final int MIN_TIME_TO_RESOLUTION_DAYS = 1;
    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;
    private static final int BATCH_SIZE = 5000;

    private final DoctorRepository doctorRepository;
    private final MedicalCaseRepository medicalCaseRepository;
    private final ClinicalExperienceRepository clinicalExperienceRepository;
    private final MedicalSpecialtyRepository medicalSpecialtyRepository;
    private final Random random = new Random();

    public ClinicalExperienceGeneratorServiceImpl(
            DoctorRepository doctorRepository,
            MedicalCaseRepository medicalCaseRepository,
            ClinicalExperienceRepository clinicalExperienceRepository,
            MedicalSpecialtyRepository medicalSpecialtyRepository) {
        this.doctorRepository = doctorRepository;
        this.medicalCaseRepository = medicalCaseRepository;
        this.clinicalExperienceRepository = clinicalExperienceRepository;
        this.medicalSpecialtyRepository = medicalSpecialtyRepository;
    }

    @Override
    @Transactional
    public void generateClinicalExperiences(int doctorCount, int caseCount, SyntheticDataGenerationProgress progress,
                                            List<String> complexityLevels, List<String> outcomes, List<String> complications,
                                            Map<String, List<String>> specialtyProceduresMap, List<String> extendedProcedures) {
        log.info("Generating clinical experiences with expertise-based matching");

        List<String> loadedComplexityLevels = complexityLevels != null && !complexityLevels.isEmpty()
                ? complexityLevels : List.of("MEDIUM");
        List<String> loadedOutcomes = outcomes != null && !outcomes.isEmpty()
                ? outcomes : List.of("SUCCESS");
        List<String> loadedComplications = complications != null ? complications : List.of();
        Map<String, List<String>> loadedSpecialtyProceduresMap = specialtyProceduresMap != null ? specialtyProceduresMap : Map.of();
        List<String> loadedExtendedProcedures = extendedProcedures != null && !extendedProcedures.isEmpty()
                ? extendedProcedures : List.of("Consultation", "Physical Examination");

        // Load all doctors and cases from database (use 0 or negative for no limit)
        List<String> allDoctorIds = doctorRepository.findAllIds(0);
        List<Doctor> allDoctors = doctorRepository.findByIds(allDoctorIds);

        List<String> allCaseIds = medicalCaseRepository.findAllIds(0);
        List<MedicalCase> allCases = medicalCaseRepository.findByIds(allCaseIds);

        if (allDoctors.isEmpty() || allCases.isEmpty()) {
            log.warn("No doctors or cases available for clinical experience generation");
            return;
        }

        // Filter to requested counts (prefer newest entities)
        List<Doctor> doctors = filterToCount(allDoctors, doctorCount);
        List<MedicalCase> cases = filterToCount(allCases, caseCount);

        // Log selection statistics
        log.info("Selected {} doctors out of {} total in database (newest: {})",
                doctors.size(), allDoctors.size(), doctors.stream().map(Doctor::id).toList());
        log.info("Selected {} cases out of {} total in database (newest: {})",
                cases.size(), allCases.size(), cases.stream().map(MedicalCase::id).toList());

        List<MedicalSpecialty> allSpecialties = medicalSpecialtyRepository.findAll();
        Map<String, MedicalSpecialty> specialtyMap = allSpecialties.stream()
                .collect(Collectors.toMap(MedicalSpecialty::name, Function.identity()));

        List<ClinicalExperience> experiences = new ArrayList<>();
        int experienceCount = 0;
        int matchedCount = 0;
        int unmatchedCount = 0;

        // Track assigned cases to avoid duplicates
        Set<String> assignedCaseIds = new HashSet<>();
        Map<String, Integer> doctorCaseCount = new HashMap<>();

        // Phase 1: Guarantee each doctor gets at least one case
        log.info("Phase 1: Ensuring every doctor gets at least one case");
        // Process ALL doctors in the database to ensure none are left without cases
        for (int i = 0; i < allDoctors.size(); i++) {
            Doctor doctor = allDoctors.get(i);

            if (progress != null && progress.isCancelled()) {
                log.info("Generation cancelled during Phase 1 at {}/{}", i, allDoctors.size());
                return;
            }

            if (progress != null && allDoctors.size() > 0 && (i + 1) % Math.max(1, allDoctors.size() / 10) == 0) {
                int phase1Progress = 90 + ((i + 1) * 4 / allDoctors.size());
                progress.updateProgress(phase1Progress, "Clinical Experiences (Phase 1)",
                        String.format("Assigning cases to doctors: {}/{}", i + 1, allDoctors.size()));
            }

            // Find matching cases for this doctor from the filtered cases set
            List<MedicalCase> matchingCases = findMatchingCasesForDoctor(doctor, cases, specialtyMap);

            // Filter to only unassigned cases
            List<MedicalCase> unassignedMatchingCases = matchingCases.stream()
                    .filter(c -> !assignedCaseIds.contains(c.id()))
                    .toList();

            MedicalCase assignedCase;

            if (!unassignedMatchingCases.isEmpty()) {
                assignedCase = unassignedMatchingCases.get(random.nextInt(unassignedMatchingCases.size()));
                matchedCount++;
            } else {
                // Find any unassigned case from the filtered cases set
                List<MedicalCase> unassignedCases = cases.stream()
                        .filter(c -> !assignedCaseIds.contains(c.id()))
                        .toList();

                if (!unassignedCases.isEmpty()) {
                    assignedCase = unassignedCases.get(random.nextInt(unassignedCases.size()));
                    unmatchedCount++;
                } else {
                    // All cases already assigned, assign any case (duplicate assignment allowed)
                    log.warn("All cases already assigned, reassigning case to doctor {}", doctor.id());
                    assignedCase = cases.get(random.nextInt(cases.size()));
                    unmatchedCount++;
                }
            }

            // Create clinical experience
            ClinicalExperience experience = createClinicalExperience(doctor, assignedCase,
                    loadedComplexityLevels, loadedOutcomes, loadedComplications,
                    loadedSpecialtyProceduresMap, loadedExtendedProcedures);
            experiences.add(experience);
            experienceCount++;

            // Track case assignment and doctor case count
            assignedCaseIds.add(assignedCase.id());
            doctorCaseCount.put(doctor.id(), doctorCaseCount.getOrDefault(doctor.id(), 0) + 1);
        }

        // Phase 2: Distribute remaining unassigned cases
        List<MedicalCase> remainingCases = cases.stream()
                .filter(c -> !assignedCaseIds.contains(c.id()))
                .toList();

        if (!remainingCases.isEmpty()) {
            log.info("Phase 2: Distributing {} remaining cases", remainingCases.size());

            for (int i = 0; i < remainingCases.size(); i++) {
                MedicalCase medicalCase = remainingCases.get(i);

                if (progress != null && progress.isCancelled()) {
                    log.info("Generation cancelled during Phase 2 at {}/{}", i, remainingCases.size());
                    return;
                }

                // Find matching doctors for this case (still use filtered doctors for distribution)
                List<Doctor> matchingDoctors = findMatchingDoctorsForCase(medicalCase, doctors, specialtyMap);

                Doctor assignedDoctor;
                if (!matchingDoctors.isEmpty()) {
                    assignedDoctor = matchingDoctors.get(random.nextInt(matchingDoctors.size()));
                    matchedCount++;
                } else {
                    // Use filtered doctors for distribution but ensure all doctors eventually get cases
                    assignedDoctor = doctors.get(random.nextInt(doctors.size()));
                    unmatchedCount++;
                }

                // Create clinical experience
                ClinicalExperience experience = createClinicalExperience(assignedDoctor, medicalCase,
                        loadedComplexityLevels, loadedOutcomes, loadedComplications,
                        loadedSpecialtyProceduresMap, loadedExtendedProcedures);
                experiences.add(experience);
                experienceCount++;

                // Track doctor case count
                doctorCaseCount.put(assignedDoctor.id(), doctorCaseCount.getOrDefault(assignedDoctor.id(), 0) + 1);
            }
        }

        Map<String, ClinicalExperience> uniqueExperiencesMap = new LinkedHashMap<>();
        for (ClinicalExperience experience : experiences) {
            String key = experience.doctorId() + "|" + experience.caseId();
            if (!uniqueExperiencesMap.containsKey(key)) {
                uniqueExperiencesMap.put(key, experience);
            }
        }
        List<ClinicalExperience> uniqueExperiences = new ArrayList<>(uniqueExperiencesMap.values());

        List<String> experienceIds = uniqueExperiences.stream().map(ClinicalExperience::id).toList();
        Set<String> existingIds = clinicalExperienceRepository.findExistingIds(experienceIds);

        List<ClinicalExperience> toInsert = new ArrayList<>();
        List<ClinicalExperience> toUpdate = new ArrayList<>();
        for (ClinicalExperience experience : uniqueExperiences) {
            if (existingIds.contains(experience.id())) {
                toUpdate.add(experience);
            } else {
                toInsert.add(experience);
            }
        }

        for (int i = 0; i < toInsert.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, toInsert.size());
            clinicalExperienceRepository.insertBatch(toInsert.subList(i, end));
        }

        for (int i = 0; i < toUpdate.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, toUpdate.size());
            clinicalExperienceRepository.updateBatch(toUpdate.subList(i, end));
        }

        // Log statistics for all doctors in the database
        Map<String, Integer> allDoctorCaseCounts = new HashMap<>();
        // Initialize all doctors with 0 cases
        for (Doctor doctor : allDoctors) {
            allDoctorCaseCounts.put(doctor.id(), 0);
        }
        // Update with actual case counts
        allDoctorCaseCounts.putAll(doctorCaseCount);

        if (!allDoctorCaseCounts.isEmpty()) {
            IntSummaryStatistics stats = allDoctorCaseCounts.values().stream()
                    .mapToInt(Integer::intValue)
                    .summaryStatistics();

            int doctorsWithZeroCases = (int) allDoctorCaseCounts.values().stream()
                    .filter(count -> count == 0)
                    .count();

            log.info("Clinical Experience Statistics:");
            log.info("  Total doctors: {}", allDoctors.size());
            log.info("  Total cases: {}", cases.size());
            log.info("  Doctors with 0 cases: {}", doctorsWithZeroCases);
            log.info("  Cases per doctor - Min: {}, Max: {}, Average: {:.2f}",
                    stats.getMin(), stats.getMax(), stats.getAverage());
            log.info("  Generated {} clinical experiences ({} matched to expertise, {} unmatched, {} inserted, {} updated)",
                    experienceCount, matchedCount, unmatchedCount, toInsert.size(), toUpdate.size());
        } else {
            log.info("Generated {} clinical experiences for {} cases ({} matched to expertise, {} unmatched, {} inserted, {} updated)",
                    experienceCount, cases.size(), matchedCount, unmatchedCount, toInsert.size(), toUpdate.size());
        }
    }

    /**
     * Finds doctors that match a given medical case based on specialty and ICD-10 code ranges.
     * This is the reverse of findMatchingCasesForDoctor - finds doctors for a case instead of cases for a doctor.
     *
     * @param medicalCase  The medical case to find matching doctors for
     * @param doctors      List of all available doctors
     * @param specialtyMap Map of specialty names to MedicalSpecialty entities
     * @return List of doctors that match the case's requirements
     */
    private List<Doctor> findMatchingDoctorsForCase(
            MedicalCase medicalCase,
            List<Doctor> doctors,
            Map<String, MedicalSpecialty> specialtyMap) {

        List<Doctor> matchingDoctors = new ArrayList<>();

        if (medicalCase.icd10Codes() == null || medicalCase.icd10Codes().isEmpty()) {
            return matchingDoctors;
        }

        // Check each doctor's specialties against the case's ICD-10 codes
        for (Doctor doctor : doctors) {
            if (doctor.specialties() == null || doctor.specialties().isEmpty()) {
                continue;
            }

            Set<String> doctorIcd10Ranges = new HashSet<>();
            for (String specialtyName : doctor.specialties()) {
                MedicalSpecialty specialty = specialtyMap.get(specialtyName);
                if (specialty != null && specialty.icd10CodeRanges() != null) {
                    doctorIcd10Ranges.addAll(specialty.icd10CodeRanges());
                }
            }

            // Check if any of the case's ICD-10 codes match the doctor's specialty ranges
            for (String icd10Code : medicalCase.icd10Codes()) {
                if (matchesIcd10Range(icd10Code, doctorIcd10Ranges)) {
                    matchingDoctors.add(doctor);
                    break; // Found a match, no need to check other ICD-10 codes for this doctor
                }
            }
        }

        return matchingDoctors;
    }

    private List<MedicalCase> findMatchingCasesForDoctor(
            Doctor doctor,
            List<MedicalCase> cases,
            Map<String, MedicalSpecialty> specialtyMap) {

        List<MedicalCase> matchingCases = new ArrayList<>();

        if (doctor.specialties() == null || doctor.specialties().isEmpty()) {
            return matchingCases;
        }

        Set<String> doctorIcd10Ranges = new HashSet<>();
        for (String specialtyName : doctor.specialties()) {
            MedicalSpecialty specialty = specialtyMap.get(specialtyName);
            if (specialty != null && specialty.icd10CodeRanges() != null) {
                doctorIcd10Ranges.addAll(specialty.icd10CodeRanges());
            }
        }

        for (MedicalCase medicalCase : cases) {
            if (medicalCase.icd10Codes() != null && !medicalCase.icd10Codes().isEmpty()) {
                for (String icd10Code : medicalCase.icd10Codes()) {
                    if (matchesIcd10Range(icd10Code, doctorIcd10Ranges)) {
                        matchingCases.add(medicalCase);
                        break;
                    }
                }
            }
        }

        return matchingCases;
    }

    private boolean matchesIcd10Range(String icd10Code, Set<String> ranges) {
        if (icd10Code == null || icd10Code.isEmpty() || ranges == null || ranges.isEmpty()) {
            return false;
        }

        if (icd10Code.length() < 3) {
            return false;
        }

        String prefix = icd10Code.substring(0, 1);
        String numericPart = icd10Code.substring(1).replace(".", "");
        int codeValue;

        try {
            if (numericPart.length() >= 2) {
                codeValue = Integer.parseInt(numericPart.substring(0, Math.min(3, numericPart.length())));
            } else {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }

        for (String range : ranges) {
            if (range.contains("-")) {
                String[] parts = range.split("-");
                if (parts.length == 2) {
                    String startStr = parts[0].trim();
                    String endStr = parts[1].trim();

                    if (startStr.length() >= 3 && endStr.length() >= 3) {
                        String startPrefix = startStr.substring(0, 1);
                        String endPrefix = endStr.substring(0, 1);

                        if (prefix.equals(startPrefix) && prefix.equals(endPrefix)) {
                            try {
                                int startValue = Integer.parseInt(startStr.substring(1, Math.min(4, startStr.length())));
                                int endValue = Integer.parseInt(endStr.substring(1, Math.min(4, endStr.length())));

                                if (codeValue >= startValue && codeValue <= endValue) {
                                    return true;
                                }
                            } catch (NumberFormatException e) {
                                // Continue to next range
                            }
                        }
                    }
                }
            } else {
                if (range.length() >= 1 && icd10Code.startsWith(range.substring(0, 1))) {
                    if (range.length() == 1) {
                        return true;
                    }
                    if (icd10Code.startsWith(range)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private List<String> getProceduresForSpecialty(Doctor doctor, Map<String, List<String>> specialtyProceduresMap,
                                                   List<String> extendedProcedures) {
        if (doctor.specialties() == null || doctor.specialties().isEmpty()) {
            return extendedProcedures;
        }

        Set<String> relevantProcedures = new HashSet<>();
        for (String specialtyName : doctor.specialties()) {
            List<String> procedures = specialtyProceduresMap.get(specialtyName);
            if (procedures != null) {
                relevantProcedures.addAll(procedures);
            }
        }

        if (!relevantProcedures.isEmpty()) {
            relevantProcedures.addAll(List.of("Consultation", "Physical Examination", "Blood Test",
                    "Medication Adjustment", "Follow-up Visit"));
            return new ArrayList<>(relevantProcedures);
        }

        return extendedProcedures;
    }

    private ClinicalExperience createClinicalExperience(Doctor doctor, MedicalCase medicalCase,
                                                        List<String> complexityLevels, List<String> outcomes,
                                                        List<String> complications,
                                                        Map<String, List<String>> specialtyProceduresMap,
                                                        List<String> extendedProcedures) {
        String experienceId = IdGenerator.generateId();

        List<String> specialtyProcedures = getProceduresForSpecialty(doctor, specialtyProceduresMap, extendedProcedures);

        List<String> procedures = new ArrayList<>();
        int procedureCount = random.nextInt(3) + 1;
        Set<String> selectedProcedures = new HashSet<>();

        List<String> availableProcedures = new ArrayList<>(specialtyProcedures);
        if (availableProcedures.isEmpty()) {
            availableProcedures = extendedProcedures;
        }

        while (selectedProcedures.size() < procedureCount && selectedProcedures.size() < availableProcedures.size()) {
            selectedProcedures.add(availableProcedures.get(random.nextInt(availableProcedures.size())));
        }
        procedures.addAll(selectedProcedures);

        String complexityLevel = complexityLevels.get(random.nextInt(complexityLevels.size()));
        String outcome = outcomes.get(random.nextInt(outcomes.size()));

        List<String> complicationList = new ArrayList<>();
        if (random.nextDouble() < COMPLICATION_PROBABILITY && !complications.isEmpty()) {
            int complicationCount = Math.min(random.nextInt(MAX_COMPLICATIONS) + MIN_COMPLICATIONS, complications.size());
            Set<String> selectedComplications = new HashSet<>();
            while (selectedComplications.size() < complicationCount && selectedComplications.size() < complications.size()) {
                selectedComplications.add(complications.get(random.nextInt(complications.size())));
            }
            complicationList.addAll(selectedComplications);
        }

        int timeToResolution = random.nextInt(MAX_TIME_TO_RESOLUTION_DAYS) + MIN_TIME_TO_RESOLUTION_DAYS;
        int rating = random.nextInt(MAX_RATING) + MIN_RATING;

        return new ClinicalExperience(
                experienceId,
                doctor.id(),
                medicalCase.id(),
                procedures,
                complexityLevel,
                outcome,
                complicationList,
                timeToResolution,
                rating
        );
    }

    /**
     * Filters a list to the specified count, preferring the newest entities
     * (those with highest IDs, assuming IDs are sequentially generated).
     *
     * <p>This method ensures that when running the synthetic data generator multiple times,
     * the newly generated entities (with higher IDs) are selected for linking.
     *
     * @param entities List of entities to filter
     * @param count    Target count
     * @param <T>      Entity type (must have an id() method)
     * @return Filtered list of entities
     */
    private <T> List<T> filterToCount(List<T> entities, int count) {
        if (entities.size() <= count) {
            return entities;
        }

        // Take the last 'count' entities (newest)
        return entities.subList(entities.size() - count, entities.size());
    }
}
