package com.berdachuk.medexpertmatch.doctor.domain;

/**
 * Medical specialty entity.
 * Represents a medical specialty with ICD-10 code ranges.
 */
public record MedicalSpecialty(
        String id,                    // CHAR(24) - internal MongoDB-compatible ID
        String name,
        String normalizedName,         // Normalized for matching
        String description,
        java.util.List<String> icd10CodeRanges,  // e.g., ["I00-I99", "E00-E90"]
        java.util.List<String> relatedSpecialtyIds
) {
}
