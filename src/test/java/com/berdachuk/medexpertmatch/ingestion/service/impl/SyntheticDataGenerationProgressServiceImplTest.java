package com.berdachuk.medexpertmatch.ingestion.service.impl;

import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataGenerationProgressService;
import com.berdachuk.medexpertmatch.ingestion.syntheticdata.domain.RunSummary;
import com.berdachuk.medexpertmatch.ingestion.syntheticdata.domain.SyntheticDataGenerationRun;
import com.berdachuk.medexpertmatch.ingestion.syntheticdata.repository.SyntheticDataGenerationRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SyntheticDataGenerationProgressServiceImplTest {

    private SyntheticDataGenerationRunRepository runRepository;
    private SyntheticDataGenerationProgressService progressService;

    @BeforeEach
    void setUp() {
        runRepository = mock(SyntheticDataGenerationRunRepository.class);
        progressService = new SyntheticDataGenerationProgressService(runRepository);
    }

    @Test
    void getRecentRunsBySize_returnsLatestPerSize() {
        LocalDateTime t1 = LocalDateTime.of(2026, 6, 9, 10, 0, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 6, 10, 10, 0, 0);
        when(runRepository.findAll()).thenReturn(List.of(
                new SyntheticDataGenerationRun("a", "large", 500, 10000, t1, t1.plusMinutes(2), 120000L, 0L, 0L, 0L, 0L, null),
                new SyntheticDataGenerationRun("b", "large", 500, 10000, t2, t2.plusMinutes(1), 60000L, 0L, 0L, 0L, 0L, null)
        ));

        Map<String, List<RunSummary>> result = progressService.getRecentRunsBySize();

        assertTrue(result.containsKey("large"));
        assertEquals(2, result.get("large").size());
        assertEquals(t2, result.get("large").getFirst().startTime());
    }

    @Test
    void getRecentRunsBySize_emptyWhenNoRuns() {
        when(runRepository.findAll()).thenReturn(List.of());

        Map<String, List<RunSummary>> result = progressService.getRecentRunsBySize();

        assertTrue(result.isEmpty());
    }
}