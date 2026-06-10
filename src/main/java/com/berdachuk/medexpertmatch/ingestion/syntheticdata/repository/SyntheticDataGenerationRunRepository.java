package com.berdachuk.medexpertmatch.ingestion.syntheticdata.repository;

import com.berdachuk.medexpertmatch.ingestion.syntheticdata.domain.SyntheticDataGenerationRun;

import java.util.List;

public interface SyntheticDataGenerationRunRepository {

    String insert(SyntheticDataGenerationRun run);

    void update(SyntheticDataGenerationRun run);

    List<SyntheticDataGenerationRun> findLatestBySize(String size, int limit);

    List<SyntheticDataGenerationRun> findAll();
}