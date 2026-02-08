package com.berdachuk.medexpertmatch.evidence.service;

import com.berdachuk.medexpertmatch.evidence.domain.PubMedArticle;

import java.util.List;

/**
 * Service interface for querying PubMed medical literature database.
 */
public interface PubMedService {

    /**
     * Searches PubMed for articles matching the query string.
     *
     * @param query      Search query string
     * @param maxResults Maximum number of results to return
     * @return List of PubMed articles matching the query
     */
    List<PubMedArticle> search(String query, int maxResults);
}
