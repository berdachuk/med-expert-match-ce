package com.berdachuk.medexpertmatch.ingestion.service;

import org.hl7.fhir.r5.model.Bundle;

import java.util.List;
import java.util.Map;

public interface SyntheticDataGenerationService {

    record DataSizeConfig(
            String size,
            int doctorCount,
            int caseCount,
            String description,
            int estimatedTimeMinutes
    ) {
    }

    void generateIcd10Codes(String size, SyntheticDataGenerationProgress progress);

    void generateMedicalSpecialties(String size, SyntheticDataGenerationProgress progress);

    void generateAdditionalProcedures(String size, SyntheticDataGenerationProgress progress);

    void generateFacilities(int count);

    void generateDoctors(int count, SyntheticDataGenerationProgress progress);

    void generateMedicalCases(int count, SyntheticDataGenerationProgress progress);

    List<Bundle> generateFhirBundles(int count);

    void generateClinicalExperiences(int doctorCount, int caseCount, SyntheticDataGenerationProgress progress);

    Map<String, DataSizeConfig> getAvailableSizes();

    int calculateFacilityCount(int doctorCount);
}
