/**
 * Medical Coding module - ICD-10 codes and medical coding.
 * <p>
 * This module provides:
 * - Domain entities (ICD10Code)
 * - Repository interfaces and implementations
 * - Medical coding utilities
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"core", "core :: repository", "core :: repository.sql"})
package com.berdachuk.medexpertmatch.medicalcoding;
