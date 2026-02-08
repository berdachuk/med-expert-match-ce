package com.berdachuk.medexpertmatch.retrieval.domain;

/**
 * Result of scoring a doctor-case match.
 * Combines multiple signals: vector similarity, graph relationships, and historical performance.
 */
public record ScoreResult(
        /**
         * Overall match score (0-100).
         */
        double overallScore,

        /**
         * Vector similarity score component (0-1).
         */
        double vectorSimilarityScore,

        /**
         * Graph relationship score component (0-1).
         */
        double graphRelationshipScore,

        /**
         * Historical performance score component (0-1).
         */
        double historicalPerformanceScore,

        /**
         * Rationale explaining the score.
         */
        String rationale
) {
}
