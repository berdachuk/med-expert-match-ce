package com.berdachuk.medexpertmatch.embedding.exception;

import com.berdachuk.medexpertmatch.core.exception.MedExpertMatchException;

/**
 * Thrown when embedding generation fails (single-endpoint or multi-endpoint pool).
 */
public class EmbeddingException extends MedExpertMatchException {

    public EmbeddingException(String message) {
        super("EMBEDDING_ERROR", message);
    }

    public EmbeddingException(String message, Throwable cause) {
        super("EMBEDDING_ERROR", message, cause);
    }
}
