package com.berdachuk.medexpertmatch.doctor.domain;

import java.util.List;

/**
 * Doctor domain entity.
 * Represents a doctor/specialist in the system.
 * <p>
 * Doctor IDs are external system IDs stored as VARCHAR(74).
 * Supports any external ID format, including:
 * - UUID strings (e.g., "550e8400-e29b-41d4-a716-446655440000")
 * - 19-digit numeric strings (e.g., "8760000000000420950")
 * - Other external system ID formats
 */
public record Doctor(
        String id,                    // VARCHAR(74) - external system ID (UUID, 19-digit numeric, or other format)
        String name,
        String email,
        List<String> specialties,     // Medical specialties
        List<String> certifications,  // Board certifications
        List<String> facilityIds,      // Facility affiliations (external system IDs)
        boolean telehealthEnabled,
        String availabilityStatus
) {
}
