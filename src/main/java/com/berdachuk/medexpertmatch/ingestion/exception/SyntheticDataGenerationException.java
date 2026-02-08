package com.berdachuk.medexpertmatch.ingestion.exception;

import com.berdachuk.medexpertmatch.core.exception.MedExpertMatchException;

/**
 * Exception thrown when synthetic data generation operations fail.
 */
public class SyntheticDataGenerationException extends MedExpertMatchException {

    public SyntheticDataGenerationException(String message) {
        super("SYNTHETIC_DATA_GENERATION_ERROR", message);
    }

    public SyntheticDataGenerationException(String message, Throwable cause) {
        super("SYNTHETIC_DATA_GENERATION_ERROR", message, cause);
    }
}
