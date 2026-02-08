package com.berdachuk.medexpertmatch.retrieval.repository;

import com.berdachuk.medexpertmatch.retrieval.domain.ConsultationMatch;

import java.util.List;

/**
 * Repository for consultation match persistence.
 */
public interface ConsultationMatchRepository {

    /**
     * Deletes all consultation matches for the given case.
     *
     * @param caseId Case ID (internal CHAR(24))
     */
    void deleteByCaseId(String caseId);

    /**
     * Inserts consultation matches in batch.
     *
     * @param matches List of consultation matches to insert
     * @return List of inserted match IDs
     */
    List<String> insertBatch(List<ConsultationMatch> matches);

    /**
     * Returns total count of consultation match records.
     *
     * @return Total number of consultation matches
     */
    long count();

    /**
     * Deletes all consultation match records. For use in tests.
     *
     * @return Number of records deleted
     */
    int deleteAll();
}
