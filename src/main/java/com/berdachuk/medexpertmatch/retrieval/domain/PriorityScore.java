package com.berdachuk.medexpertmatch.retrieval.domain;

/**
 * Priority score for consultation queue prioritization.
 * Combines urgency, complexity, and physician availability.
 */
public record PriorityScore(
        /**
         * Overall priority score (0-100).
         */
        double overallScore,

        /**
         * Urgency level score component (0-1).
         */
        double urgencyScore,

        /**
         * Case complexity score component (0-1).
         */
        double complexityScore,

        /**
         * Physician availability score component (0-1).
         */
        double availabilityScore,

        /**
         * Rationale explaining the priority score.
         */
        String rationale
) {
}
