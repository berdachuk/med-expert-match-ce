package com.berdachuk.medexpertmatch.doctor.repository;

import com.berdachuk.medexpertmatch.doctor.domain.MedicalSpecialty;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for medical specialty operations.
 */
public interface MedicalSpecialtyRepository {

    /**
     * Finds a medical specialty by its unique identifier.
     *
     * @param specialtyId The unique identifier of the medical specialty (24-character hex string)
     * @return Optional containing the medical specialty if found, empty otherwise
     */
    Optional<MedicalSpecialty> findById(String specialtyId);

    /**
     * Finds a medical specialty by its name.
     *
     * @param name The name of the medical specialty
     * @return Optional containing the medical specialty if found, empty otherwise
     */
    Optional<MedicalSpecialty> findByName(String name);

    /**
     * Finds all medical specialties.
     *
     * @return List of all medical specialties
     */
    List<MedicalSpecialty> findAll();

    /**
     * Inserts a new medical specialty.
     *
     * @param specialty Medical specialty entity to insert
     * @return The inserted medical specialty ID
     * @throws org.springframework.dao.DataIntegrityViolationException if medical specialty with same ID or name already exists
     */
    String insert(MedicalSpecialty specialty);

    /**
     * Updates an existing medical specialty.
     *
     * @param specialty Medical specialty entity to update
     * @return The updated medical specialty ID
     * @throws org.springframework.dao.EmptyResultDataAccessException if medical specialty does not exist
     */
    String update(MedicalSpecialty specialty);

    /**
     * Deletes all medical specialty records.
     * Warning: This is a destructive operation.
     *
     * @return Number of records deleted
     */
    int deleteAll();
}
