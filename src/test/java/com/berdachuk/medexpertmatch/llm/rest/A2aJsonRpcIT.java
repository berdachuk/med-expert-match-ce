package com.berdachuk.medexpertmatch.llm.rest;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REQ-018: integration coverage for registered requirement.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class A2aJsonRpcIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void jsonRpcSendMessageRoutesEvidenceSkill() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "jsonrpc", "2.0",
                "id", "it-1",
                "method", "sendMessage",
                "params", java.util.Map.of(
                        "skill", "evidence_search",
                        "message", "Anonymized COPD exacerbation management literature")));

        mockMvc.perform(post("/a2a/v1/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("completed"))
                .andExpect(jsonPath("$.result.skill").value("evidence_search"))
                .andExpect(jsonPath("$.result.result.summary").exists());
    }

    @Test
    @DisplayName("JSON-RPC doctor_match returns completed envelope with result.message")
    void jsonRpcDoctorMatchContract() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "jsonrpc", "2.0",
                "id", "it-match",
                "method", "sendMessage",
                "params", java.util.Map.of(
                        "skill", "doctor_match",
                        "message", "45-year-old with chest pain, rule out ACS")));

        mockMvc.perform(post("/a2a/v1/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("completed"))
                .andExpect(jsonPath("$.result.skill").value("doctor_match"))
                .andExpect(jsonPath("$.result.result.message").exists())
                .andExpect(jsonPath("$.result.result.phiDetected").value(false));
    }

    @Test
    @DisplayName("JSON-RPC returns -32601 for unknown method")
    void jsonRpcUnknownMethodError() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "jsonrpc", "2.0",
                "id", "err-1",
                "method", "unknownMethod",
                "params", java.util.Map.of()));

        mockMvc.perform(post("/a2a/v1/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32601))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("Method not found")));
    }

    @Test
    @DisplayName("JSON-RPC returns -32602 for invalid params")
    void jsonRpcInvalidParamsError() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "jsonrpc", "2.0",
                "id", "err-2",
                "method", "sendMessage",
                "params", "not-an-object"));

        mockMvc.perform(post("/a2a/v1/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32602));
    }

    @Test
    @DisplayName("JSON-RPC returns -32600 for invalid jsonrpc version")
    void jsonRpcInvalidVersionError() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "jsonrpc", "1.0",
                "id", "err-3",
                "method", "sendMessage",
                "params", java.util.Map.of("skill", "doctor_match", "message", "test case")));

        mockMvc.perform(post("/a2a/v1/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32600));
    }
}
