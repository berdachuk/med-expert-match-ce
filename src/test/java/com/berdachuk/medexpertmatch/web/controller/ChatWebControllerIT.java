package com.berdachuk.medexpertmatch.web.controller;

import com.berdachuk.medexpertmatch.core.security.HeaderBasedUserContext;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ChatWebControllerIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldLoadChatPage() throws Exception {
        mockMvc.perform(get("/chat")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, "test-user-chat"))
                .andExpect(status().isOk())
                .andExpect(view().name("chat"))
                .andExpect(model().attributeExists("currentPage", "chats", "currentChat", "messages"))
                .andExpect(model().attribute("currentPage", "chat"));
    }

    @Test
    @DisplayName("Chat page includes data lifecycle controls")
    void includesLifecycleControls() throws Exception {
        mockMvc.perform(get("/chat")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, "lifecycle-ui-user"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"exportBundleBtn\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"deleteAllDataBtn\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"deleteAllDataModal\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"lifecycleToast\"")));
    }

    @Test
    @DisplayName("Chat page renders localized mode label")
    void includesLocalizedChatModeLabels() throws Exception {
        mockMvc.perform(get("/chat")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, "chat-mode-i18n-user"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("expert_match")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("??chat.mode."))));
    }
}
