package com.berdachuk.medexpertmatch.ingestion.syntheticdata.domain;

import java.time.LocalDateTime;

public record SyntheticDataGenerationRun(
        String id,
        String size,
        int doctorCount,
        int caseCount,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long totalDurationMs,
        Long descriptionMs,
        Long embeddingMs,
        Long clinicalExperienceMs,
        Long graphBuildMs,
        String errorMessage
) {
}