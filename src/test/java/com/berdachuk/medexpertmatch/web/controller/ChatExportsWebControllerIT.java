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
class ChatExportsWebControllerIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Admin chat exports audit page loads with action filter")
    void adminPageLoads() throws Exception {
        mockMvc.perform(get("/admin/chat-exports").param("user", "admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/chat-exports"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"actionFilter\"")));
    }

    @Test
    @DisplayName("Non-admin redirected from chat exports page")
    void nonAdminRedirected() throws Exception {
        mockMvc.perform(get("/admin/chat-exports"))
                .andExpect(status().is3xxRedirection());
    }
}
