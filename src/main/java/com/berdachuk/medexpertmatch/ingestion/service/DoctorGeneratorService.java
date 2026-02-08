package com.berdachuk.medexpertmatch.ingestion.service;

import java.util.List;

/**
 * Service for generating doctors.
 */
public interface DoctorGeneratorService {

    /**
     * Generates doctors.
     *
     * @param count                Number of doctors to generate
     * @param progress             Optional progress tracker
     * @param medicalSpecialties   List of medical specialties (loaded from CSV)
     * @param availabilityStatuses List of availability statuses (loaded from CSV)
     */
    void generateDoctors(int count, SyntheticDataGenerationProgress progress,
                         List<String> medicalSpecialties, List<String> availabilityStatuses);
}
