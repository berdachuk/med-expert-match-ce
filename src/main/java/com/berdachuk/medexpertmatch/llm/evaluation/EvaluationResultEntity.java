package com.berdachuk.medexpertmatch.llm.evaluation;

public record EvaluationResultEntity(
        String id,
        String runId,
        String caseId,
        String predictedAnswer,
        boolean exactMatch,
        boolean normalizedMatch,
        Double semanticSimilarity,
        boolean semanticPass
) {}
