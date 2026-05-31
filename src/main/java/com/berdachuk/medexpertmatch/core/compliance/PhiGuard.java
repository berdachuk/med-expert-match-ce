package com.berdachuk.medexpertmatch.core.compliance;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Detects and redacts PHI-shaped tokens (SSN, MRN, DOB, email, phone, patient-name labels).
 * Shared across chat export, A2A bridge, and long-term AutoMemory.
 */
public final class PhiGuard {

    private static final String REDACTION = "[REDACTED]";

    private static final Pattern SSN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern EMAIL = Pattern.compile("\\b[\\w.+-]+@[\\w-]+\\.[\\w.-]+\\b");
    private static final Pattern PHONE = Pattern.compile("\\(?\\b\\d{3}\\)?[\\s.-]\\d{3}[\\s.-]\\d{4}\\b");
    private static final Pattern DOB = Pattern.compile(
            "\\b(0?[1-9]|1[0-2])[/-](0?[1-9]|[12]\\d|3[01])[/-](19|20)\\d{2}\\b");
    private static final Pattern MRN = Pattern.compile("(?i)\\bMRN[:#]?\\s*\\d{4,}\\b");
    private static final Pattern PATIENT_NAME =
            Pattern.compile("(?i)\\bpatient(?:'s)?\\s+name\\b\\s*[:=]?\\s*[A-Z][a-z]+");
    private static final Pattern SSN_LABEL = Pattern.compile("(?i)\\bssn\\b\\s*[:=]?\\s*\\d{3}");

    private static final List<Pattern> PATTERNS =
            List.of(SSN, EMAIL, PHONE, DOB, MRN, PATIENT_NAME, SSN_LABEL);

    private PhiGuard() {
    }

    public static boolean containsPhi(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (Pattern p : PATTERNS) {
            if (p.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    public static String redact(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String result = text;
        for (Pattern p : PATTERNS) {
            result = p.matcher(result).replaceAll(REDACTION);
        }
        return result;
    }
}
