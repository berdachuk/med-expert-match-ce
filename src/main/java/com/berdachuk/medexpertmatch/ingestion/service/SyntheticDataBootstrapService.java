package com.berdachuk.medexpertmatch.ingestion.service;

import java.util.List;
import java.util.Map;

/**
 * Bootstrap service for loading and persisting reference synthetic data.
 */
public interface SyntheticDataBootstrapService {

    /**
     * Loads reference data from CSV files and populates shared state.
     */
    void loadDataFromFiles();

    /**
     * Persists medical specialties from CSV data.
     *
     * @param specialtiesData Medical specialty rows
     */
    void persistMedicalSpecialties(List<Map<String, String>> specialtiesData);

    /**
     * Persists ICD-10 codes from CSV data.
     *
     * @param icd10Data ICD-10 rows
     */
    void persistIcd10Codes(List<Map<String, String>> icd10Data);

    /**
     * Persists procedures from CSV data.
     *
     * @param proceduresData Procedure rows
     */
    void persistProcedures(List<Map<String, String>> proceduresData);
}
