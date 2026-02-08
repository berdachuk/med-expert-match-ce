package com.berdachuk.medexpertmatch.medicalcase.repository;

import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for medical case operations.
 */
public interface MedicalCaseRepository {

    /**
     * Finds a medical case by its unique identifier.
     *
     * @param caseId The unique identifier of the medical case (24-character hex string)
     * @return Optional containing the medical case if found, empty otherwise
     */
    Optional<MedicalCase> findById(String caseId);

    /**
     * Finds multiple medical cases by their unique identifiers.
     *
     * @param caseIds List of medical case unique identifiers
     * @return List of medical cases found, empty list if none found
     */
    List<MedicalCase> findByIds(List<String> caseIds);

    /**
     * Finds medical cases by urgency level.
     *
     * @param urgencyLevel The urgency level to filter by
     * @param maxResults   Maximum number of results to return
     * @return List of medical cases with the specified urgency level
     */
    List<MedicalCase> findByUrgencyLevel(String urgencyLevel, int maxResults);

    /**
     * Finds medical cases by case type.
     *
     * @param caseType   The case type to filter by
     * @param maxResults Maximum number of results to return
     * @return List of medical cases with the specified case type
     */
    List<MedicalCase> findByCaseType(String caseType, int maxResults);

    /**
     * Finds medical cases by required specialty.
     *
     * @param specialty  The required specialty to filter by
     * @param maxResults Maximum number of results to return
     * @return List of medical cases requiring the specified specialty
     */
    List<MedicalCase> findByRequiredSpecialty(String specialty, int maxResults);

    /**
     * Finds medical cases by ICD-10 code.
     *
     * @param icd10Code  The ICD-10 code to filter by
     * @param maxResults Maximum number of results to return
     * @return List of medical cases with the specified ICD-10 code
     */
    List<MedicalCase> findByIcd10Code(String icd10Code, int maxResults);

    /**
     * Inserts a new medical case.
     *
     * @param medicalCase Medical case entity to insert
     * @return The inserted medical case ID
     * @throws org.springframework.dao.DataIntegrityViolationException if medical case with same ID already exists
     */
    String insert(MedicalCase medicalCase);

    /**
     * Updates an existing medical case.
     *
     * @param medicalCase Medical case entity to update
     * @return The updated medical case ID
     * @throws org.springframework.dao.EmptyResultDataAccessException if medical case does not exist
     */
    String update(MedicalCase medicalCase);

    /**
     * Batch inserts multiple medical cases.
     *
     * @param medicalCases List of medical case entities to insert
     * @return List of inserted medical case IDs
     * @throws org.springframework.dao.DataIntegrityViolationException if any medical case with same ID already exists
     */
    List<String> insertBatch(List<MedicalCase> medicalCases);

    /**
     * Batch updates multiple medical cases.
     *
     * @param medicalCases List of medical case entities to update
     * @return List of updated medical case IDs
     * @throws org.springframework.dao.EmptyResultDataAccessException if any medical case does not exist
     */
    List<String> updateBatch(List<MedicalCase> medicalCases);

    /**
     * Finds all medical case IDs, optionally limited by count.
     *
     * @param limit Maximum number of IDs to return (0 or negative for no limit)
     * @return List of medical case IDs
     */
    List<String> findAllIds(int limit);

    /**
     * Deletes all medical case records.
     * Warning: This is a destructive operation.
     *
     * @return Number of records deleted
     */
    int deleteAll();

    /**
     * Finds medical cases that don't have embeddings.
     *
     * @return List of medical cases without embeddings
     */
    List<MedicalCase> findWithoutEmbeddings();

    /**
     * Finds medical cases that don't have descriptions.
     *
     * @return List of medical cases without descriptions
     */
    List<MedicalCase> findWithoutDescriptions();

    /**
     * Updates the abstract for a medical case.
     *
     * @param caseId       Medical case ID
     * @param abstractText Comprehensive medical case abstract
     */
    void updateAbstract(String caseId, String abstractText);

    /**
     * Updates the embedding for a medical case.
     *
     * @param caseId    The medical case ID
     * @param embedding The embedding vector as a list of Double values
     * @param dimension The dimension of the embedding vector
     */
    void updateEmbedding(String caseId, List<Double> embedding, int dimension);

    /**
     * Searches medical cases by text query and optional filters.
     *
     * @param query        Text query to search in chiefComplaint, symptoms, additionalNotes (optional)
     * @param specialty    Filter by required specialty (optional)
     * @param urgencyLevel Filter by urgency level (optional)
     * @param caseId       Filter by exact case ID (optional)
     * @param offset       Number of records to skip
     * @param maxResults   Maximum number of results to return
     * @return List of matching medical cases
     */
    List<MedicalCase> search(String query, String specialty, String urgencyLevel, String caseId, int offset,
                             int maxResults);

    /**
     * Finds medical cases with pagination support.
     *
     * @param offset Number of records to skip
     * @param limit  Maximum number of records to return
     * @return List of medical cases
     */
    List<MedicalCase> findAllPaginated(int offset, int limit);

    /**
     * Checks if a medical case has an embedding.
     *
     * @param caseId The medical case ID to check
     * @return true if the medical case has an embedding, false otherwise
     */
    boolean hasEmbedding(String caseId);

    /**
     * Calculates the average cosine similarity between a query case and a list of doctor case IDs.
     *
     * @param queryCaseId   The ID of the query medical case
     * @param doctorCaseIds The list of doctor case IDs to compare against
     * @return The average cosine similarity, or null if no valid comparisons could be made
     */
    Double calculateVectorSimilarity(String queryCaseId, List<String> doctorCaseIds);
}
