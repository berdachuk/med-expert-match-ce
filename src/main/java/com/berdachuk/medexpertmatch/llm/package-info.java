/**
 * LLM module - LLM orchestration and Agent Skills integration.
 * <p>
 * This module provides:
 * - Medical agent service for orchestrating agent skills
 * - Medical agent tools (Java @Tool methods)
 * - REST controllers for agent API endpoints
 * <p>
 * Orchestration services legitimately depend on multiple domain modules to coordinate complex workflows.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"core", "evidence", "doctor", "facility", "medicalcase", "clinicalexperience", "graph", "retrieval", "caseanalysis", "medicalcoding", "embedding", "web"})
package com.berdachuk.medexpertmatch.llm;
