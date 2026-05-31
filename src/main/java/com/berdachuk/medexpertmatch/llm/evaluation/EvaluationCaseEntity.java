package com.berdachuk.medexpertmatch.llm.evaluation;

public record EvaluationCaseEntity(
        String id,
        String datasetId,
        String question,
        String groundTruthAnswer,
        String metaJson
) {}
