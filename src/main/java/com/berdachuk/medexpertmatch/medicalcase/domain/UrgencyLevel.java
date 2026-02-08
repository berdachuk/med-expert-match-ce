package com.berdachuk.medexpertmatch.medicalcase.domain;

/**
 * Medical case urgency level enumeration.
 */
public enum UrgencyLevel {
    /**
     * Critical - life-threatening, immediate attention required.
     */
    CRITICAL,

    /**
     * High - urgent, attention needed within hours.
     */
    HIGH,

    /**
     * Medium - important but not urgent, attention needed within days.
     */
    MEDIUM,

    /**
     * Low - routine case, attention needed within weeks.
     */
    LOW
}
