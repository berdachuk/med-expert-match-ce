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
class A2aAgentCardOpenApiIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Agent card response matches OpenAPI AgentCardResponse contract")
    void agentCardMatchesOpenApiContract() throws Exception {
        mockMvc.perform(get("/.well-known/agent.json")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, "agent-card-openapi-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").isNotEmpty())
                .andExpect(jsonPath("$.description").isNotEmpty())
                .andExpect(jsonPath("$.url").isNotEmpty())
                .andExpect(jsonPath("$.version").isNotEmpty())
                .andExpect(jsonPath("$.protocolVersion").isNotEmpty())
                .andExpect(jsonPath("$.skills").isArray())
                .andExpect(jsonPath("$.skills[0].id").exists())
                .andExpect(jsonPath("$.skills[0].name").exists())
                .andExpect(jsonPath("$.skills[0].description").exists())
                .andExpect(jsonPath("$.capabilities.streaming").isBoolean())
                .andExpect(jsonPath("$.endpoints.jsonrpc").isNotEmpty())
                .andExpect(jsonPath("$.endpoints.stream").isNotEmpty())
                .andExpect(jsonPath("$.endpoints.skills").isNotEmpty())
                .andExpect(jsonPath("$.rateLimits.windowSeconds").isNumber())
                .andExpect(jsonPath("$.rateLimits.defaultPerMinute").isNumber())
                .andExpect(jsonPath("$.rateLimits.highPerMinute").isNumber())
                .andExpect(jsonPath("$.rateLimits.scopes").isArray());
    }
}
