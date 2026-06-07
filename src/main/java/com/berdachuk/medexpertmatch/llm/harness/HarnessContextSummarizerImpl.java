package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class HarnessContextSummarizerImpl implements HarnessContextSummarizer {

    static final int DEFAULT_TOP_MATCHES = 10;
    static final int DEFAULT_TOP_CITATIONS = 3;
    static final int MAX_SUMMARY_CHARS = 3000;

    private static final Pattern PMID_LINE = Pattern.compile("^\\s*\\d+\\.\\s*(PMID:\\d+\\s*-\\s*.+)$", Pattern.MULTILINE);

    private final ObjectMapper objectMapper;

    public HarnessContextSummarizerImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String summarizeToolResults(String rawToolResults, HarnessContextKind kind) {
        if (rawToolResults == null || rawToolResults.isBlank()) {
            return rawToolResults != null ? rawToolResults : "";
        }
        try {
            return switch (kind) {
                case DOCTOR_MATCHES -> summarizeDoctorMatches(rawToolResults);
                case EVIDENCE -> summarizeEvidence(rawToolResults);
                case ROUTING -> summarizeRouting(rawToolResults);
                case GENERIC -> summarizeGeneric(rawToolResults);
            };
        } catch (Exception e) {
            log.warn("Harness context summarization failed ({}), using length cap", kind, e);
            return capLength(rawToolResults);
        }
    }

    private String summarizeDoctorMatches(String raw) throws Exception {
        List<DoctorMatch> matches = objectMapper.readValue(raw, new TypeReference<List<DoctorMatch>>() {});
        ObjectNode root = objectMapper.createObjectNode();
        mergeWhitelistFields(raw, root);
        root.put("match_count", matches.size());
        ArrayNode top = objectMapper.createArrayNode();
        int limit = Math.min(matches.size(), DEFAULT_TOP_MATCHES);
        for (int i = 0; i < limit; i++) {
            DoctorMatch match = matches.get(i);
            ObjectNode entry = objectMapper.createObjectNode();
            entry.put("rank", match.rank() > 0 ? match.rank() : i + 1);
            entry.put("doctor_id", match.doctor().id());
            if (match.doctor().name() != null && !match.doctor().name().isBlank()) {
                entry.put("name", match.doctor().name());
            }
            entry.put("score", match.matchScore());
            if (match.doctor().specialties() != null && !match.doctor().specialties().isEmpty()) {
                entry.put("specialty", match.doctor().specialties().getFirst());
            }
            top.add(entry);
        }
        root.set("top_matches", top);
        return objectMapper.writeValueAsString(root);
    }

    private String summarizeEvidence(String raw) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        mergeWhitelistFields(raw, root);
        List<String> citations = new ArrayList<>();
        Matcher matcher = PMID_LINE.matcher(raw);
        while (matcher.find()) {
            citations.add(matcher.group(1).trim());
        }
        if (citations.isEmpty()) {
            return capLength(raw);
        }
        root.put("evidence_count", citations.size());
        ArrayNode top = objectMapper.createArrayNode();
        int limit = Math.min(citations.size(), DEFAULT_TOP_CITATIONS);
        for (int i = 0; i < limit; i++) {
            top.add(citations.get(i));
        }
        root.set("top_citations", top);
        return objectMapper.writeValueAsString(root);
    }

    private String summarizeRouting(String raw) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        mergeWhitelistFields(raw, root);
        List<String> lines = raw.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
        root.put("facility_count", lines.size());
        ArrayNode top = objectMapper.createArrayNode();
        int limit = Math.min(lines.size(), DEFAULT_TOP_MATCHES);
        for (int i = 0; i < limit; i++) {
            top.add(lines.get(i));
        }
        root.set("top_facilities", top);
        return objectMapper.writeValueAsString(root);
    }

    private String summarizeGeneric(String raw) throws Exception {
        JsonNode node = objectMapper.readTree(raw);
        if (!node.isObject()) {
            return capLength(raw);
        }
        ObjectNode summary = objectMapper.createObjectNode();
        copyWhitelist(node, summary);
        if (summary.isEmpty()) {
            return capLength(raw);
        }
        return objectMapper.writeValueAsString(summary);
    }

    private void mergeWhitelistFields(String raw, ObjectNode target) {
        try {
            JsonNode node = objectMapper.readTree(raw);
            if (node.isObject()) {
                copyWhitelist(node, target);
            }
        } catch (Exception ignored) {
            // non-JSON payloads skip whitelist merge
        }
    }

    private static void copyWhitelist(JsonNode source, ObjectNode target) {
        for (String field : HarnessContextWhitelist.PRESERVED_FIELDS) {
            if (source.has(field) && !source.get(field).isNull()) {
                target.set(field, source.get(field));
            }
        }
    }

    private static String capLength(String raw) {
        if (raw.length() <= MAX_SUMMARY_CHARS) {
            return raw;
        }
        return raw.substring(0, MAX_SUMMARY_CHARS) + "\n\n[Context truncated for clinical interpretation]";
    }
}
