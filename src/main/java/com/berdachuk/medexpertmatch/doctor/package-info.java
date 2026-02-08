/**
 * Doctor module - Doctor/expert data management.
 * <p>
 * This module provides:
 * - Domain entities (Doctor, MedicalSpecialty)
 * - Repository interfaces and implementations
 * - REST controllers for doctor management
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"core", "core :: exception", "core :: repository", "core :: repository.sql"})
package com.berdachuk.medexpertmatch.doctor;
