/**
 * Ingestion module - Data ingestion (medical cases, doctor profiles).
 * <p>
 * This module provides:
 * - FHIR adapters for converting FHIR Bundles to internal entities
 * - Synthetic data generator for test data creation
 * - REST controllers for data ingestion
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"core", "core :: util", "core :: exception", "doctor", "doctor :: domain", "doctor :: repository", "medicalcase", "medicalcase :: domain", "medicalcase :: repository", "medicalcase :: service", "clinicalexperience", "clinicalexperience :: domain", "clinicalexperience :: repository", "medicalcoding", "medicalcoding :: domain", "medicalcoding :: repository", "facility", "facility :: domain", "facility :: repository", "graph", "graph :: service", "embedding", "embedding :: service"})
package com.berdachuk.medexpertmatch.ingestion;
