/**
 * Retrieval module - Hybrid GraphRAG retrieval (vector + graph + keyword + reranking).
 * <p>
 * This module provides:
 * - Matching service for doctor-case matching
 * - Semantic Graph Retrieval service combining multiple signals
 * - Priority scoring for consultation queues
 * - Route scoring for facility routing
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"core", "medicalcase", "clinicalexperience", "doctor", "embedding", "evidence", "facility", "graph", "medicalcoding"})
package com.berdachuk.medexpertmatch.retrieval;
