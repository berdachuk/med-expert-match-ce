package com.berdachuk.medexpertmatch.chat.rest;

import com.berdachuk.medexpertmatch.chat.service.ChatService;
import com.berdachuk.medexpertmatch.core.security.HeaderBasedUserContext;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

/**
 * End-to-end smoke coverage for chat page assets and SSE lifecycle (M18 Step 4).
 * Playwright/browser automation remains optional for local runs; see testing skill.
 */
/**
 * REQ-014: integration coverage for registered requirement.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ChatE2ESmokeIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChatService chatService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Chat page loads streaming assets and server-rendered markdown history")
    void chatPageSmoke() throws Exception {
        String userId = "chat-e2e-smoke-user";
        var chat = chatService.createChat(userId, "Smoke", "auto");
        chatService.appendAssistantMessage(chat.id(), userId, "**Ready** for streaming");

        mockMvc.perform(get("/chat")
                        .param("chatId", chat.id())
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"agentTodoPanel\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"agentQuestionPanel\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/js/chat.js")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<strong>Ready</strong>")));
    }

    @Test
    @DisplayName("Chat SSE stream completes with token, activity, and done events")
    void chatStreamSmoke() throws Exception {
        String userId = "chat-e2e-stream-user";
        var createResult = mockMvc.perform(post("/api/v1/chats")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Smoke stream\",\"agentId\":\"auto\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String chatId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        MvcResult asyncStarted = mockMvc.perform(post("/api/v1/chats/" + chatId + "/messages/stream")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hello\",\"agentId\":\"auto\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult completed = mockMvc.perform(asyncDispatch(asyncStarted))
                .andExpect(status().isOk())
                .andReturn();

        String body = completed.getResponse().getContentAsString();
        assertTrue(body.contains("event:token"), "stream must emit tokens");
        assertTrue(body.contains("event:done"), "stream must complete");
        assertFalse(body.contains("javascript:"), "stream must not emit script links");
    }
}
