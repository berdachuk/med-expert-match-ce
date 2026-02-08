package com.berdachuk.medexpertmatch.retrieval.domain;

import lombok.Builder;

import java.util.List;

/**
 * Options for doctor-case matching.
 */
@Builder
public record MatchOptions(
        /**
         * Maximum number of matches to return.
         */
        int maxResults,

        /**
         * Minimum match score threshold (0-100).
         */
        Double minScore,

        /**
         * Preferred specialties (if any).
         */
        List<String> preferredSpecialties,

        /**
         * Require telehealth capability.
         */
        Boolean requireTelehealth,

        /**
         * Preferred facility IDs (if any).
         */
        List<String> preferredFacilityIds
) {
    public MatchOptions {
        if (maxResults <= 0) {
            maxResults = 10; // Default to 10 results
        }
    }

    public static MatchOptions defaultOptions() {
        return MatchOptions.builder()
                .maxResults(10)
                .build();
    }
}
