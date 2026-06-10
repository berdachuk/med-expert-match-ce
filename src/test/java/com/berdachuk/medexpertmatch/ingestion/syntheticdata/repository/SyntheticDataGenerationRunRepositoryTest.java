package com.berdachuk.medexpertmatch.ingestion.syntheticdata.repository;

import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.ingestion.syntheticdata.domain.SyntheticDataGenerationRun;
import com.berdachuk.medexpertmatch.ingestion.syntheticdata.repository.impl.SyntheticDataGenerationRunRepositoryImpl;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SyntheticDataGenerationRunRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private SyntheticDataGenerationRunRepository repository;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @BeforeEach
    void setUp() {
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.synthetic_data_generation_runs");
    }

    @Test
    void insert_roundTrips() {
        LocalDateTime startTime = LocalDateTime.of(2026, 6, 10, 10, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 6, 10, 10, 1, 35);
        SyntheticDataGenerationRun run = new SyntheticDataGenerationRun(
                IdGenerator.generateId(), "large", 500, 10000,
                startTime, endTime, 95000L, 60000L, 30000L, 5000L, 500L, null
        );

        String id = repository.insert(run);
        assertNotNull(id);

        List<SyntheticDataGenerationRun> all = repository.findAll();
        assertEquals(1, all.size());
        SyntheticDataGenerationRun stored = all.getFirst();
        assertEquals("large", stored.size());
        assertEquals(500, stored.doctorCount());
        assertEquals(10000, stored.caseCount());
        assertEquals(95000L, stored.totalDurationMs());
        assertEquals(60000L, stored.descriptionMs());
        assertNull(stored.errorMessage());
    }

    @Test
    void findLatestBySize_returnsNewestFirst() {
        LocalDateTime t1 = LocalDateTime.of(2026, 6, 9, 10, 0, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 6, 10, 10, 0, 0);
        repository.insert(new SyntheticDataGenerationRun(
                IdGenerator.generateId(), "large", 500, 10000,
                t1, t1.plusMinutes(2), 120000L, 0L, 0L, 0L, 0L, null));
        repository.insert(new SyntheticDataGenerationRun(
                IdGenerator.generateId(), "large", 500, 10000,
                t2, t2.plusMinutes(1), 60000L, 0L, 0L, 0L, 0L, null));
        repository.insert(new SyntheticDataGenerationRun(
                IdGenerator.generateId(), "small", 50, 1000,
                t2, t2.plusMinutes(1), 30000L, 0L, 0L, 0L, 0L, null));

        List<SyntheticDataGenerationRun> largeRuns = repository.findLatestBySize("large", 5);
        assertEquals(2, largeRuns.size());
        assertEquals(t2, largeRuns.getFirst().startTime());

        List<SyntheticDataGenerationRun> smallRuns = repository.findLatestBySize("small", 5);
        assertEquals(1, smallRuns.size());
    }

    @Test
    void findAll_listsAllRows() {
        repository.insert(new SyntheticDataGenerationRun(
                IdGenerator.generateId(), "tiny", 3, 60,
                LocalDateTime.now(), LocalDateTime.now(), 5000L, 0L, 0L, 0L, 0L, null));
        repository.insert(new SyntheticDataGenerationRun(
                IdGenerator.generateId(), "large", 500, 10000,
                LocalDateTime.now(), LocalDateTime.now(), 95000L, 0L, 0L, 0L, 0L, null));

        List<SyntheticDataGenerationRun> all = repository.findAll();
        assertEquals(2, all.size());
    }
}