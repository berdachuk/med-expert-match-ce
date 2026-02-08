/**
 * Medical Case module - Medical case data management.
 * <p>
 * This module provides:
 * - Domain entities (MedicalCase, CaseType, UrgencyLevel)
 * - Repository interfaces and implementations
 * - REST controllers for medical case management
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"core", "core :: util", "core :: repository", "core :: repository.sql"})
package com.berdachuk.medexpertmatch.medicalcase;
