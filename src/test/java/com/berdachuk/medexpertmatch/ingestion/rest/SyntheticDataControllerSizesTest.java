package com.berdachuk.medexpertmatch.ingestion.rest;

import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataGenerationProgressService;
import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataGenerationService;
import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SyntheticDataController#getAvailableSizes()}.
 * <p>
 * M76 motivation: the controller passes the {@code data-sizes.csv} catalog
 * to the admin UI and to {@code POST /api/v1/synthetic-data/generate}.
 * Adding the new {@code very_large} size and fixing the estimate numbers
 * must surface in this endpoint, otherwise the dropdown won't show the
 * new option.
 */
class SyntheticDataControllerSizesTest {

    private SyntheticDataGenerator generator;
    private SyntheticDataController controller;

    @BeforeEach
    void setUp() {
        generator = mock(SyntheticDataGenerator.class);
        SyntheticDataGenerationProgressService progressService = mock(SyntheticDataGenerationProgressService.class);
        controller = new SyntheticDataController(generator, progressService);
    }

    @Test
    @DisplayName("M76: GET /api/v1/synthetic-data/sizes exposes all 8 sizes including very_large")
    void getAvailableSizesExposesVeryLarge() {
        // Mimic what loadDataFromFiles() populates into catalogState
        Map<String, SyntheticDataGenerationService.DataSizeConfig> catalog = Map.of(
                "tiny",        new SyntheticDataGenerationService.DataSizeConfig("tiny", 3, 60, "Tiny (3 doctors, 60 cases)", 1),
                "micro",       new SyntheticDataGenerationService.DataSizeConfig("micro", 5, 100, "Micro (5 doctors, 100 cases)", 1),
                "mini",        new SyntheticDataGenerationService.DataSizeConfig("mini", 10, 200, "Mini (10 doctors, 200 cases)", 1),
                "compact",     new SyntheticDataGenerationService.DataSizeConfig("compact", 20, 400, "Compact (20 doctors, 400 cases)", 1),
                "small",       new SyntheticDataGenerationService.DataSizeConfig("small", 50, 1000, "Small (50 doctors, 1,000 cases)", 2),
                "standard",    new SyntheticDataGenerationService.DataSizeConfig("standard", 100, 2000, "Standard (100 doctors, 2,000 cases)", 3),
                "large",       new SyntheticDataGenerationService.DataSizeConfig("large", 500, 10000, "Large (500 doctors, 10,000 cases)", 5),
                "very_large",  new SyntheticDataGenerationService.DataSizeConfig("very_large", 1000, 20000, "Very Large (1,000 doctors, 20,000 cases)", 10)
        );
        when(generator.getAvailableSizes()).thenReturn(catalog);

        ResponseEntity<List<Map<String, Object>>> response = controller.getAvailableSizes();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(8, response.getBody().size(),
                "M76: 8 sizes (tiny, micro, mini, compact, small, standard, large, very_large) must be exposed");

        // Find very_large and assert its shape
        Map<String, Object> veryLarge = response.getBody().stream()
                .filter(row -> "very_large".equals(row.get("size")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("very_large must be in /sizes response"));
        assertEquals(1000, veryLarge.get("doctorCount"));
        assertEquals(20000, veryLarge.get("caseCount"));
        assertEquals(10, veryLarge.get("estimatedTimeMinutes"));
        assertEquals("Very Large (1,000 doctors, 20,000 cases)", veryLarge.get("description"));
    }

    @Test
    @DisplayName("M76: every /sizes entry has case_count == 20 × doctor_count")
    void everySizeEntryFollows20CasesPerDoctor() {
        Map<String, SyntheticDataGenerationService.DataSizeConfig> catalog = Map.of(
                "tiny",        new SyntheticDataGenerationService.DataSizeConfig("tiny", 3, 60, "Tiny", 1),
                "micro",       new SyntheticDataGenerationService.DataSizeConfig("micro", 5, 100, "Micro", 1),
                "mini",        new SyntheticDataGenerationService.DataSizeConfig("mini", 10, 200, "Mini", 1),
                "compact",     new SyntheticDataGenerationService.DataSizeConfig("compact", 20, 400, "Compact", 1),
                "small",       new SyntheticDataGenerationService.DataSizeConfig("small", 50, 1000, "Small", 2),
                "standard",    new SyntheticDataGenerationService.DataSizeConfig("standard", 100, 2000, "Standard", 3),
                "large",       new SyntheticDataGenerationService.DataSizeConfig("large", 500, 10000, "Large", 5),
                "very_large",  new SyntheticDataGenerationService.DataSizeConfig("very_large", 1000, 20000, "Very Large", 10)
        );
        when(generator.getAvailableSizes()).thenReturn(catalog);

        ResponseEntity<List<Map<String, Object>>> response = controller.getAvailableSizes();
        for (Map<String, Object> row : response.getBody()) {
            int doctors = (int) row.get("doctorCount");
            int cases = (int) row.get("caseCount");
            assertEquals(20 * doctors, cases,
                    String.format("Size '%s' must follow 20 cases/doctor (got %d doctors, %d cases)",
                            row.get("size"), doctors, cases));
        }
    }

    @Test
    @DisplayName("M76: every /sizes entry has a realistic time estimate (1-60 minutes)")
    void everySizeEntryEstimateIsRealistic() {
        Map<String, SyntheticDataGenerationService.DataSizeConfig> catalog = Map.of(
                "tiny",        new SyntheticDataGenerationService.DataSizeConfig("tiny", 3, 60, "Tiny", 1),
                "micro",       new SyntheticDataGenerationService.DataSizeConfig("micro", 5, 100, "Micro", 1),
                "mini",        new SyntheticDataGenerationService.DataSizeConfig("mini", 10, 200, "Mini", 1),
                "compact",     new SyntheticDataGenerationService.DataSizeConfig("compact", 20, 400, "Compact", 1),
                "small",       new SyntheticDataGenerationService.DataSizeConfig("small", 50, 1000, "Small", 2),
                "standard",    new SyntheticDataGenerationService.DataSizeConfig("standard", 100, 2000, "Standard", 3),
                "large",       new SyntheticDataGenerationService.DataSizeConfig("large", 500, 10000, "Large", 5),
                "very_large",  new SyntheticDataGenerationService.DataSizeConfig("very_large", 1000, 20000, "Very Large", 10)
        );
        when(generator.getAvailableSizes()).thenReturn(catalog);

        ResponseEntity<List<Map<String, Object>>> response = controller.getAvailableSizes();
        for (Map<String, Object> row : response.getBody()) {
            int estimate = (int) row.get("estimatedTimeMinutes");
            assertTrue(estimate > 0 && estimate < 60,
                    String.format("Size '%s' must have a positive estimate under 60 minutes (got %d)",
                            row.get("size"), estimate));
        }
    }
}
