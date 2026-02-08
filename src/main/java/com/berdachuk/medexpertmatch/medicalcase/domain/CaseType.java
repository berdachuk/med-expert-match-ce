package com.berdachuk.medexpertmatch.medicalcase.domain;

/**
 * Medical case type enumeration.
 */
public enum CaseType {
    /**
     * Inpatient case - patient is admitted to hospital.
     */
    INPATIENT,

    /**
     * Second opinion case - seeking second opinion from specialist.
     */
    SECOND_OPINION,

    /**
     * Consult request case - consultation request in queue.
     */
    CONSULT_REQUEST
}
