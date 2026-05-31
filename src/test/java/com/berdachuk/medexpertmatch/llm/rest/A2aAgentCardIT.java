package com.berdachuk.medexpertmatch.llm.rest;

import com.berdachuk.medexpertmatch.core.security.HeaderBasedUserContext;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class A2aAgentCardIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Agent card is served at well-known discovery paths with skills and JSON-RPC endpoint")
    void servesAgentCardAtWellKnownPaths() throws Exception {
        for (String path : new String[]{"/.well-known/agent-card.json", "/.well-known/agent.json"}) {
            mockMvc.perform(get(path)
                            .header(HeaderBasedUserContext.USER_ID_HEADER, "agent-card-it-user"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.skills[?(@.id=='doctor_match')]").exists())
                    .andExpect(jsonPath("$.skills[?(@.id=='evidence_search')]").exists())
                    .andExpect(jsonPath("$.endpoints.jsonrpc").exists())
                    .andExpect(jsonPath("$.endpoints.stream").exists())
                    .andExpect(jsonPath("$.endpoints.skills").exists())
                    .andExpect(jsonPath("$.rateLimits.windowSeconds").exists())
                    .andExpect(jsonPath("$.rateLimits.scopes[?(@=='CHAT_SSE')]").exists())
                    .andExpect(jsonPath("$.rateLimits.scopes[?(@=='A2A')]").exists());
        }
    }
}
