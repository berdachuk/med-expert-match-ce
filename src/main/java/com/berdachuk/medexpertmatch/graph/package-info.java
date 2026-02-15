/**
 * Graph module - Graph relationship management using Apache AGE.
 * <p>
 * This module provides:
 * - Graph service for Cypher queries on Apache AGE
 * - Medical graph builder service for populating graph relationships
 * - Graph visualization services
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"core", "core :: exception", "core :: health", "doctor", "doctor :: domain", "doctor :: repository", "medicalcase", "medicalcase :: domain", "medicalcase :: repository", "clinicalexperience", "clinicalexperience :: domain", "clinicalexperience :: repository", "medicalcoding", "medicalcoding :: domain", "medicalcoding :: repository", "facility", "facility :: domain", "facility :: repository"})
package com.berdachuk.medexpertmatch.graph;
