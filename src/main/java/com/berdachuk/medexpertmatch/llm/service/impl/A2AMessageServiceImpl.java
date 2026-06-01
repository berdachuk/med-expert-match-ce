package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.core.compliance.PhiGuard;
import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import com.berdachuk.medexpertmatch.llm.chat.ChatAgentProfile;
import com.berdachuk.medexpertmatch.llm.chat.ChatToolContextHolder;
import com.berdachuk.medexpertmatch.llm.service.A2AMessageService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.llm.tools.EvidenceAgentTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class A2AMessageServiceImpl implements A2AMessageService {

    private static final int EVIDENCE_MAX_RESULTS = 5;

    private final MedicalAgentService medicalAgentService;
    private final EvidenceAgentTools evidenceAgentTools;

    public A2AMessageServiceImpl(MedicalAgentService medicalAgentService, EvidenceAgentTools evidenceAgentTools) {
        this.medicalAgentService = medicalAgentService;
        this.evidenceAgentTools = evidenceAgentTools;
    }

    @Override
    public Map<String, Object> sendMessage(Map<String, Object> request) {
        String skill = resolveSkill(request);
        String message = extractMessageText(request);

        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message text is required");
        }
        if (PhiGuard.containsPhi(message)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "PHI detected in message payload — anonymize before sending");
        }

        return executeSkill(skill, message);
    }

    @Override
    public Map<String, Object> handleJsonRpc(Map<String, Object> request) {
        Object id = request.get("id");
        String version = stringField(request, "jsonrpc");
        if (version != null && !"2.0".equals(version)) {
            return jsonRpcError(id, -32600, "Invalid Request: jsonrpc must be 2.0");
        }

        String method = stringField(request, "method");
        if (method == null || method.isBlank()) {
            return jsonRpcError(id, -32600, "Invalid Request: method is required");
        }

        if (!"sendMessage".equals(method)) {
            return jsonRpcError(id, -32601, "Method not found: " + method);
        }

        Object params = request.get("params");
        if (!(params instanceof Map<?, ?> paramMap)) {
            return jsonRpcError(id, -32602, "Invalid params");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> paramBody = (Map<String, Object>) paramMap;
        try {
            Map<String, Object> result = sendMessage(paramBody);
            return jsonRpcResult(id, result);
        } catch (ResponseStatusException ex) {
            return jsonRpcError(id, mapHttpStatus(ex.getStatusCode().value()), ex.getReason());
        }
    }

    @Override
    public SseEmitter streamMessage(Map<String, Object> request) {
        SseEmitter emitter = new SseEmitter(120_000L);
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> result = sendMessage(request);
                String text = extractStreamText(result);
                for (String chunk : chunkText(text)) {
                    emitter.send(SseEmitter.event().name("token").data(Map.of("t", chunk)));
                }
                emitter.send(SseEmitter.event().name("done").data(result.get("skill")));
                emitter.complete();
            } catch (ResponseStatusException ex) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(ex.getReason()));
                } catch (IOException ignored) {
                    // emitter already failed
                }
                emitter.completeWithError(ex);
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        });
        return emitter;
    }

    private static String extractStreamText(Map<String, Object> result) {
        Object nested = result.get("result");
        if (!(nested instanceof Map<?, ?> resultMap)) {
            return result.toString();
        }
        Object message = resultMap.get("message");
        if (message != null) {
            return message.toString();
        }
        Object summary = resultMap.get("summary");
        if (summary != null) {
            return summary.toString();
        }
        return resultMap.toString();
    }

    private static List<String> chunkText(String text) {
        if (text == null || text.isBlank()) {
            return List.of("");
        }
        int chunkSize = Math.max(8, text.length() / 10);
        List<String> chunks = new java.util.ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize) {
            chunks.add(text.substring(i, Math.min(text.length(), i + chunkSize)));
        }
        return chunks;
    }

    private Map<String, Object> executeSkill(String skill, String message) {
        String sessionId = "a2a-" + UUID.randomUUID();
        OrchestrationContextHolder.setSessionId(sessionId);
        ChatToolContextHolder.setProfile(profileForSkill(skill));
        try {
            return switch (skill) {
                case "doctor_match" -> bridgeDoctorMatch(message);
                case "evidence_search" -> bridgeEvidenceSearch(message);
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown skill: " + skill);
            };
        } finally {
            ChatToolContextHolder.clear();
            OrchestrationContextHolder.clear();
        }
    }

    private static ChatAgentProfile profileForSkill(String skill) {
        return switch (skill) {
            case "doctor_match" -> ChatAgentProfile.SPECIALIST_MATCHER;
            case "evidence_search" -> ChatAgentProfile.EVIDENCE_SCOUT;
            default -> ChatAgentProfile.AUTO;
        };
    }

    private Map<String, Object> bridgeDoctorMatch(String message) {
        log.info("A2A doctor_match bridge — message length {}", message.length());
        MedicalAgentService.AgentResponse agentResponse = medicalAgentService.matchFromText(
                message, Map.of("interactiveIntake", false));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", agentResponse.response() != null ? agentResponse.response() : "");
        result.put("metadata", sanitizeMetadata(agentResponse.metadata()));
        result.put("phiDetected", false);

        return Map.of(
                "status", "completed",
                "skill", "doctor_match",
                "result", result);
    }

    private Map<String, Object> bridgeEvidenceSearch(String message) {
        log.info("A2A evidence_search bridge — message length {}", message.length());
        List<String> guidelines = evidenceAgentTools.search_clinical_guidelines(message, null, EVIDENCE_MAX_RESULTS);
        List<String> pubmed = evidenceAgentTools.query_pubmed(message, EVIDENCE_MAX_RESULTS);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("guidelines", guidelines);
        result.put("pubmed", pubmed);
        result.put("summary", buildEvidenceSummary(guidelines, pubmed));
        result.put("phiDetected", false);

        return Map.of(
                "status", "completed",
                "skill", "evidence_search",
                "result", result);
    }

    private static String buildEvidenceSummary(List<String> guidelines, List<String> pubmed) {
        return "Guidelines: " + guidelines.size() + " item(s); PubMed: " + pubmed.size() + " item(s)";
    }

    private static Map<String, Object> sanitizeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> safe = new LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            if (value == null) {
                return;
            }
            String asText = value.toString();
            if (!PhiGuard.containsPhi(asText)) {
                safe.put(key, value);
            }
        });
        return safe;
    }

    private static Map<String, Object> jsonRpcResult(Object id, Map<String, Object> result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    private static Map<String, Object> jsonRpcError(Object id, int code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("error", Map.of("code", code, "message", message != null ? message : "error"));
        return response;
    }

    private static int mapHttpStatus(int httpStatus) {
        return switch (httpStatus) {
            case 400 -> -32602;
            case 404 -> -32601;
            default -> -32000;
        };
    }

    private static String resolveSkill(Map<String, Object> request) {
        String skill = stringField(request, "skill");
        if (skill == null || skill.isBlank()) {
            Object params = request.get("params");
            if (params instanceof Map<?, ?> paramMap) {
                Object nested = paramMap.get("skill");
                if (nested != null) {
                    skill = nested.toString();
                }
            }
        }
        return skill != null && !skill.isBlank() ? skill : "doctor_match";
    }

    @SuppressWarnings("unchecked")
    private static String extractMessageText(Map<String, Object> request) {
        Object params = request.get("params");
        if (params instanceof Map<?, ?> paramMap) {
            Object message = paramMap.get("message");
            if (message instanceof Map<?, ?> messageMap) {
                Object parts = messageMap.get("parts");
                if (parts instanceof Iterable<?> iterable) {
                    for (Object part : iterable) {
                        if (part instanceof Map<?, ?> partMap) {
                            Object text = partMap.get("text");
                            if (text != null) {
                                return text.toString();
                            }
                        }
                    }
                }
                Object text = messageMap.get("text");
                if (text != null) {
                    return text.toString();
                }
            }
            if (message instanceof String s) {
                return s;
            }
        }
        Object direct = request.get("message");
        return direct != null ? direct.toString() : null;
    }

    private static String stringField(Map<String, Object> request, String key) {
        Object value = request.get(key);
        return value != null ? value.toString() : null;
    }
}
