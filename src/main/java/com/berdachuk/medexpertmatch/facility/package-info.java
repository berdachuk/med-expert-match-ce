/**
 * Facility module - Medical facility data.
 * <p>
 * This module provides:
 * - Domain entities (Facility)
 * - Repository interfaces and implementations
 * - Facility management
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"core", "core :: repository", "core :: repository.sql"})
package com.berdachuk.medexpertmatch.facility;
