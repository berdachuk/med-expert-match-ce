package com.berdachuk.medexpertmatch.llm.rest;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(com.berdachuk.medexpertmatch.chat.service.ChatRateLimitLowLimitTestConfig.class)
class A2aRateLimitIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String STREAM_BODY = """
            {"skill":"evidence_search","message":"Anonymized COPD management literature"}
            """;

    @Test
    @DisplayName("A2A stream returns 429 with Retry-After when rate limit exhausted")
    void rateLimitsA2aStream() throws Exception {
        String userId = "a2a-rate-limit-user";
        String payload = STREAM_BODY;

        mockMvc.perform(post("/a2a/v1/stream")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/a2a/v1/stream")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/a2a/v1/stream")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"));
    }
}
