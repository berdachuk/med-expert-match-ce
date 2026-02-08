package com.berdachuk.medexpertmatch.facility.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Facility domain entity.
 * Represents a healthcare facility or organization in the system.
 * <p>
 * Facility IDs are external system IDs (19-digit numeric strings) stored as VARCHAR(74).
 */
public record Facility(
        String id,                    // VARCHAR(74) - external system ID
        String name,
        String facilityType,          // 'ACADEMIC', 'COMMUNITY', 'SPECIALTY_CENTER', etc.
        String locationCity,
        String locationState,
        String locationCountry,
        BigDecimal locationLatitude,
        BigDecimal locationLongitude,
        List<String> capabilities,     // Array of capabilities (e.g., 'PCI', 'ECMO', 'ICU', 'SURGERY')
        Integer capacity,              // Total capacity
        Integer currentOccupancy      // Current occupancy
) {
}
