package com.berdachuk.medexpertmatch.web.controller;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * REQ-003: integration coverage for registered requirement.
 * REQ-011: integration coverage for registered requirement.
 * REQ-125: integration coverage for registered requirement.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class QueueControllerIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldLoadQueuePage() throws Exception {
        mockMvc.perform(get("/queue"))
                .andExpect(status().isOk())
                .andExpect(view().name("queue"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(model().attribute("currentPage", "queue"));
    }

    @Test
    void shouldPrioritizeConsults() throws Exception {
        mockMvc.perform(post("/queue/prioritize"))
                .andExpect(status().isOk())
                .andExpect(view().name("queue"))
                .andExpect(model().attributeExists("prioritizationResult"))
                .andExpect(model().attributeDoesNotExist("error"));
    }
}
