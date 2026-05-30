package com.berdachuk.medexpertmatch.llm.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class A2ASendMessageTest {

    private final A2AMessageServiceImpl service = new A2AMessageServiceImpl();

    @Test
    @DisplayName("sendMessage rejects PHI in payload")
    void rejectsPhi() {
        Map<String, Object> body = Map.of(
                "skill", "doctor_match",
                "params", Map.of(
                        "message", Map.of(
                                "parts", List.of(Map.of("text", "Patient name: John Smith with MRN: 12345678")))));

        assertThrows(ResponseStatusException.class, () -> service.sendMessage(body));
    }

    @Test
    @DisplayName("sendMessage accepts anonymized clinical text")
    void acceptsAnonymizedMessage() {
        Map<String, Object> body = Map.of(
                "skill", "evidence_search",
                "message", "Summarize hypertension treatment guidelines");

        Map<String, Object> response = service.sendMessage(body);
        assertEquals("accepted", response.get("status"));
        assertEquals("evidence_search", response.get("skill"));
    }
}
