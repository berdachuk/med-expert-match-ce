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
class A2aJsonRpcRateLimitIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String JSONRPC_BODY = """
            {"jsonrpc":"2.0","id":"1","method":"sendMessage","params":{"skill":"evidence_search","message":"Anonymized COPD management literature"}}
            """;

    @Test
    @DisplayName("A2A JSON-RPC returns 429 with Retry-After when rate limit exhausted")
    void rateLimitsJsonRpc() throws Exception {
        String userId = "a2a-jsonrpc-rate-limit-user";

        mockMvc.perform(post("/a2a/v1/jsonrpc")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONRPC_BODY))
                .andExpect(status().isOk());

        mockMvc.perform(post("/a2a/v1/jsonrpc")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONRPC_BODY))
                .andExpect(status().isOk());

        mockMvc.perform(post("/a2a/v1/jsonrpc")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONRPC_BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"));
    }
}
