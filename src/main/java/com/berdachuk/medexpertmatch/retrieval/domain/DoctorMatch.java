package com.berdachuk.medexpertmatch.retrieval.domain;

import com.berdachuk.medexpertmatch.doctor.domain.Doctor;

/**
 * Result of matching a doctor to a medical case.
 */
public record DoctorMatch(
        /**
         * The matched doctor.
         */
        Doctor doctor,

        /**
         * Match score (0-100).
         */
        double matchScore,

        /**
         * Rank in the match results (1 = best match).
         */
        int rank,

        /**
         * Rationale explaining why this doctor was matched.
         */
        String rationale
) {
}
