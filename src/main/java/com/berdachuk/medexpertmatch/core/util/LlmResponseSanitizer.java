package com.berdachuk.medexpertmatch.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LlmResponseSanitizer {

    private static final String STRATEGIZING_MARKER = "strategizing complete";

    /**
     * Toggle for the M74 embedded-JSON renderer. Default {@code true}.
     * Set via {@code medexpertmatch.llm.response.render-embedded-json: false}
     * in application.yml to disable; the sanitizer then leaves any
     * embedded JSON block untouched so operators can debug a prompt change.
     */
    private static final AtomicBoolean RENDER_EMBEDDED_JSON = new AtomicBoolean(true);

    /**
     * Lightweight Jackson mapper for parsing the small JSON objects that
     * the LLM sometimes returns in its {@code Response} block.
     */
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    // Public label map — fixed order so the output is deterministic.
    // Short keys (M132 ultra-compact JSON) are paired with their legacy long-key alias so
    // both forms render to identical prose; unknown-field suppression checks both.
    private static final Map<String, String> FIELD_LABELS = new LinkedHashMap<>();
    static {
        FIELD_LABELS.put("requiredSpecialty", "Recommended specialty");
        FIELD_LABELS.put("sp",               "Recommended specialty");
        FIELD_LABELS.put("urgencyLevel",       "Urgency");
        FIELD_LABELS.put("u",                 "Urgency");
        FIELD_LABELS.put("clinicalFindings",   "Key findings");
        FIELD_LABELS.put("cf",                "Key findings");
        FIELD_LABELS.put("icd10Codes",         "ICD-10 codes");
        FIELD_LABELS.put("icd",               "ICD-10 codes");
        FIELD_LABELS.put("caseSummary",        "Summary");
        FIELD_LABELS.put("sm",                "Summary");
        FIELD_LABELS.put("recommendations",    "Recommendations");
        FIELD_LABELS.put("urgencyRationale",   "Urgency rationale");
    }

    private static final Pattern CONTENT_SECTION_PATTERN = Pattern.compile(
            "(?i)(?:^|[\\n.]\\s*)(Case Summary|Clinical Presentation|Matched Doctors|"
                    + "Matching Rationale(?: Explanation)?|Evidence Summary|Recommendations)\\s*:?(?=\\s*(?:\\n|$))",
            Pattern.MULTILINE);

    private static final Pattern NUMBERED_SECTION_PATTERN = Pattern.compile(
            "(?i)\\d+\\.\\s*(Case Summary|Clinical Presentation|Matched Doctors|"
                    + "Matching Rationale(?: Explanation)?|Evidence Summary|Recommendations)\\b");

    private static final Pattern MARKDOWN_HEADER_PATTERN = Pattern.compile(
            "(?i)\\*{1,2}(Case Summary|Clinical Presentation|Matched Doctors|"
                    + "Matching Rationale(?: Explanation)?|Evidence Summary|Recommendations|"
                    + "Required Medical Specialty|Urgency Level|Clinical Findings|ICD-10 Codes|"
                    + "Case Summary|Summary|Recommendations)\\s*:?\\*{0,2}\\s*\\n",
            Pattern.MULTILINE);

    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile(
            "\\{[^}]*\"(requiredSpecialty|urgencyLevel|clinicalFindings|icd10Codes|caseSummary|sp|u|cf|icd|sm)\"[^}]*\\}",
            Pattern.DOTALL);

    private static final int MIN_REASONING_CHARS = 40;

    private LlmResponseSanitizer() {
    }

    public record ReasoningSplit(String reasoning, String content) {
    }

    /**
     * Extracts JSON from LLM response text, handling markdown code fences and trailing prose.
     *
     * @deprecated Use {@link LenientJsonOutputConverter} instead, which provides
     *             the same fence-stripping and trailing-content cleanup via
     *             {@link LenientJsonOutputConverter#cleanResponse(String)} and
     *             integrates with Spring AI's structured output pipeline.
     */
    @Deprecated(since = "M136", forRemoval = false)
    public static String extractJson(String llmOutput) {
        if (llmOutput == null || llmOutput.isBlank()) {
            return llmOutput;
        }
        String result = llmOutput.trim();
        if (result.contains("```json")) {
            int start = result.indexOf("```json") + 7;
            int end = result.lastIndexOf("```");
            if (end > start) {
                result = result.substring(start, end).trim();
            }
        } else if (result.contains("```")) {
            int start = result.indexOf("```") + 3;
            int end = result.lastIndexOf("```");
            if (end > start) {
                result = result.substring(start, end).trim();
            }
        }
        int lastBrace = result.lastIndexOf('}');
        int lastBracket = result.lastIndexOf(']');
        int lastClose = Math.max(lastBrace, lastBracket);
        if (lastClose > 0 && lastClose < result.length() - 1) {
            String after = result.substring(lastClose + 1).trim();
            if (!after.isEmpty() && !after.startsWith(",") && !after.startsWith("}")) {
                result = result.substring(0, lastClose + 1);
            }
        }
        return result;
    }

    public static String stripLlmReasoning(String response) {
        if (response == null || response.isBlank()) {
            return response;
        }
        ReasoningSplit split = splitReasoningFromResponse(response);
        String content = split.content() != null && !split.content().isBlank() ? split.content() : response;
        content = stripCodeFences(content);
        content = stripToolResultsSection(content);
        content = stripLeadingReasoningHeaders(normalizeModelTokens(content));
        return content;
    }

    static String stripToolResultsSection(String response) {
        if (response == null || response.isBlank()) {
            return response;
        }
        int idx = response.toLowerCase().indexOf("**tool execution results:**");
        if (idx < 0) {
            idx = response.toLowerCase().indexOf("tool execution results:");
        }
        if (idx >= 0) {
            int after = response.indexOf('\n', idx);
            if (after < 0) after = idx;
            int nextSection = response.indexOf("**", after + 1);
            if (nextSection < 0) nextSection = response.indexOf("\n\n", after + 1);
            if (nextSection > after) {
                return response.substring(0, idx).trim() + "\n" + response.substring(nextSection).trim();
            }
            return response.substring(0, idx).trim();
        }
        return response;
    }

    static String stripLeadingJsonBlocks(String response) {
        if (response == null || response.isBlank()) {
            return response;
        }
        String result = response;
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(result);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String before = result.substring(0, start).trim();
            String after = result.substring(end).trim();
            if (before.isEmpty() || before.endsWith("```") || before.endsWith("```json")) {
                result = after;
                matcher = JSON_BLOCK_PATTERN.matcher(result);
            } else {
                break;
            }
        }
        return result;
    }

    /**
     * Splits MedGemma chain-of-thought from the user-facing clinical content.
     */
    public static ReasoningSplit splitReasoningFromResponse(String response) {
        if (response == null || response.isBlank()) {
            return new ReasoningSplit("", response);
        }
        if (response.contains("class=\"llm-thinking\"")) {
            return new ReasoningSplit("", response);
        }

        String cleaned = normalizeModelTokens(response);
        int contentStart = findBestContentStart(cleaned);
        if (contentStart > MIN_REASONING_CHARS) {
            String reasoning = cleaned.substring(0, contentStart).trim();
            String content = cleaned.substring(contentStart).trim();
            return new ReasoningSplit(trimReasoningLabel(reasoning), content);
        }

        if (hasPlanningMarkers(cleaned.toLowerCase(Locale.ROOT)) && contentStart > 0) {
            String reasoning = cleaned.substring(0, contentStart).trim();
            String content = cleaned.substring(contentStart).trim();
            return new ReasoningSplit(trimReasoningLabel(reasoning), content);
        }

        return new ReasoningSplit("", cleaned);
    }

    /**
     * Wraps detected model reasoning in a collapsible HTML block ahead of the clinical answer.
     * <p>
     * <b>M74 — UI-only JSON rendering.</b> Embedded JSON blocks
     * (e.g. {@code "Response\n{ \"requiredSpecialty\": ... }"}) are
     * rendered as human-readable prose in the chat panel via
     * {@link #renderEmbeddedJson(String)}. The server-side data path
     * ({@link #toHumanReadable(String)}) is left untouched so internal
     * consumers of the LLM response still see the original JSON.
     */
    public static String formatForChatDisplay(String response) {
        if (response == null || response.isBlank()) {
            return response;
        }
        if (response.contains("class=\"llm-thinking\"")) {
            return response;
        }

        ReasoningSplit split = splitReasoningFromResponse(response);
        String rawContent = split.content() != null && !split.content().isBlank() ? split.content() : response;
        String content = stripLlmReasoning(rawContent);
        if (RENDER_EMBEDDED_JSON.get()) {
            content = renderEmbeddedJson(content);
        }

        if (split.reasoning() == null || split.reasoning().length() < MIN_REASONING_CHARS) {
            return content;
        }

        return buildCollapsibleReasoning(split.reasoning())
                + "<div class=\"llm-answer-label\">Response</div>\n"
                + "<div class=\"llm-answer\">\n\n"
                + content
                + "\n\n</div>";
    }

    private static String buildCollapsibleReasoning(String reasoning) {
        return "<details class=\"llm-thinking\"><summary>Model reasoning (click to expand)</summary>"
                + "<div class=\"llm-thinking-body\">"
                + escapeHtml(reasoning.trim())
                + "</div></details>\n";
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String normalizeModelTokens(String response) {
        return response.trim().replaceAll("<unused\\d+>", "").trim();
    }

    private static String trimReasoningLabel(String reasoning) {
        if (reasoning == null || reasoning.isBlank()) {
            return "";
        }
        String trimmed = reasoning.trim();
        while (trimmed.toLowerCase().startsWith("thought")) {
            int newlineIdx = trimmed.indexOf('\n');
            if (newlineIdx > 0) {
                trimmed = trimmed.substring(newlineIdx + 1).trim();
            } else {
                return "";
            }
        }
        return trimmed;
    }

    private static int findBestContentStart(String response) {
        int afterStrategizing = findContentStartAfterStrategizing(response);
        if (afterStrategizing >= 0) {
            return afterStrategizing;
        }

        String lower = response.toLowerCase(Locale.ROOT);
        if (hasPlanningMarkers(lower)) {
            int latest = findLatestValidSectionStart(response);
            if (latest >= 0) {
                return latest;
            }
        }

        int numbered = findFirstNumberedSectionStart(response);
        if (numbered >= 0) {
            return numbered;
        }

        int markdown = findFirstMarkdownHeaderStart(response);
        if (markdown >= 0) {
            return markdown;
        }

        int fallback = findFallbackContentStart(response);
        if (fallback >= 0) {
            return fallback;
        }

        return findFirstValidSectionStart(response);
    }

    private static int findFirstMarkdownHeaderStart(String response) {
        Matcher matcher = MARKDOWN_HEADER_PATTERN.matcher(response);
        while (matcher.find()) {
            int start = matcher.start();
            if (start > MIN_REASONING_CHARS) {
                return start;
            }
        }
        return -1;
    }

    private static int findFallbackContentStart(String response) {
        if (!hasPlanningMarkers(response.toLowerCase(Locale.ROOT))) {
            return -1;
        }
        String lower = response.toLowerCase(Locale.ROOT);
        String[] markers = {"case summary", "matching rationale explanation", "matching rationale", "clinical presentation"};
        int best = -1;
        for (String marker : markers) {
            int idx = lower.lastIndexOf(marker);
            if (idx > MIN_REASONING_CHARS && idx > best) {
                best = idx;
            }
        }
        return best;
    }

    private static int findContentStartAfterStrategizing(String response) {
        String lower = response.toLowerCase(Locale.ROOT);
        int markerIdx = lower.lastIndexOf(STRATEGIZING_MARKER);
        if (markerIdx < 0) {
            return -1;
        }
        int searchFrom = markerIdx + STRATEGIZING_MARKER.length();
        String tail = response.substring(searchFrom);

        int relNumbered = findFirstNumberedSectionStart(tail);
        if (relNumbered >= 0) {
            return searchFrom + relNumbered;
        }

        int relSection = findFirstValidSectionStart(tail);
        if (relSection >= 0) {
            return searchFrom + relSection;
        }
        return -1;
    }

    private static int findFirstNumberedSectionStart(String response) {
        Matcher matcher = NUMBERED_SECTION_PATTERN.matcher(response);
        while (matcher.find()) {
            if (isValidSectionMatch(response, matcher.start())) {
                return matcher.start();
            }
        }
        return -1;
    }

    private static int findFirstValidSectionStart(String response) {
        Matcher matcher = CONTENT_SECTION_PATTERN.matcher(response);
        while (matcher.find()) {
            int start = matcher.start(1);
            if (isValidSectionMatch(response, start)) {
                return start;
            }
        }
        return -1;
    }

    private static int findLatestValidSectionStart(String response) {
        int best = -1;
        Matcher numbered = NUMBERED_SECTION_PATTERN.matcher(response);
        while (numbered.find()) {
            if (isValidSectionMatch(response, numbered.start())) {
                best = numbered.start();
            }
        }
        Matcher section = CONTENT_SECTION_PATTERN.matcher(response);
        while (section.find()) {
            int start = section.start(1);
            if (isValidSectionMatch(response, start) && start > best) {
                best = start;
            }
        }
        return best;
    }

    private static boolean isValidSectionMatch(String response, int sectionStart) {
        int lineStart = sectionStart;
        while (lineStart > 0 && response.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }
        int lineEnd = response.indexOf('\n', sectionStart);
        if (lineEnd < 0) {
            lineEnd = response.length();
        }
        String line = response.substring(lineStart, lineEnd).trim();
        String lowerLine = line.toLowerCase(Locale.ROOT);
        if (line.contains("?") && (lowerLine.contains("yes") || lowerLine.contains("no"))) {
            return false;
        }
        if (lowerLine.contains("brief overview")
                || lowerLine.contains("only analysis")
                || lowerLine.contains("invent demographics")
                || lowerLine.contains("will monitor")) {
            return false;
        }
        return true;
    }

    private static boolean hasPlanningMarkers(String lower) {
        return looksLikeMedGemmaPlanningPrefix(lower)
                || lower.contains("the user wants")
                || lower.contains("explain matching rationale:")
                || lower.contains("presents matched doctors:")
                || lower.contains("provide recommendations:")
                || lower.contains("key learnings");
    }

    private static String stripLeadingReasoningHeaders(String cleaned) {
        String[] reasoningHeaders = {
                "Understand the Goal:", "Analyze the", "Step 1:", "Step 2:", "Step 3:",
                "Thought:", "Thinking:", "Reasoning:", "Analysis:", "Let me think",
                "Let's analyze", "First, I'll", "I need to", "The task is", "Key Information"
        };

        for (String header : reasoningHeaders) {
            if (cleaned.toLowerCase().startsWith(header.toLowerCase())) {
                int doubleNewlineIdx = cleaned.indexOf("\n\n");
                if (doubleNewlineIdx > 0 && doubleNewlineIdx < cleaned.length() - 2) {
                    cleaned = cleaned.substring(doubleNewlineIdx + 2).trim();
                    break;
                }
            }
        }

        while (cleaned.toLowerCase().startsWith("thought")) {
            int newlineIdx = cleaned.indexOf('\n');
            if (newlineIdx > 0) {
                cleaned = cleaned.substring(newlineIdx + 1).trim();
            } else {
                break;
            }
        }
        return cleaned;
    }

    private static String stripCodeFences(String cleaned) {
        if (cleaned.contains("```")) {
            cleaned = cleaned.replaceAll("```[a-zA-Z]*\\n?", "").replaceAll("```", "").trim();
        }
        return cleaned;
    }

    private static String stripMedGemmaPlanningPrefix(String response) {
        if (response == null || response.isBlank()) {
            return response;
        }
        if (!hasPlanningMarkers(response.toLowerCase(Locale.ROOT))) {
            return response;
        }
        int contentStart = findBestContentStart(response);
        if (contentStart > 0) {
            return response.substring(contentStart).trim();
        }
        return response;
    }

    private static boolean looksLikeMedGemmaPlanningPrefix(String lower) {
        return lower.contains("mental sandbox")
                || lower.contains("constraint checklist")
                || lower.contains("confidence score")
                || lower.contains("strategizing complete")
                || lower.startsWith("thought")
                || lower.startsWith("recommendations: yes")
                || lower.contains("summarize the case:")
                || lower.contains("self-correction:")
                || lower.startsWith("the user wants")
                || lower.contains("double check constraints")
                || lower.contains("the analysis seems correct");
    }

    public static String toHumanReadable(String response) {
        if (response == null || response.isBlank()) {
            return response;
        }
        String cleaned = response.trim();

        cleaned = stripJsonPrefix(cleaned);
        cleaned = stripFinalResponsePrefix(cleaned);
        cleaned = cleanJsonOnlyContent(cleaned);

        return cleaned;
    }

    /**
     * Spring-friendly setter for the {@link #RENDER_EMBEDDED_JSON} flag.
     * Wired by {@link LlmResponseRenderConfig} from the
     * {@code medexpertmatch.llm.response.render-embedded-json} property.
     */
    public static void setRenderEmbeddedJson(boolean enabled) {
        RENDER_EMBEDDED_JSON.set(enabled);
    }

    private static String stripJsonPrefix(String response) {
        String[] prefixes = {"final response:", "final answer:", "response:", "answer:",
                "here is the", "the result is:"};
        String lower = response.toLowerCase().trim();
        for (String prefix : prefixes) {
            if (lower.startsWith(prefix)) {
                String after = response.substring(prefix.length()).trim();
                if (!after.isEmpty()) {
                    return after;
                }
            }
        }
        return response;
    }

    private static String stripFinalResponsePrefix(String response) {
        String cleaned = response.trim();
        String lower = cleaned.toLowerCase();

        String[] headers = {"final response:", "final answer:", "response:", "answer:",
                "final response", "final answer"};
        for (String header : headers) {
            if (lower.startsWith(header)) {
                String after = cleaned.substring(header.length()).trim();
                if (!after.isEmpty()) {
                    cleaned = after;
                    lower = cleaned.toLowerCase();
                }
            }
        }
        return cleaned;
    }

    private static String cleanJsonOnlyContent(String response) {
        String trimmed = response.trim();

        if (isJsonOnly(trimmed)) {
            return "[Data received; unable to display formatted response]";
        }

        if (trimmed.startsWith("[") && looksLikeJsonArray(trimmed)) {
            int closeIdx = findClosingBracket(trimmed);
            if (closeIdx > 0 && isJsonOnly(trimmed.substring(0, closeIdx + 1))) {
                String remainder = trimmed.substring(closeIdx + 1).trim();
                if (remainder.isEmpty()) {
                    return "[Data received; unable to display formatted response]";
                }
                return remainder;
            }
        }

        if (trimmed.startsWith("{") && looksLikeJsonObject(trimmed)) {
            int closeIdx = findClosingBrace(trimmed);
            if (closeIdx > 0 && isJsonOnly(trimmed.substring(0, closeIdx + 1))) {
                String remainder = trimmed.substring(closeIdx + 1).trim();
                if (remainder.isEmpty()) {
                    return "[Data received; unable to display formatted response]";
                }
                return remainder;
            }
        }

        return response;
    }

    // -----------------------------------------------------------------
    // M74: render embedded JSON blocks as human-readable prose.
    // -----------------------------------------------------------------

    /**
     * Scans {@code response} for any JSON object ({@code { ... }}) and
     * replaces each one that parses cleanly with a prose rendering of
     * its fields. JSON that fails to parse is left in place (we never
     * delete data we cannot understand). Text outside the JSON blocks
     * is preserved.
     */
    static String renderEmbeddedJson(String response) {
        if (response == null || response.isEmpty()) {
            return response;
        }
        if (!response.contains("{")) {
            return response;
        }

        StringBuilder out = new StringBuilder(response.length());
        int cursor = 0;
        boolean replacedAny = false;
        while (cursor < response.length()) {
            int openIdx = response.indexOf('{', cursor);
            if (openIdx < 0) {
                out.append(response, cursor, response.length());
                break;
            }
            // Copy everything before the '{' verbatim
            out.append(response, cursor, openIdx);

            int closeIdx = findMatchingBrace(response, openIdx);
            if (closeIdx < 0) {
                // Unbalanced brace — leave the rest untouched.
                out.append(response, openIdx, response.length());
                break;
            }
            String candidate = response.substring(openIdx, closeIdx + 1);
            String rendered = renderJsonObjectText(candidate);
            if (rendered != null) {
                out.append(rendered);
                replacedAny = true;
            } else {
                // Could not parse (or empty {}) — preserve the original block
                out.append(candidate);
            }
            cursor = closeIdx + 1;
        }
        String result = out.toString();
        // If the only thing in the response is a single empty JSON object,
        // surface the legacy generic message so the chat panel never
        // shows a bare "{}" blob.
        if (!replacedAny && response.trim().equals("{}")) {
            return "[Data received; unable to display formatted response]";
        }
        return result;
    }

    /**
     * Attempts to parse {@code jsonText} as a JSON object and render it
     * as prose. Returns {@code null} if the text is not a parseable
     * JSON object or has no fields worth rendering (caller should
     * leave the original in place).
     */
    @SuppressWarnings("unchecked")
    static String renderJsonObjectText(String jsonText) {
        if (jsonText == null) {
            return null;
        }
        String trimmed = jsonText.trim();
        if (!trimmed.startsWith("{")) {
            return null;
        }
        Map<String, Object> map;
        try {
            map = JSON_MAPPER.readValue(trimmed, Map.class);
        } catch (JsonProcessingException e) {
            return null;
        }
        if (map == null || map.isEmpty()) {
            // Empty JSON object — there is literally no information to
            // surface. Caller will fall back to the generic message.
            return null;
        }
        return formatFieldsAsProse(map);
    }

    /**
     * Renders a parsed JSON object as a multi-line prose string with
     * friendly labels in a fixed order. Unknown fields are rendered
     * after the known fields in insertion order.
     */
    private static String formatFieldsAsProse(Map<String, Object> map) {
        List<String> lines = new ArrayList<>();

        // 1. Known fields first, in the fixed order defined in FIELD_LABELS.
        for (Map.Entry<String, String> e : FIELD_LABELS.entrySet()) {
            Object value = map.get(e.getKey());
            if (value == null) {
                continue;
            }
            String rendered = renderValue(value);
            if (rendered != null) {
                lines.add(e.getValue() + ": " + rendered);
            }
        }
        // 2. Unknown fields, in the order Jackson gave us (insertion order
        //    for parsed LinkedHashMap).
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (FIELD_LABELS.containsKey(e.getKey())) {
                continue;
            }
            Object value = e.getValue();
            if (value == null) {
                continue;
            }
            String rendered = renderValue(value);
            if (rendered != null) {
                lines.add(e.getKey() + ": " + rendered);
            }
        }
        if (lines.isEmpty()) {
            return null;
        }
        return String.join("\n", lines);
    }

    /**
     * Renders a single JSON value as a human-friendly string.
     * Strings are kept as-is (with surrounding quotes stripped).
     * Arrays of primitives / objects are comma-joined.
     * Nested objects and nulls are rendered as {@code null} so the
     * caller skips them.
     */
    private static String renderValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return s;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof List<?> list) {
            List<String> parts = new ArrayList<>(list.size());
            for (Object item : list) {
                String r = renderValue(item);
                if (r != null) {
                    parts.add(r);
                }
            }
            return parts.isEmpty() ? null : String.join(", ", parts);
        }
        if (value instanceof Map<?, ?> nested) {
            return renderValue(new ArrayList<>(nested.values()));
        }
        return value.toString();
    }

    /**
     * Returns the index of the {@code }} that closes the JSON object
     * opened at {@code openIdx}, respecting string boundaries and
     * nested braces. Returns {@code -1} if the brace is not matched.
     */
    private static int findMatchingBrace(String text, int openIdx) {
        if (openIdx < 0 || openIdx >= text.length() || text.charAt(openIdx) != '{') {
            return -1;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = openIdx; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static boolean isJsonOnly(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("[")) {
            int closeIdx = findClosingBracket(trimmed);
            return closeIdx == trimmed.length() - 1;
        }
        if (trimmed.startsWith("{")) {
            int closeIdx = findClosingBrace(trimmed);
            return closeIdx == trimmed.length() - 1;
        }
        return false;
    }

    private static boolean looksLikeJsonArray(String text) {
        return text.startsWith("[") && (text.contains("\"") || text.contains("{"));
    }

    private static boolean looksLikeJsonObject(String text) {
        return text.startsWith("{") && text.contains("\"");
    }

    private static int findClosingBracket(String text) {
        int depth = 0;
        boolean inString = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (!inString) {
                if (c == '[') depth++;
                if (c == ']') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return -1;
    }

    private static int findClosingBrace(String text) {
        int depth = 0;
        boolean inString = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (!inString) {
                if (c == '{') depth++;
                if (c == '}') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return -1;
    }
}
