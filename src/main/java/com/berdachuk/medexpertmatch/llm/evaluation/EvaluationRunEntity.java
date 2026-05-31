package com.berdachuk.medexpertmatch.llm.evaluation;

public record EvaluationRunEntity(
        String id,
        String datasetId,
        Double normalizedAccuracy,
        Double meanSemanticSimilarity,
        Double semanticAccuracyAtThreshold,
        String config
) {}
