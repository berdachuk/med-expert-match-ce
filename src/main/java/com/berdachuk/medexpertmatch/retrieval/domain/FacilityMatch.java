package com.berdachuk.medexpertmatch.retrieval.domain;

import com.berdachuk.medexpertmatch.facility.domain.Facility;

/**
 * Result of matching a facility for case routing.
 */
public record FacilityMatch(
        /**
         * The matched facility.
         */
        Facility facility,

        /**
         * Route score (0-100).
         */
        double routeScore,

        /**
         * Rank in the match results (1 = best match).
         */
        int rank,

        /**
         * Rationale explaining why this facility was matched.
         */
        String rationale
) {
}
