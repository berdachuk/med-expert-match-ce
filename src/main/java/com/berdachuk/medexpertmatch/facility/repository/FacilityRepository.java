package com.berdachuk.medexpertmatch.facility.repository;

import com.berdachuk.medexpertmatch.facility.domain.Facility;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for facility operations.
 */
public interface FacilityRepository {

    /**
     * Finds a facility by its unique identifier.
     *
     * @param facilityId The unique identifier of the facility (external system ID: UUID, 19-digit numeric, or other format)
     * @return Optional containing the facility if found, empty otherwise
     */
    Optional<Facility> findById(String facilityId);

    /**
     * Finds all facilities.
     *
     * @return List of all facilities
     */
    List<Facility> findAll();

    /**
     * Finds all facility IDs, optionally limited by count.
     *
     * @param limit Maximum number of IDs to return (0 or negative for no limit)
     * @return List of facility IDs (external system IDs: UUID, 19-digit numeric, or other format)
     */
    List<String> findAllIds(int limit);

    /**
     * Inserts a new facility.
     *
     * @param facility Facility entity to insert
     * @return The inserted facility ID (external system ID: UUID, 19-digit numeric, or other format)
     * @throws org.springframework.dao.DataIntegrityViolationException if facility with same ID already exists
     */
    String insert(Facility facility);

    /**
     * Updates an existing facility.
     *
     * @param facility Facility entity to update
     * @return The updated facility ID (external system ID: UUID, 19-digit numeric, or other format)
     * @throws org.springframework.dao.EmptyResultDataAccessException if facility does not exist
     */
    String update(Facility facility);

    /**
     * Deletes all facility records.
     * Warning: This is a destructive operation.
     *
     * @return Number of records deleted
     */
    int deleteAll();
}
