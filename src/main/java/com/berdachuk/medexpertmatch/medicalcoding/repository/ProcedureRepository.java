package com.berdachuk.medexpertmatch.medicalcoding.repository;

import com.berdachuk.medexpertmatch.medicalcoding.domain.Procedure;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for medical procedure operations.
 */
public interface ProcedureRepository {

    /**
     * Finds a procedure by its unique identifier.
     *
     * @param id The unique identifier of the procedure (24-character hex string)
     * @return Optional containing the procedure if found, empty otherwise
     */
    Optional<Procedure> findById(String id);

    /**
     * Finds a procedure by its name (case-insensitive).
     *
     * @param name The procedure name
     * @return Optional containing the procedure if found, empty otherwise
     */
    Optional<Procedure> findByName(String name);

    /**
     * Finds a procedure by its normalized name.
     *
     * @param normalizedName The normalized procedure name
     * @return Optional containing the procedure if found, empty otherwise
     */
    Optional<Procedure> findByNormalizedName(String normalizedName);

    /**
     * Finds procedures by category.
     *
     * @param category The category to filter by
     * @return List of procedures in the specified category
     */
    List<Procedure> findByCategory(String category);

    /**
     * Finds all procedures.
     *
     * @return List of all procedures
     */
    List<Procedure> findAll();

    /**
     * Inserts a new procedure.
     *
     * @param procedure Procedure entity to insert
     * @return The inserted procedure ID
     * @throws org.springframework.dao.DataIntegrityViolationException if procedure with same ID or name already exists
     */
    String insert(Procedure procedure);

    /**
     * Updates an existing procedure.
     *
     * @param procedure Procedure entity to update
     * @return The updated procedure ID
     * @throws org.springframework.dao.EmptyResultDataAccessException if procedure does not exist
     */
    String update(Procedure procedure);

    /**
     * Deletes all procedure records.
     * Warning: This is a destructive operation.
     *
     * @return Number of records deleted
     */
    int deleteAll();
}
