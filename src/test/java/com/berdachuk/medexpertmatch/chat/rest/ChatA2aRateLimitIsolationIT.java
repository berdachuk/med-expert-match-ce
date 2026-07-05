package com.berdachuk.medexpertmatch.chat.rest;

import com.berdachuk.medexpertmatch.core.security.HeaderBasedUserContext;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REQ-014: integration coverage for registered requirement.
 * REQ-016: integration coverage for registered requirement.
 * REQ-018: integration coverage for registered requirement.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(com.berdachuk.medexpertmatch.chat.service.ChatRateLimitLowLimitTestConfig.class)
class ChatA2aRateLimitIsolationIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String A2A_BODY = """
            {"skill":"evidence_search","message":"Anonymized COPD management literature"}
            """;

    @Test
    @DisplayName("A2A rate limit exhaustion does not block chat SSE stream")
    void a2aDoesNotExhaustChatBucket() throws Exception {
        String userId = "bucket-isolation-user";
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/a2a/v1/stream")
                            .header(HeaderBasedUserContext.USER_ID_HEADER, userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(A2A_BODY))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/a2a/v1/stream")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(A2A_BODY))
                .andExpect(status().isTooManyRequests());

        var createResult = mockMvc.perform(post("/api/v1/chats")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Isolation\",\"agentId\":\"auto\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String chatId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        MvcResult asyncStarted = mockMvc.perform(post("/api/v1/chats/" + chatId + "/messages/stream")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Anonymized triage question\",\"agentId\":\"auto\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(asyncStarted)).andExpect(status().isOk());
    }
}
