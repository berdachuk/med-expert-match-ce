package com.berdachuk.medexpertmatch.web.controller;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class SessionTokensWebControllerIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Admin session tokens page loads for ?user=admin")
    void adminPageLoads() throws Exception {
        mockMvc.perform(get("/admin/session-tokens").param("user", "admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/session-tokens"));
    }

    @Test
    @DisplayName("Non-admin redirected from session tokens page")
    void nonAdminRedirected() throws Exception {
        mockMvc.perform(get("/admin/session-tokens"))
                .andExpect(status().is3xxRedirection());
    }
}
