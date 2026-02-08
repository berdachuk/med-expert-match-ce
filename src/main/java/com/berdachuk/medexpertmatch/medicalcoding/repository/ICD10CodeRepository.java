package com.berdachuk.medexpertmatch.medicalcoding.repository;

import com.berdachuk.medexpertmatch.medicalcoding.domain.ICD10Code;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for ICD-10 code operations.
 */
public interface ICD10CodeRepository {

    /**
     * Finds an ICD-10 code by its unique identifier.
     *
     * @param codeId The unique identifier of the ICD-10 code (24-character hex string)
     * @return Optional containing the ICD-10 code if found, empty otherwise
     */
    Optional<ICD10Code> findById(String codeId);

    /**
     * Finds an ICD-10 code by its code string (e.g., "I21.9").
     *
     * @param code The ICD-10 code string
     * @return Optional containing the ICD-10 code if found, empty otherwise
     */
    Optional<ICD10Code> findByCode(String code);

    /**
     * Finds multiple ICD-10 codes by their code strings.
     *
     * @param codes List of ICD-10 code strings
     * @return List of ICD-10 codes found, empty list if none found
     */
    List<ICD10Code> findByCodes(List<String> codes);

    /**
     * Finds ICD-10 codes by category.
     *
     * @param category   The category to filter by
     * @param maxResults Maximum number of results to return
     * @return List of ICD-10 codes in the specified category
     */
    List<ICD10Code> findByCategory(String category, int maxResults);

    /**
     * Finds ICD-10 codes by parent code.
     *
     * @param parentCode The parent code to filter by
     * @return List of ICD-10 codes with the specified parent code
     */
    List<ICD10Code> findByParentCode(String parentCode);

    /**
     * Finds all ICD-10 codes.
     *
     * @return List of all ICD-10 codes
     */
    List<ICD10Code> findAll();

    /**
     * Inserts a new ICD-10 code.
     *
     * @param icd10Code ICD-10 code entity to insert
     * @return The inserted ICD-10 code ID
     * @throws org.springframework.dao.DataIntegrityViolationException if ICD-10 code with same ID or code already exists
     */
    String insert(ICD10Code icd10Code);

    /**
     * Updates an existing ICD-10 code.
     *
     * @param icd10Code ICD-10 code entity to update
     * @return The updated ICD-10 code ID
     * @throws org.springframework.dao.EmptyResultDataAccessException if ICD-10 code does not exist
     */
    String update(ICD10Code icd10Code);

    /**
     * Batch inserts multiple ICD-10 codes.
     *
     * @param icd10Codes List of ICD-10 code entities to insert
     * @return List of inserted ICD-10 code IDs
     * @throws org.springframework.dao.DataIntegrityViolationException if any ICD-10 code with same ID or code already exists
     */
    List<String> insertBatch(List<ICD10Code> icd10Codes);

    /**
     * Batch updates multiple ICD-10 codes.
     *
     * @param icd10Codes List of ICD-10 code entities to update
     * @return List of updated ICD-10 code IDs
     * @throws org.springframework.dao.EmptyResultDataAccessException if any ICD-10 code does not exist
     */
    List<String> updateBatch(List<ICD10Code> icd10Codes);

    /**
     * Finds existing ICD-10 code strings from the provided list.
     * Used for batch existence checking to avoid N+1 queries.
     *
     * @param codes List of ICD-10 code strings to check
     * @return Set of existing ICD-10 code strings
     */
    Set<String> findExistingCodes(List<String> codes);

    /**
     * Deletes all ICD-10 code records.
     * Warning: This is a destructive operation.
     *
     * @return Number of records deleted
     */
    int deleteAll();
}
