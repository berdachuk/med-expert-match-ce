package com.berdachuk.medexpertmatch.llm.tools.support;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Validates and normalizes internal medical case IDs (24-character hex).
 */
@Slf4j
public final class AgentToolCaseIdValidator {

    private static final Pattern HEX_24 = Pattern.compile("^[0-9a-fA-F]{24}$");

    private AgentToolCaseIdValidator() {
    }

    public static Optional<String> normalizeIfValid(String caseId) {
        if (caseId == null || caseId.length() != 24 || !HEX_24.matcher(caseId).matches()) {
            return Optional.empty();
        }
        return Optional.of(caseId.trim().toLowerCase());
    }

    public static String requireValid(String caseId) {
        if (caseId == null) {
            throw new IllegalArgumentException("Invalid case ID: null (expected 24-character hex string)");
        }
        if (caseId.length() != 24) {
            throw new IllegalArgumentException(String.format(
                    "Invalid case ID format: '%s' (expected 24-character hex string, got %d characters)",
                    caseId, caseId.length()));
        }
        if (!HEX_24.matcher(caseId).matches()) {
            throw new IllegalArgumentException(String.format(
                    "Invalid case ID format: '%s' (expected 24-character hex string, contains invalid characters)",
                    caseId));
        }
        return caseId.trim().toLowerCase();
    }

    public static String requireValid(
            String caseId,
            String toolName,
            LogStreamService logStreamService,
            String sessionId) {
        if (caseId == null) {
            String errorMsg = "Invalid case ID: null (expected 24-character hex string)";
            log.error(errorMsg);
            logStreamService.logError(sessionId, toolName + " validation failed", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        log.info("VALIDATING caseId format - original: '{}', length: {}", caseId, caseId.length());
        if (caseId.length() != 24) {
            String errorMsg = String.format(
                    "Invalid case ID format: '%s' (expected 24-character hex string, got %d characters)",
                    caseId, caseId.length());
            log.error(errorMsg);
            logStreamService.logError(sessionId, toolName + " validation failed", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        if (!HEX_24.matcher(caseId).matches()) {
            String errorMsg = String.format(
                    "Invalid case ID format: '%s' (expected 24-character hex string, contains invalid characters)",
                    caseId);
            log.error(errorMsg);
            logStreamService.logError(sessionId, toolName + " validation failed", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        log.info("Case ID validation passed - original: '{}', length: {}, hex format: valid", caseId, caseId.length());
        return caseId.trim().toLowerCase();
    }
}
