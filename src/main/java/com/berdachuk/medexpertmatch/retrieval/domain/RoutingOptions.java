package com.berdachuk.medexpertmatch.retrieval.domain;

import lombok.Builder;

import java.util.List;

/**
 * Options for facility-case routing.
 */
@Builder
public record RoutingOptions(
        /**
         * Maximum number of facility matches to return.
         */
        int maxResults,

        /**
         * Minimum route score threshold (0-100).
         */
        Double minScore,

        /**
         * Preferred facility types (e.g., "ACADEMIC", "SPECIALTY_CENTER").
         */
        List<String> preferredFacilityTypes,

        /**
         * Required capabilities (e.g., "ICU", "SURGERY").
         */
        List<String> requiredCapabilities,

        /**
         * Maximum distance in kilometers (for geographic filtering).
         */
        Double maxDistanceKm
) {
    public RoutingOptions {
        if (maxResults <= 0) {
            maxResults = 5; // Default to 5 results
        }
    }

    public static RoutingOptions defaultOptions() {
        return RoutingOptions.builder()
                .maxResults(5)
                .build();
    }
}
