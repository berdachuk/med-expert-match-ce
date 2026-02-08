package com.berdachuk.medexpertmatch.retrieval.service;

import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.facility.domain.Facility;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.retrieval.domain.PriorityScore;
import com.berdachuk.medexpertmatch.retrieval.domain.RouteScoreResult;
import com.berdachuk.medexpertmatch.retrieval.domain.ScoreResult;

/**
 * Semantic Graph Retrieval service.
 * Combines vector embeddings, graph relationships, and historical performance for scoring.
 */
public interface SemanticGraphRetrievalService {

    /**
     * Scores doctor-case match using vector similarity, graph relationships, historical performance.
     *
     * @param medicalCase Medical case to match
     * @param doctor      Doctor to score
     * @return ScoreResult with overall score and component scores
     */
    ScoreResult score(MedicalCase medicalCase, Doctor doctor);

    /**
     * Scores facility-case routing using complexity, outcomes, capacity, proximity.
     *
     * @param medicalCase Medical case to route
     * @param facility    Facility to score
     * @return RouteScoreResult with overall route score and component scores
     */
    RouteScoreResult semanticGraphRetrievalRouteScore(MedicalCase medicalCase, Facility facility);

    /**
     * Computes priority score combining urgency, complexity, and physician availability.
     *
     * @param medicalCase Medical case to prioritize
     * @return PriorityScore with overall priority score and component scores
     */
    PriorityScore computePriorityScore(MedicalCase medicalCase);
}
