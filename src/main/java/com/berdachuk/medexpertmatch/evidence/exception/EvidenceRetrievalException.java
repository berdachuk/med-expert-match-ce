package com.berdachuk.medexpertmatch.evidence.exception;

import com.berdachuk.medexpertmatch.core.exception.MedExpertMatchException;

/**
 * Exception thrown when evidence retrieval operations fail (PubMed API,
 * guideline search, etc.).
 */
public class EvidenceRetrievalException extends MedExpertMatchException {

    public EvidenceRetrievalException(String message) {
        super("EVIDENCE_RETRIEVAL_ERROR", message);
    }

    public EvidenceRetrievalException(String message, Throwable cause) {
        super("EVIDENCE_RETRIEVAL_ERROR", message, cause);
    }
}
