package com.berdachuk.medexpertmatch.clinicalexperience.domain;

import java.util.List;

/**
 * Clinical experience domain entity.
 * Links doctors to cases with outcomes and metrics.
 * <p>
 * Clinical experience IDs are internal MongoDB-compatible IDs (CHAR(24)).
 * doctor_id: External system ID (VARCHAR(74)) - supports UUID, 19-digit numeric, or other formats
 * case_id: Internal ID (CHAR(24))
 */
public record ClinicalExperience(
        String id,                    // CHAR(24) - internal MongoDB-compatible ID
        String doctorId,
        // VARCHAR(74) - external system doctor ID (UUID, 19-digit numeric, or other format)
        String caseId,                 // CHAR(24) - internal case ID
        List<String> proceduresPerformed,
        String complexityLevel,        // LOW, MEDIUM, HIGH, CRITICAL
        String outcome,                // SUCCESS, IMPROVED, STABLE, COMPLICATED, etc.
        List<String> complications,
        Integer timeToResolution,      // Days
        Integer rating                 // 1-5
) {
}
