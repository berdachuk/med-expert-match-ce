package com.berdachuk.medexpertmatch.medicalcoding.domain;

/**
 * Medical procedure domain entity.
 * Represents a medical procedure, test, or intervention.
 * <p>
 * Procedure IDs are internal MongoDB-compatible IDs (CHAR(24)).
 */
public record Procedure(
        String id,                    // CHAR(24) - internal MongoDB-compatible ID
        String name,                  // Procedure name (e.g., "Echocardiogram", "CT Scan")
        String normalizedName,        // Normalized name for matching
        String description,           // Optional description
        String category               // Optional category (e.g., "Diagnostic", "Therapeutic")
) {
}
