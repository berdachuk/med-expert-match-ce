package com.berdachuk.medexpertmatch.medicalcoding.domain;

import java.util.List;

/**
 * ICD-10 code domain entity.
 * Represents an ICD-10 code with hierarchy and descriptions.
 * <p>
 * ICD-10 code IDs are internal MongoDB-compatible IDs (CHAR(24)).
 */
public record ICD10Code(
        String id,                    // CHAR(24) - internal MongoDB-compatible ID
        String code,                  // e.g., "I21.9"
        String description,
        String category,               // e.g., "Diseases of the circulatory system"
        String parentCode,            // Parent code in hierarchy
        List<String> relatedCodes     // Array of related ICD-10 codes
) {
}
