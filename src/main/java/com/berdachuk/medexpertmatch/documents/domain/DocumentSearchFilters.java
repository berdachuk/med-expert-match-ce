package com.berdachuk.medexpertmatch.documents.domain;

import java.time.LocalDate;

/**
 * Optional metadata filters for faceted document chunk search.
 */
public record DocumentSearchFilters(
        String category,
        String source,
        LocalDate fromDate,
        LocalDate toDate
) {
    public static DocumentSearchFilters none() {
        return new DocumentSearchFilters(null, null, null, null);
    }
}
