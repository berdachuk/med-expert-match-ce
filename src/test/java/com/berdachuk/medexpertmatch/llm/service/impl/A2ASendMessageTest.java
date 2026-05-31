package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.llm.tools.MedicalAgentTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class A2ASendMessageTest {

    @Mock
    private MedicalAgentService medicalAgentService;

    @Mock
    private MedicalAgentTools medicalAgentTools;

    private A2AMessageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new A2AMessageServiceImpl(medicalAgentService, medicalAgentTools);
    }

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
    @DisplayName("sendMessage routes evidence_search to PubMed and guidelines tools")
    void routesEvidenceSearch() {
        when(medicalAgentTools.search_clinical_guidelines(any(), eq(null), anyInt()))
                .thenReturn(List.of("Guideline 1"));
        when(medicalAgentTools.query_pubmed(any(), anyInt()))
                .thenReturn(List.of("PubMed article 1"));

        Map<String, Object> body = Map.of(
                "skill", "evidence_search",
                "message", "Summarize hypertension treatment guidelines");

        Map<String, Object> response = service.sendMessage(body);
        assertEquals("completed", response.get("status"));
        assertEquals("evidence_search", response.get("skill"));
        verify(medicalAgentTools).query_pubmed("Summarize hypertension treatment guidelines", 5);
    }

    @Test
    @DisplayName("sendMessage routes doctor_match to matchFromText workflow")
    void routesDoctorMatch() {
        when(medicalAgentService.matchFromText(any(), any()))
                .thenReturn(new MedicalAgentService.AgentResponse("Matched specialists", Map.of("caseId", "abc")));

        Map<String, Object> body = Map.of(
                "skill", "doctor_match",
                "message", "45-year-old with chest pain, rule out ACS");

        Map<String, Object> response = service.sendMessage(body);
        assertEquals("completed", response.get("status"));
        assertEquals("doctor_match", response.get("skill"));
        verify(medicalAgentService).matchFromText(eq("45-year-old with chest pain, rule out ACS"), any());
    }

    @Test
    @DisplayName("JSON-RPC sendMessage returns result envelope")
    void jsonRpcSendMessage() {
        when(medicalAgentTools.search_clinical_guidelines(any(), eq(null), anyInt()))
                .thenReturn(List.of("G1"));
        when(medicalAgentTools.query_pubmed(any(), anyInt()))
                .thenReturn(List.of("P1"));

        Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "id", "req-1",
                "method", "sendMessage",
                "params", Map.of(
                        "skill", "evidence_search",
                        "message", "COPD exacerbation management"));

        Map<String, Object> response = service.handleJsonRpc(request);
        assertEquals("2.0", response.get("jsonrpc"));
        assertEquals("req-1", response.get("id"));
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        assertEquals("completed", result.get("status"));
    }

    @Test
    @DisplayName("JSON-RPC unknown method maps to -32601")
    void jsonRpcUnknownMethod() {
        Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "id", 1,
                "method", "ping",
                "params", Map.of());

        Map<String, Object> response = service.handleJsonRpc(request);
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) response.get("error");
        assertEquals(-32601, error.get("code"));
    }

    @Test
    @DisplayName("JSON-RPC invalid params maps to -32602")
    void jsonRpcInvalidParams() {
        Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "id", 2,
                "method", "sendMessage",
                "params", "bad");

        Map<String, Object> response = service.handleJsonRpc(request);
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) response.get("error");
        assertEquals(-32602, error.get("code"));
    }
}
