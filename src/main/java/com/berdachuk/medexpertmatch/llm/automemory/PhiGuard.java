package com.berdachuk.medexpertmatch.llm.automemory;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Guards the long-term memory layer against persisting Protected Health Information (PHI).
 * <p>
 * Durable memory must hold ONLY non-PHI (clinician preferences, routing policies, model config).
 * This guard detects PHI-shaped tokens (SSN, MRN, DOB, email, phone, explicit patient-name
 * labels) so callers can reject or redact content before it is written to disk. It is intentionally
 * conservative: a false positive merely drops a memory entry, whereas a false negative would leak
 * patient data — so the guard errs toward over-detection.
 */
public final class PhiGuard {

    private static final String REDACTION = "[REDACTED]";

    private static final Pattern SSN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern EMAIL = Pattern.compile("\\b[\\w.+-]+@[\\w-]+\\.[\\w.-]+\\b");
    // Phone: optional country/area grouping, 10 digits with common separators.
    private static final Pattern PHONE = Pattern.compile("\\(?\\b\\d{3}\\)?[\\s.-]\\d{3}[\\s.-]\\d{4}\\b");
    private static final Pattern DOB = Pattern.compile(
            "\\b(0?[1-9]|1[0-2])[/-](0?[1-9]|[12]\\d|3[01])[/-](19|20)\\d{2}\\b");
    private static final Pattern MRN = Pattern.compile("(?i)\\bMRN[:#]?\\s*\\d{4,}\\b");
    // Explicit patient-name labels, e.g. "Patient name: John Smith".
    private static final Pattern PATIENT_NAME =
            Pattern.compile("(?i)\\bpatient(?:'s)?\\s+name\\b\\s*[:=]?\\s*[A-Z][a-z]+");
    // Bare SSN label even without the canonical dash format.
    private static final Pattern SSN_LABEL = Pattern.compile("(?i)\\bssn\\b\\s*[:=]?\\s*\\d{3}");

    private static final List<Pattern> PATTERNS =
            List.of(SSN, EMAIL, PHONE, DOB, MRN, PATIENT_NAME, SSN_LABEL);

    private PhiGuard() {
    }

    /**
     * @return {@code true} if the text contains any PHI-shaped token; {@code false} for null/blank.
     */
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

    /**
     * Replaces any PHI-shaped tokens with {@value #REDACTION}. Clean non-PHI text is returned
     * unchanged.
     */
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
