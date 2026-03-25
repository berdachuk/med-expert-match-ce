/**
 * Ingestion module - Data ingestion (medical cases, doctor profiles).
 * <p>
 * This module provides:
 * - FHIR adapters for converting FHIR Bundles to internal entities
 * - Synthetic data generator for test data creation
 * - REST controllers for data ingestion
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"core", "doctor", "medicalcase", "clinicalexperience", "medicalcoding", "facility", "graph", "embedding"})
package com.berdachuk.medexpertmatch.ingestion;
