package com.berdachuk.medexpertmatch.llm.evaluation;

import java.util.List;

public record EvalCase(
        String id,
        String type,
        String caseId,
        String expectedSpecialty,
        List<String> requiredFields,
        Integer minMatches
) {}
