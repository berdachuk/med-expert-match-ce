package com.berdachuk.medexpertmatch.llm.chat;

import java.util.regex.Pattern;

/**
 * Medical-domain intent patterns for goal classification (English and other languages).
 */
public final class GoalIntentPatterns {

    static final Pattern CASE_DETAIL_REQUEST = Pattern.compile(
            "\\b(?:detail(?:ing)?\\s+(?:the\\s+)?(?:clinical\\s+)?case|clinical\\s+case\\s+details?|"
                    + "case\\s+(?:details?|summary|breakdown|deep[- ]dive|narrative|workup|overview)|"
                    + "elaborate\\s+(?:on\\s+)?(?:the\\s+)?(?:clinical\\s+)?case|"
                    + "expand\\s+(?:on\\s+)?(?:the\\s+)?(?:clinical\\s+)?case|"
                    + "break\\s+down\\s+(?:the\\s+)?case|walk\\s+me\\s+through\\s+(?:the\\s+)?(?:clinical\\s+)?case|"
                    + "clinical\\s+(?:findings|assessment|picture|summary|details?|context)|"
                    + "patient\\s+(?:case|summary|presentation|profile)|"
                    + "summarize\\s+(?:the\\s+)?(?:(?:clinical|patient)\\s+)?case|in[- ]depth\\s+(?:case\\s+)?analysis|"
                    + "what\\s+are\\s+the\\s+clinical\\s+findings|describe\\s+(?:the\\s+)?(?:clinical\\s+)?case|"
                    + "analyze\\s+(?:this\\s+)?case|case\\s+analysis|differential\\s+diagnosis|"
                    + "clinical\\s+breakdown|deeper\\s+(?:clinical\\s+)?analysis|"
                    + "explain\\s+(?:the\\s+)?(?:clinical\\s+)?case|review\\s+(?:the\\s+)?clinical\\s+case)\\b",
            Pattern.CASE_INSENSITIVE);

    static final Pattern CASE_DETAIL_REQUEST_RU = Pattern.compile(
            "(?:детализируй|опиши|разверни|раскрой|подробнее|расскажи\\s+подробнее|что\\s+ещё|что\\s+еще)"
                    + ".{0,40}(?:случа|кейс)|(?:клиническ(?:ий|ого)\\s+случа|анализ\\s+случа)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    static final Pattern MATCH_DOCTORS_KEYWORDS = Pattern.compile(
            "\\b(?:find\\s+specialist|match\\s+doctor|recommend\\s+doctor|expert\\s+match|"
                    + "find\\s+doctors?|rank\\s+doctors?|best\\s+doctor|find\\s+expert|"
                    + "suggest\\s+(?:a\\s+)?specialist|who\\s+should\\s+treat|physician\\s+recommendation|"
                    + "expert\\s+referral|specialist\\s+ranking|top\\s+doctors?|"
                    + "suitable\\s+specialist|clinician\\s+match|specialist\\s+recommendation|"
                    + "match\\s+specialists?|locate\\s+(?:a\\s+)?specialist|refer\\s+to\\s+specialist)\\b",
            Pattern.CASE_INSENSITIVE);

    static final Pattern ANALYZE_CASE_KEYWORDS = Pattern.compile(
            "\\b(?:analyze\\s+case|analyze\\s+this\\s+case|\\bicd\\b|diagnosis\\s+hint|"
                    + "clinical\\s+findings|case\\s+summary|clinical\\s+assessment|case\\s+workup|"
                    + "coding\\s+suggestions?|urgency\\s+assessment|patient\\s+workup|"
                    + "find\\s+case\\s+information|case\\s+details)\\b",
            Pattern.CASE_INSENSITIVE);

    static final Pattern ROUTE_CASE_KEYWORDS = Pattern.compile(
            "\\b(?:route\\s+(?:case|patient)|facility|referral|where\\s+to\\s+send|"
                    + "send\\s+to\\s+hospital|appropriate\\s+facility|care\\s+center|"
                    + "where\\s+should\\s+(?:the\\s+)?patient\\s+go|treatment\\s+center)\\b",
            Pattern.CASE_INSENSITIVE);

    static final Pattern TRIAGE_KEYWORDS = Pattern.compile(
            "\\b(?:urgency|triage|intake|red\\s+flag|priority\\s+level|how\\s+urgent|"
                    + "acuity\\s+assessment|emergency\\s+severity)\\b",
            Pattern.CASE_INSENSITIVE);

    static final Pattern EVIDENCE_KEYWORDS = Pattern.compile(
            "\\b(?:pubmed|evidence|guideline|literature|clinical\\s+literature|"
                    + "treatment\\s+guidelines?|research\\s+papers?|systematic\\s+review)\\b",
            Pattern.CASE_INSENSITIVE);

    static final Pattern RUSSIAN_FOLLOW_UP = Pattern.compile(
            "(?:ещё|еще|подробнее|расскажи|что\\s+ещё|что\\s+еще|продолжай|дальше)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern ELABORATION_FOLLOW_UP = Pattern.compile(
            "\\b(tell me more|more (?:details|info|information)|provide more|what about|"
                    + "how about|elaborate|expand|details? (?:about|on|for)|explain (?:more|further))\\b",
            Pattern.CASE_INSENSITIVE);

    private GoalIntentPatterns() {
    }

    public static boolean looksLikeCaseDetailRequest(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String trimmed = message.trim();
        return CASE_DETAIL_REQUEST.matcher(trimmed).find()
                || CASE_DETAIL_REQUEST_RU.matcher(trimmed).find();
    }

    public static boolean looksLikeCaseContinuation(String message) {
        return looksLikeCaseDetailRequest(message)
                || GoalClassifier.requestsMoreDoctors(message);
    }

    public static boolean matchesMatchDoctorsKeywords(String message) {
        return message != null && MATCH_DOCTORS_KEYWORDS.matcher(message).find();
    }

    public static boolean matchesAnalyzeCaseKeywords(String message) {
        return message != null && (ANALYZE_CASE_KEYWORDS.matcher(message).find()
                || looksLikeCaseDetailRequest(message));
    }

    public static boolean matchesRouteCaseKeywords(String message) {
        return message != null && ROUTE_CASE_KEYWORDS.matcher(message).find();
    }

    public static boolean matchesTriageKeywords(String message) {
        return message != null && TRIAGE_KEYWORDS.matcher(message).find();
    }

    public static boolean matchesEvidenceKeywords(String message) {
        return message != null && EVIDENCE_KEYWORDS.matcher(message).find();
    }

    public static boolean looksLikePubmedIntent(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String lower = message.trim().toLowerCase();
        return lower.contains("pubmed")
                || lower.contains("literature")
                || lower.contains("research paper")
                || lower.contains("systematic review");
    }

    public static boolean matchesRussianFollowUp(String message) {
        return message != null && RUSSIAN_FOLLOW_UP.matcher(message.trim()).find();
    }

    /**
     * True when the user asks to elaborate on the prior turn (not to find more doctors).
     * Examples: "provide more details", "?", "tell me more".
     */
    public static boolean looksLikeElaborationFollowUp(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        if (GoalClassifier.requestsMoreDoctors(message)) {
            return false;
        }
        String trimmed = message.trim();
        if (looksLikeCaseDetailRequest(trimmed)) {
            return true;
        }
        if (ELABORATION_FOLLOW_UP.matcher(trimmed).find()) {
            return true;
        }
        if (matchesRussianFollowUp(trimmed)) {
            return true;
        }
        return trimmed.length() <= 20 && trimmed.matches("^[?.!…]+$");
    }
}
