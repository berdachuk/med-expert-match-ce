package com.berdachuk.medexpertmatch.medicalcase.service;

import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;

/**
 * Service for generating comprehensive medical case descriptions.
 * Uses LLM enhancement with fallback to simple concatenation.
 */
public interface MedicalCaseDescriptionService {
    /**
     * Generates comprehensive description for medical case.
     * Uses LLM enhancement with fallback to simple concatenation.
     *
     * @param medicalCase Medical case to generate description for
     * @return Generated description text
     */
    String generateDescription(MedicalCase medicalCase);

    /**
     * Generates description if not already present, otherwise returns existing.
     *
     * @param medicalCase Medical case (may have abstract already set)
     * @return Description text (existing or newly generated)
     */
    String getOrGenerateDescription(MedicalCase medicalCase);
}
