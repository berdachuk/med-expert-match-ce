package com.berdachuk.medexpertmatch.llm.evaluation;

import java.util.List;

public record EvalDataset(
        String datasetId,
        String version,
        List<EvalCase> cases
) {}
