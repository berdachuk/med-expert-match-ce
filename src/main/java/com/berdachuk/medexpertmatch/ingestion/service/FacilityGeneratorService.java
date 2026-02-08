package com.berdachuk.medexpertmatch.ingestion.service;

import java.util.List;

/**
 * Service for generating facilities.
 */
public interface FacilityGeneratorService {

    /**
     * Generates facilities.
     *
     * @param count                Number of facilities to generate
     * @param facilityTypes        List of facility types (loaded from CSV)
     * @param facilityCapabilities List of facility capabilities (loaded from CSV)
     */
    void generateFacilities(int count, List<String> facilityTypes, List<String> facilityCapabilities);
}
