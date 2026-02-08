package com.berdachuk.medexpertmatch.ingestion.service;

import java.util.List;
import java.util.Map;

/**
 * Service for generating clinical experiences linking doctors to cases.
 */
public interface ClinicalExperienceGeneratorService {

    /**
     * Generates clinical experiences linking doctors to cases.
     * Ensures cases are matched to doctors based on their specialties and expertise.
     *
     * @param doctorCount            Number of doctors
     * @param caseCount              Number of cases
     * @param progress               Optional progress tracker
     * @param complexityLevels       List of complexity levels (loaded from CSV)
     * @param outcomes               List of outcomes (loaded from CSV)
     * @param complications          List of complications (loaded from CSV)
     * @param specialtyProceduresMap Map of specialty names to procedure names (loaded from CSV)
     * @param extendedProcedures     Extended list of procedures
     */
    void generateClinicalExperiences(int doctorCount, int caseCount, SyntheticDataGenerationProgress progress,
                                     List<String> complexityLevels, List<String> outcomes, List<String> complications,
                                     Map<String, List<String>> specialtyProceduresMap, List<String> extendedProcedures);
}
