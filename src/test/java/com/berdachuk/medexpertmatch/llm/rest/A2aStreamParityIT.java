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
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies A2A SSE stream uses the same token envelope as chat ({@code event:token}, JSON {@code {"t":"..."}}).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class A2aStreamParityIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("A2A stream emits chat-compatible token and done events")
    void a2aStreamParity() throws Exception {
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
        assertTrue(sse.contains("event:token"), "A2A stream must emit token events");
        assertTrue(sse.contains("\"t\""), "A2A token data must use chat JSON envelope");
        assertTrue(sse.contains("event:done"), "A2A stream must complete with done event");
    }
}
