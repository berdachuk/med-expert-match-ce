package com.berdachuk.medexpertmatch.retrieval.service.impl;

import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;
import com.berdachuk.medexpertmatch.retrieval.domain.MatchSignalBreakdown;
import com.berdachuk.medexpertmatch.retrieval.domain.ScoreResult;
import com.berdachuk.medexpertmatch.retrieval.service.MatchExplainabilityService;
import com.berdachuk.medexpertmatch.retrieval.service.SemanticGraphRetrievalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class MatchExplainabilityServiceImpl implements MatchExplainabilityService {

    private final MedicalCaseRepository medicalCaseRepository;
    private final SemanticGraphRetrievalService semanticGraphRetrievalService;

    public MatchExplainabilityServiceImpl(
            MedicalCaseRepository medicalCaseRepository,
            SemanticGraphRetrievalService semanticGraphRetrievalService) {
        this.medicalCaseRepository = medicalCaseRepository;
        this.semanticGraphRetrievalService = semanticGraphRetrievalService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchSignalBreakdown> explainMatches(String caseId, List<DoctorMatch> matches, int limit) {
        if (caseId == null || caseId.isBlank() || matches == null || matches.isEmpty()) {
            return List.of();
        }
        MedicalCase medicalCase = medicalCaseRepository.findById(caseId.trim().toLowerCase()).orElse(null);
        if (medicalCase == null) {
            return List.of();
        }
        int capped = Math.max(1, Math.min(limit, matches.size()));
        List<MatchSignalBreakdown> breakdowns = new ArrayList<>();
        for (int i = 0; i < capped; i++) {
            DoctorMatch match = matches.get(i);
            if (match.doctor() == null) {
                continue;
            }
            ScoreResult score = semanticGraphRetrievalService.score(medicalCase, match.doctor());
            breakdowns.add(toBreakdown(match, score));
        }
        return breakdowns;
    }

    private static MatchSignalBreakdown toBreakdown(DoctorMatch match, ScoreResult score) {
        int vector = toPercent(score.vectorSimilarityScore());
        int graph = toPercent(score.graphRelationshipScore());
        int history = toPercent(score.historicalPerformanceScore());
        String specialty = match.doctor().specialties() != null && !match.doctor().specialties().isEmpty()
                ? match.doctor().specialties().getFirst()
                : "";
        return new MatchSignalBreakdown(
                match.doctor().id(),
                match.doctor().name(),
                specialty,
                score.overallScore(),
                match.rank(),
                vector,
                graph,
                history);
    }

    private static int toPercent(double component) {
        return (int) Math.round(Math.max(0.0, Math.min(1.0, component)) * 100.0);
    }
}
