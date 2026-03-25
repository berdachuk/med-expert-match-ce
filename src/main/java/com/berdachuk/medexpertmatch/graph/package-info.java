/**
 * Graph module - Graph relationship management using Apache AGE.
 * <p>
 * This module provides:
 * - Graph service for Cypher queries on Apache AGE
 * - Medical graph builder service for populating graph relationships
 * - Graph visualization services
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"core", "doctor", "medicalcase", "clinicalexperience", "medicalcoding", "facility"})
package com.berdachuk.medexpertmatch.graph;
