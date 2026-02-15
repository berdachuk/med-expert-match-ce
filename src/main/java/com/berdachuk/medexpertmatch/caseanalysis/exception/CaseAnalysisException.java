package com.berdachuk.medexpertmatch.caseanalysis.exception;

import com.berdachuk.medexpertmatch.core.exception.MedExpertMatchException;

/**
 * Exception thrown when case analysis operations fail (ICD-10 extraction,
 * urgency classification, specialty determination, etc.).
 */
public class CaseAnalysisException extends MedExpertMatchException {

    public CaseAnalysisException(String message) {
        super("CASE_ANALYSIS_ERROR", message);
    }

    public CaseAnalysisException(String message, Throwable cause) {
        super("CASE_ANALYSIS_ERROR", message, cause);
    }
}
