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

/**
 * REQ-018: integration coverage for registered requirement.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class HarnessWorkflowRunListControllerIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("admin can list harness workflow runs via REST")
    void listRuns() throws Exception {
        mockMvc.perform(get("/api/v1/workflows/runs")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, "admin")
                        .param("state", "NEEDS_HUMAN")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("non-reviewer cannot list harness runs")
    void forbiddenForRegularUser() throws Exception {
        mockMvc.perform(get("/api/v1/workflows/runs")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, "user-1"))
                .andExpect(status().isForbidden());
    }
}
