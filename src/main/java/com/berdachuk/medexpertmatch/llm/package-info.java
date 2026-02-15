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
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"core", "core :: exception", "core :: service", "core :: util", "evidence", "evidence :: domain", "evidence :: service", "doctor", "doctor :: domain", "doctor :: repository", "facility", "facility :: domain", "facility :: repository", "medicalcase", "medicalcase :: domain", "medicalcase :: repository", "medicalcase :: service", "clinicalexperience", "clinicalexperience :: domain", "clinicalexperience :: repository", "graph", "graph :: service", "retrieval", "retrieval :: domain", "retrieval :: service", "caseanalysis", "caseanalysis :: domain", "caseanalysis :: service", "medicalcoding", "medicalcoding :: domain", "embedding", "embedding :: service", "web"})
package com.berdachuk.medexpertmatch.llm;
