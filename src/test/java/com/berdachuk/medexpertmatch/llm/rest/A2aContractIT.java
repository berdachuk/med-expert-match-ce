package com.berdachuk.medexpertmatch.llm.rest;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for A2A JSON-RPC and stream envelopes (M20).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class A2aContractIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("GET /a2a/v1/skills is listed in A2A contract surface")
    void skillsEndpointListed() throws Exception {
        mockMvc.perform(get("/a2a/v1/skills"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("JSON-RPC returns -32601 for unknown methods")
    void unknownMethodErrorCode() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "jsonrpc", "2.0",
                "id", "1",
                "method", "unknownMethod",
                "params", java.util.Map.of()));

        MvcResult result = mockMvc.perform(post("/a2a/v1/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(-32601, json.get("error").get("code").asInt());
    }

    @Test
    @DisplayName("JSON-RPC rejects PHI payloads with -32602")
    void phiPayloadErrorCode() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "jsonrpc", "2.0",
                "id", "2",
                "method", "sendMessage",
                "params", java.util.Map.of(
                        "skill", "evidence_search",
                        "message", "Patient SSN 123-45-6789")));

        MvcResult result = mockMvc.perform(post("/a2a/v1/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(-32602, json.get("error").get("code").asInt());
    }

    @Test
    @DisplayName("A2A stream emits token JSON envelope and done event")
    void streamContract() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "skill", "evidence_search",
                "message", "Anonymized COPD exacerbation management literature"));

        MvcResult asyncStarted = mockMvc.perform(post("/a2a/v1/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult completed = mockMvc.perform(asyncDispatch(asyncStarted))
                .andExpect(status().isOk())
                .andReturn();

        String sse = completed.getResponse().getContentAsString();
        assertTrue(sse.contains("event:token"));
        assertTrue(sse.contains("\"t\""));
        assertTrue(sse.contains("event:done"));
    }
}
