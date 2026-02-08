package com.berdachuk.medexpertmatch.doctor.repository;

import com.berdachuk.medexpertmatch.doctor.domain.Doctor;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for doctor operations.
 */
public interface DoctorRepository {

    /**
     * Finds a doctor by their unique identifier.
     *
     * @param doctorId The unique identifier of the doctor (external system ID: UUID, 19-digit numeric, or other format)
     * @return Optional containing the doctor if found, empty otherwise
     */
    Optional<Doctor> findById(String doctorId);

    /**
     * Finds a doctor by their email address.
     *
     * @param email The email address of the doctor
     * @return Optional containing the doctor if found, empty otherwise
     */
    Optional<Doctor> findByEmail(String email);

    /**
     * Finds multiple doctors by their unique identifiers.
     *
     * @param doctorIds List of doctor unique identifiers (external system IDs: UUID, 19-digit numeric, or other format)
     * @return List of doctors found, empty list if none found
     */
    List<Doctor> findByIds(List<String> doctorIds);

    /**
     * Finds doctor identifiers by name using exact or partial matching.
     *
     * @param name       The name to search for
     * @param maxResults Maximum number of results to return
     * @return List of doctor identifiers (external system IDs: UUID, 19-digit numeric, or other format) matching the name, empty list if none found
     */
    List<String> findDoctorIdsByName(String name, int maxResults);

    /**
     * Finds doctor identifiers by name using similarity matching.
     *
     * @param name                The name to search for
     * @param similarityThreshold Minimum similarity threshold (0.0 to 1.0)
     * @param maxResults          Maximum number of results to return
     * @return List of doctor identifiers (external system IDs: UUID, 19-digit numeric, or other format) matching the name with similarity >= threshold, empty list if none found
     */
    List<String> findDoctorIdsByNameSimilarity(String name, double similarityThreshold, int maxResults);

    /**
     * Finds doctor identifiers by facility affiliation.
     *
     * @param facilityId The facility ID (external system ID)
     * @param limit      Maximum number of IDs to return
     * @return List of doctor IDs affiliated with the facility
     */
    List<String> findDoctorIdsByFacilityId(String facilityId, int limit);

    /**
     * Finds doctors by specialty.
     *
     * @param specialty  The medical specialty to search for
     * @param maxResults Maximum number of results to return
     * @return List of doctors with the specified specialty
     */
    List<Doctor> findBySpecialty(String specialty, int maxResults);

    /**
     * Finds doctors with telehealth capability.
     *
     * @param maxResults Maximum number of results to return
     * @return List of doctors with telehealth enabled
     */
    List<Doctor> findTelehealthEnabled(int maxResults);

    /**
     * Inserts a new doctor.
     *
     * @param doctor Doctor entity to insert
     * @return The inserted doctor ID (external system ID: UUID, 19-digit numeric, or other format)
     * @throws org.springframework.dao.DataIntegrityViolationException if doctor with same ID already exists
     */
    String insert(Doctor doctor);

    /**
     * Updates an existing doctor.
     *
     * @param doctor Doctor entity to update
     * @return The updated doctor ID (external system ID: UUID, 19-digit numeric, or other format)
     * @throws org.springframework.dao.EmptyResultDataAccessException if doctor does not exist
     */
    String update(Doctor doctor);

    /**
     * Batch inserts multiple doctors.
     *
     * @param doctors List of doctor entities to insert
     * @return List of inserted doctor IDs (external system IDs: UUID, 19-digit numeric, or other format)
     * @throws org.springframework.dao.DataIntegrityViolationException if any doctor with same ID already exists
     */
    List<String> insertBatch(List<Doctor> doctors);

    /**
     * Batch updates multiple doctors.
     *
     * @param doctors List of doctor entities to update
     * @return List of updated doctor IDs (external system IDs: UUID, 19-digit numeric, or other format)
     * @throws org.springframework.dao.EmptyResultDataAccessException if any doctor does not exist
     */
    List<String> updateBatch(List<Doctor> doctors);

    /**
     * Finds all doctor IDs, optionally limited by count.
     *
     * @param limit Maximum number of IDs to return (0 or negative for no limit)
     * @return List of doctor IDs (external system IDs: UUID, 19-digit numeric, or other format)
     */
    List<String> findAllIds(int limit);

    /**
     * Deletes all doctor records.
     * Warning: This is a destructive operation.
     *
     * @return Number of records deleted
     */
    int deleteAll();
}
