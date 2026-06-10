package com.berdachuk.medexpertmatch.ingestion.syntheticdata.domain;

public record RunSummary(
        String size,
        java.time.LocalDateTime startTime,
        Long totalDurationMs
) {
}