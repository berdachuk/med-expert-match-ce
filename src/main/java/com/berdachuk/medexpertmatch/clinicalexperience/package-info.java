/**
 * Clinical Experience module - Clinical experience data.
 * <p>
 * This module provides:
 * - Domain entities (ClinicalExperience)
 * - Repository interfaces and implementations
 * - Doctor-case relationship management
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"core", "core :: util", "core :: repository", "core :: repository.sql"})
package com.berdachuk.medexpertmatch.clinicalexperience;
