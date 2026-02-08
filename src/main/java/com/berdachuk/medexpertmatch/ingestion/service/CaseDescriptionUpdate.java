package com.berdachuk.medexpertmatch.ingestion.service;

/**
 * Helper record for batch description updates.
 * Used to accumulate description updates before committing to database.
 */
public record CaseDescriptionUpdate(String caseId, String description) {
}
