package com.berdachuk.medexpertmatch.ingestion.syntheticdata.rest;

import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataGenerationProgressService;
import com.berdachuk.medexpertmatch.ingestion.syntheticdata.domain.RunSummary;
import com.berdachuk.medexpertmatch.ingestion.syntheticdata.domain.SyntheticDataGenerationRun;
import com.berdachuk.medexpertmatch.ingestion.syntheticdata.repository.SyntheticDataGenerationRunRepository;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SyntheticDataControllerStateIT extends BaseIntegrationTest {

    @Autowired
    private SyntheticDataGenerationRunRepository runRepository;

    @Autowired
    private SyntheticDataGenerationProgressService progressService;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @BeforeEach
    void setUp() {
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.synthetic_data_generation_runs");
    }

    @Test
    void progressService_includesRecentRunsBySize() {
        LocalDateTime t1 = LocalDateTime.of(2026, 6, 9, 10, 0, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 6, 10, 10, 0, 0);
        runRepository.insert(new SyntheticDataGenerationRun(
                IdGenerator.generateId(), "large", 500, 10000,
                t1, t1.plusMinutes(2), 120000L, 0L, 0L, 0L, 0L, null));
        runRepository.insert(new SyntheticDataGenerationRun(
                IdGenerator.generateId(), "large", 500, 10000,
                t2, t2.plusMinutes(1), 60000L, 0L, 0L, 0L, 0L, null));

        Map<String, List<RunSummary>> recentRuns = progressService.getRecentRunsBySize();

        assertTrue(recentRuns.containsKey("large"));
        assertEquals(2, recentRuns.get("large").size());
        assertEquals(t2, recentRuns.get("large").getFirst().startTime());
        assertEquals(60000L, recentRuns.get("large").getFirst().totalDurationMs());
    }

    @Test
    void progressService_noRuns_returnsEmpty() {
        Map<String, List<RunSummary>> recentRuns = progressService.getRecentRunsBySize();
        assertTrue(recentRuns.isEmpty());
    }
}