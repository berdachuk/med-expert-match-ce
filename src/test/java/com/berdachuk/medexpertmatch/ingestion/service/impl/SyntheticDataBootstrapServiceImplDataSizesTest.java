package com.berdachuk.medexpertmatch.ingestion.service.impl;

import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataCatalogState;
import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataGenerationService;
import com.berdachuk.medexpertmatch.ingestion.util.CsvDataLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@code data-sizes.csv} parsing path in
 * {@link SyntheticDataBootstrapServiceImpl#loadDataFromFiles()}.
 * <p>
 * M76 motivation: the previous values in the CSV were wildly wrong
 * (e.g. 2,285 minutes for the {@code large} size, when the actual
 * measured run is ~95 s) and there was no {@code very_large} option
 * for operators who wanted to stress-test the matching pipeline. These
 * tests pin the new contract:
 * <ul>
 *   <li>Every size has {@code case_count == 20 × doctor_count} (uniform
 *       20-cases-per-doctor ratio).</li>
 *   <li>Every {@code estimatedTimeMinutes} is between 1 and 60 (regression
 *       guard against the "1 day 14 hours" bug).</li>
 *   <li>The catalog exposes a {@code very_large} size with 1,000 doctors.</li>
 * </ul>
 */
class SyntheticDataBootstrapServiceImplDataSizesTest {

    private static final String DATA_SIZES_FILE = "classpath:/data/data-sizes.csv";

    private ResourceLoader resourceLoader;
    private SyntheticDataCatalogState catalogState;

    @BeforeEach
    void setUp() {
        resourceLoader = new DefaultResourceLoader();
        catalogState = new SyntheticDataCatalogState();
    }

    @Test
    @DisplayName("M76: data-sizes.csv loads at least 5 sizes including very_large")
    void loadsAllFiveSizes() {
        List<Map<String, String>> rows = CsvDataLoader.loadCsv(resourceLoader, DATA_SIZES_FILE, "data sizes");

        // Parse the rows the same way loadDataFromFiles() does, so the
        // test exercises the exact contract the production code consumes.
        for (Map<String, String> row : rows) {
            String size = row.get("size");
            String doctorCount = row.get("doctor_count");
            String caseCount = row.get("case_count");
            String estimatedTime = row.getOrDefault("estimated_time_minutes", "0");
            if (size == null || doctorCount == null || caseCount == null) {
                continue;
            }
            catalogState.getDataSizeConfigs().put(
                    size.toLowerCase(),
                    new SyntheticDataGenerationService.DataSizeConfig(
                            size.toLowerCase(),
                            Integer.parseInt(doctorCount),
                            Integer.parseInt(caseCount),
                            row.getOrDefault("description", size),
                            Integer.parseInt(estimatedTime)));
        }

        assertEquals(8, catalogState.getDataSizeConfigs().size(),
                "Catalog must expose exactly 8 sizes: tiny, micro, mini, compact, small, standard, large, very_large");
        assertNotNull(catalogState.getDataSizeConfigs().get("very_large"),
                "M76: very_large size must be present so operators can stress-test");
    }

    @Test
    @DisplayName("M76: every size follows the 20-cases-per-doctor ratio")
    void everySizeFollows20CasesPerDoctor() {
        List<Map<String, String>> rows = CsvDataLoader.loadCsv(resourceLoader, DATA_SIZES_FILE, "data sizes");
        for (Map<String, String> row : rows) {
            String size = row.get("size");
            String doctorCount = row.get("doctor_count");
            String caseCount = row.get("case_count");
            if (size == null || doctorCount == null || caseCount == null) {
                continue;
            }
            int doctors = Integer.parseInt(doctorCount);
            int cases = Integer.parseInt(caseCount);
            assertEquals(20 * doctors, cases,
                    String.format("Size '%s' must have case_count == 20 × doctor_count (got %d doctors, %d cases)",
                            size, doctors, cases));
        }
    }

    @Test
    @DisplayName("M76: every size has a positive estimate under 60 minutes (regression guard)")
    void everySizeEstimateIsRealistic() {
        List<Map<String, String>> rows = CsvDataLoader.loadCsv(resourceLoader, DATA_SIZES_FILE, "data sizes");
        for (Map<String, String> row : rows) {
            String size = row.get("size");
            String estimatedTime = row.getOrDefault("estimated_time_minutes", "0");
            if (size == null) {
                continue;
            }
            int minutes = Integer.parseInt(estimatedTime);
            assertTrue(minutes > 0,
                    String.format("Size '%s' must have a positive estimate (got %d)", size, minutes));
            assertTrue(minutes < 60,
                    String.format("Size '%s' estimate must be under 60 minutes — pre-M76 estimate of 2,285 minutes was 100x off (M76 regression guard)",
                            size, minutes));
        }
    }

    @Test
    @DisplayName("M76: very_large size exposes 1,000 doctors / 20,000 cases")
    void veryLargeSizeExposesThousandDoctors() {
        List<Map<String, String>> rows = CsvDataLoader.loadCsv(resourceLoader, DATA_SIZES_FILE, "data sizes");
        Map<String, String> veryLarge = null;
        for (Map<String, String> row : rows) {
            if ("very_large".equalsIgnoreCase(row.get("size"))) {
                veryLarge = new HashMap<>(row);
                break;
            }
        }
        assertNotNull(veryLarge, "very_large size must be present in data-sizes.csv");
        assertEquals(1000, Integer.parseInt(veryLarge.get("doctor_count")),
                "very_large must have 1,000 doctors");
        assertEquals(20000, Integer.parseInt(veryLarge.get("case_count")),
                "very_large must have 20,000 cases (20 × 1,000)");
    }
}
