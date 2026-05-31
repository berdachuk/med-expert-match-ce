package com.berdachuk.medexpertmatch.core.domain;

/**
 * Rate limit tier for API session tokens.
 */
public enum RateLimitTier {
    DEFAULT,
    HIGH,
    UNLIMITED;

    public static RateLimitTier fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT;
        }
        return RateLimitTier.valueOf(value.toUpperCase());
    }

    public String toDatabaseValue() {
        return name().toLowerCase();
    }
}
