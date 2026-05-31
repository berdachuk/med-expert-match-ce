package com.berdachuk.medexpertmatch.core.util;

/**
 * Masks API keys for admin list views.
 */
public final class ApiKeyMasker {

    private ApiKeyMasker() {
    }

    public static String prefix(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "****";
        }
        if (apiKey.length() <= 8) {
            return apiKey.substring(0, Math.min(4, apiKey.length())) + "…";
        }
        return apiKey.substring(0, 8) + "…";
    }
}
