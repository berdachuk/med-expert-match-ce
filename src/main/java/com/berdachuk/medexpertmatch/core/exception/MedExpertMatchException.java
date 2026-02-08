package com.berdachuk.medexpertmatch.core.exception;

import lombok.Getter;

/**
 * Base exception for MedExpertMatch application.
 */
@Getter
public class MedExpertMatchException extends RuntimeException {

    private final String errorCode;

    public MedExpertMatchException(String message) {
        super(message);
        this.errorCode = "MED_EXPERT_MATCH_ERROR";
    }

    public MedExpertMatchException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "MED_EXPERT_MATCH_ERROR";
    }

    public MedExpertMatchException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public MedExpertMatchException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public MedExpertMatchException(ErrorCode errorCode, String context) {
        super(errorCode.getFullMessage(context));
        this.errorCode = errorCode.getCode();
    }

    public MedExpertMatchException(ErrorCode errorCode, String context, Throwable cause) {
        super(errorCode.getFullMessage(context), cause);
        this.errorCode = errorCode.getCode();
    }
}
