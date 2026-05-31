package com.berdachuk.medexpertmatch.web.controller;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AdminDashboardWebControllerIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Admin dashboard hub loads for ?user=admin")
    void adminDashboardLoads() throws Exception {
        mockMvc.perform(get("/admin").param("user", "admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Session Tokens")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chat Export Audit")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chat Retention")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Operator Runbook")));
    }

    @Test
    @DisplayName("Non-admin redirected from admin dashboard")
    void nonAdminRedirected() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection());
    }
}
