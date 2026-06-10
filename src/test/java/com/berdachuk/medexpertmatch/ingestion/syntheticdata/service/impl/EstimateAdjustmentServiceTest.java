package com.berdachuk.medexpertmatch.ingestion.syntheticdata.service.impl;

import com.berdachuk.medexpertmatch.ingestion.syntheticdata.config.EstimateAdjustmentProperties;
import com.berdachuk.medexpertmatch.ingestion.syntheticdata.domain.SyntheticDataGenerationRun;
import com.berdachuk.medexpertmatch.ingestion.syntheticdata.repository.SyntheticDataGenerationRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EstimateAdjustmentServiceTest {

    private SyntheticDataGenerationRunRepository runRepository;
    private EstimateAdjustmentProperties properties;
    private EstimateAdjustmentServiceImpl service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        runRepository = mock(SyntheticDataGenerationRunRepository.class);
        properties = new EstimateAdjustmentProperties();
        properties.setEnabled(true);
        properties.setSafetyMarginMultiplier(1.5);
        properties.setMaxMinutes(60);
        service = new EstimateAdjustmentServiceImpl(runRepository, properties);
    }

    @Test
    void adjustEstimates_updatesMinutesForLarge() {
        SyntheticDataGenerationRun latestRun = new SyntheticDataGenerationRun(
                "abc123abc123abc123abc123", "large", 500, 10000,
                LocalDateTime.now(), LocalDateTime.now(), 95000L, 0L, 0L, 0L, 0L, null);
        when(runRepository.findLatestBySize("large", 1)).thenReturn(List.of(latestRun));
        when(runRepository.findLatestBySize(anyString(), eq(1))).thenAnswer(inv -> {
            String size = inv.getArgument(0);
            if ("large".equals(size)) return List.of(latestRun);
            return List.of();
        });

        Map<String, Integer> adjustments = service.adjustEstimates();

        assertTrue(adjustments.containsKey("large"));
        assertEquals(3, adjustments.get("large"));
    }

    @Test
    void adjustEstimates_noRuns_noChange() {
        when(runRepository.findLatestBySize(anyString(), eq(1))).thenReturn(List.of());

        Map<String, Integer> adjustments = service.adjustEstimates();

        assertTrue(adjustments.isEmpty());
    }

    @Test
    void adjustEstimates_cappedAtMaxMinutes() {
        SyntheticDataGenerationRun latestRun = new SyntheticDataGenerationRun(
                "abc123abc123abc123abc123", "very_large", 1000, 20000,
                LocalDateTime.now(), LocalDateTime.now(), 3600000L, 0L, 0L, 0L, 0L, null);
        when(runRepository.findLatestBySize(anyString(), eq(1))).thenAnswer(inv -> {
            String size = inv.getArgument(0);
            if ("very_large".equals(size)) return List.of(latestRun);
            return List.of();
        });

        Map<String, Integer> adjustments = service.adjustEstimates();

        assertTrue(adjustments.containsKey("very_large"));
        assertEquals(60, adjustments.get("very_large"));
    }
}