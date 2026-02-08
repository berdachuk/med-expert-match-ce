package com.berdachuk.medexpertmatch.retrieval.domain;

/**
 * Persisted consultation match (case-doctor match result).
 * Stored in consultation_matches table.
 * id: CHAR(24) internal; caseId: CHAR(24); doctorId: VARCHAR(74) external.
 */
public record ConsultationMatch(
        String id,
        String caseId,
        String doctorId,
        double matchScore,
        String matchRationale,
        int rank,
        String status
) {
}
