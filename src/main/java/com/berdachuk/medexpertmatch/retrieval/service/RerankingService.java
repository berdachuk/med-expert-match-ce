package com.berdachuk.medexpertmatch.retrieval.service;

import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;

import java.util.List;

public interface RerankingService {

    List<DoctorMatch> rerank(String caseId, List<DoctorMatch> candidates, int topK);
}
