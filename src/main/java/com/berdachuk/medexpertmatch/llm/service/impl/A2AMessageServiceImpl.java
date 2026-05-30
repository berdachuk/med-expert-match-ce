package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.llm.automemory.PhiGuard;
import com.berdachuk.medexpertmatch.llm.service.A2AMessageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class A2AMessageServiceImpl implements A2AMessageService {

    @Override
    public Map<String, Object> sendMessage(Map<String, Object> request) {
        String skill = stringField(request, "skill");
        String message = extractMessageText(request);

        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message text is required");
        }
        if (PhiGuard.containsPhi(message)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "PHI detected in message payload — anonymize before sending");
        }

        String resolvedSkill = skill != null && !skill.isBlank() ? skill : "doctor_match";
        return Map.of(
                "status", "accepted",
                "skill", resolvedSkill,
                "result", Map.of(
                        "message", "Request accepted for skill " + resolvedSkill
                                + ". Full A2A JSON-RPC executor ships in M16 after pom approval.",
                        "phiDetected", false));
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
        }
        Object direct = request.get("message");
        return direct != null ? direct.toString() : null;
    }

    private static String stringField(Map<String, Object> request, String key) {
        Object value = request.get(key);
        return value != null ? value.toString() : null;
    }
}
