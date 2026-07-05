package com.berdachuk.medexpertmatch.caseanalysis.domain;

import java.util.List;

/**
 * Wrapper for list-shaped structured LLM output (ICD-10 codes, specialties).
 */
public record StringListJson(List<String> items) {

    public StringListJson {
        items = items != null ? List.copyOf(items) : List.of();
    }

    public List<String> toList() {
        return items;
    }
}
