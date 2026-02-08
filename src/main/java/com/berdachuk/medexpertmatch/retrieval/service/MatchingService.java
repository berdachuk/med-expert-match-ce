package com.berdachuk.medexpertmatch.retrieval.service;

import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;
import com.berdachuk.medexpertmatch.retrieval.domain.FacilityMatch;
import com.berdachuk.medexpertmatch.retrieval.domain.MatchOptions;
import com.berdachuk.medexpertmatch.retrieval.domain.RoutingOptions;

import java.util.List;

/**
 * Service for orchestrating matching logic across multiple services.
 * Matches doctors to medical cases and facilities for case routing.
 */
public interface MatchingService {

    /**
     * Matches doctors to case using vector similarity, graph relationships, historical performance, specialty, telehealth.
     *
     * @param caseId  Medical case ID to match
     * @param options Matching options (max results, filters, etc.)
     * @return List of doctor matches sorted by score (best match first)
     */
    List<DoctorMatch> matchDoctorsToCase(String caseId, MatchOptions options);

    /**
     * Matches facilities for routing using complexity, outcomes, capacity, proximity, capabilities.
     *
     * @param caseId  Medical case ID to route
     * @param options Routing options (max results, filters, etc.)
     * @return List of facility matches sorted by route score (best match first)
     */
    List<FacilityMatch> matchFacilitiesForCase(String caseId, RoutingOptions options);
}
