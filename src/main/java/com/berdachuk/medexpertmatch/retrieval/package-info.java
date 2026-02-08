/**
 * Retrieval module - Hybrid GraphRAG retrieval (vector + graph + keyword + reranking).
 * <p>
 * This module provides:
 * - Matching service for doctor-case matching
 * - Semantic Graph Retrieval service combining multiple signals
 * - Priority scoring for consultation queues
 * - Route scoring for facility routing
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"core", "core :: exception", "core :: repository.sql", "core :: service", "core :: util", "medicalcase", "medicalcase :: domain", "medicalcase :: repository", "clinicalexperience", "clinicalexperience :: domain", "clinicalexperience :: repository", "doctor", "doctor :: domain", "doctor :: repository", "embedding", "embedding :: service", "evidence", "facility", "facility :: domain", "facility :: repository", "graph", "graph :: service", "medicalcoding", "medicalcoding :: domain"})
package com.berdachuk.medexpertmatch.retrieval;
