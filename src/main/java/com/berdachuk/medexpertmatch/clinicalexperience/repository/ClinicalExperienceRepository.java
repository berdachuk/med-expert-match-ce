package com.berdachuk.medexpertmatch.clinicalexperience.repository;

import com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for clinical experience operations.
 */
public interface ClinicalExperienceRepository {

    /**
     * Finds a clinical experience by its unique identifier.
     *
     * @param experienceId The unique identifier of the clinical experience (24-character hex string)
     * @return Optional containing the clinical experience if found, empty otherwise
     */
    Optional<ClinicalExperience> findById(String experienceId);

    /**
     * Finds clinical experiences by doctor ID.
     *
     * @param doctorId The doctor ID to filter by
     * @return List of clinical experiences for the doctor
     */
    List<ClinicalExperience> findByDoctorId(String doctorId);

    /**
     * Finds clinical experiences by case ID.
     *
     * @param caseId The case ID to filter by
     * @return List of clinical experiences for the case
     */
    List<ClinicalExperience> findByCaseId(String caseId);

    /**
     * Batch loads clinical experiences for multiple doctors.
     * Returns a map where key is doctor ID and value is list of clinical experiences.
     * This prevents N+1 query problems when loading experiences for multiple doctors.
     *
     * @param doctorIds List of doctor IDs
     * @return Map of doctor ID to list of clinical experiences
     */
    Map<String, List<ClinicalExperience>> findByDoctorIds(List<String> doctorIds);

    /**
     * Batch loads clinical experiences for multiple cases.
     * Returns a map where key is case ID and value is list of clinical experiences.
     * This prevents N+1 query problems when loading experiences for multiple cases.
     *
     * @param caseIds List of case IDs
     * @return Map of case ID to list of clinical experiences
     */
    Map<String, List<ClinicalExperience>> findByCaseIds(List<String> caseIds);

    /**
     * Inserts a new clinical experience.
     *
     * @param clinicalExperience Clinical experience entity to insert
     * @return The inserted clinical experience ID
     * @throws org.springframework.dao.DataIntegrityViolationException if clinical experience with same ID already exists
     */
    String insert(ClinicalExperience clinicalExperience);

    /**
     * Updates an existing clinical experience.
     *
     * @param clinicalExperience Clinical experience entity to update
     * @return The updated clinical experience ID
     * @throws org.springframework.dao.EmptyResultDataAccessException if clinical experience does not exist
     */
    String update(ClinicalExperience clinicalExperience);

    /**
     * Batch inserts multiple clinical experiences.
     *
     * @param clinicalExperiences List of clinical experience entities to insert
     * @return List of inserted clinical experience IDs
     * @throws org.springframework.dao.DataIntegrityViolationException if any clinical experience with same ID already exists
     */
    List<String> insertBatch(List<ClinicalExperience> clinicalExperiences);

    /**
     * Batch updates multiple clinical experiences.
     *
     * @param clinicalExperiences List of clinical experience entities to update
     * @return List of updated clinical experience IDs
     * @throws org.springframework.dao.EmptyResultDataAccessException if any clinical experience does not exist
     */
    List<String> updateBatch(List<ClinicalExperience> clinicalExperiences);

    /**
     * Finds existing clinical experience IDs from the provided list.
     * Used for batch existence checking to avoid N+1 queries.
     *
     * @param experienceIds List of clinical experience IDs to check
     * @return Set of existing clinical experience IDs
     */
    Set<String> findExistingIds(List<String> experienceIds);

    /**
     * Deletes all clinical experience records.
     * Warning: This is a destructive operation.
     *
     * @return Number of records deleted
     */
    int deleteAll();
}
