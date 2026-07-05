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
 * REQ-014: integration coverage for registered requirement.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ChatAgenticUxIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChatService chatService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String streamChatMessage(String userId, String chatId, String content) throws Exception {
        MvcResult asyncStarted = mockMvc.perform(post("/api/v1/chats/" + chatId + "/messages/stream")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + content + "\",\"agentId\":\"auto\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult completed = mockMvc.perform(asyncDispatch(asyncStarted))
                .andExpect(status().isOk())
                .andReturn();

        return completed.getResponse().getContentAsString();
    }

    @Test
    void agentTodoAndQuestionEndpointsRespondForChatSession() throws Exception {
        String userId = "agentic-ux-user";

        var createResult = mockMvc.perform(post("/api/v1/chats")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Agentic\",\"agentId\":\"auto\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String chatId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();
        String sessionId = userId + "-" + chatId;

        MvcResult asyncStarted = mockMvc.perform(post("/api/v1/chats/" + chatId + "/messages/stream")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Plan a triage workflow\",\"agentId\":\"auto\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(asyncStarted)).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/agent/todos/latest")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId))
                .andExpect(result -> {
                    int code = result.getResponse().getStatus();
                    if (code != 200 && code != 204) {
                        throw new AssertionError("Expected 200 or 204 but was " + code);
                    }
                });

        mockMvc.perform(get("/api/v1/agent/questions/pending")
                        .param("sessionId", sessionId)
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId))
                .andExpect(result -> {
                    int code = result.getResponse().getStatus();
                    if (code != 200 && code != 204) {
                        throw new AssertionError("Expected 200 or 204 but was " + code);
                    }
                });
    }

    @Test
    @DisplayName("Chat page exposes activity panel and server-rendered markdown history")
    void chatPageIncludesActivityPanelAndMarkdownHistory() throws Exception {
        String userId = "agentic-panel-user";
        var chat = chatService.createChat(userId, "Panel UX", "auto");
        chatService.appendAssistantMessage(chat.id(), userId, "**Bold** clinical summary");

        mockMvc.perform(get("/chat")
                        .param("chatId", chat.id())
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"agentTodoPanel\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"agentQuestionPanel\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<strong>Bold</strong>")));
    }

    @Test
    @DisplayName("Malicious markdown is sanitized server-side and not emitted as executable href")
    void markdownXssPayloadIsNotRenderedAsScriptLink() throws Exception {
        String userId = "agentic-xss-user";
        String payload = "**[click](javascript:alert(1))**";
        var chat = chatService.createChat(userId, "XSS", "auto");
        chatService.appendAssistantMessage(chat.id(), userId, payload);

        MvcResult result = mockMvc.perform(get("/chat")
                        .param("chatId", chat.id())
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertTrue(html.contains("<strong>click</strong>"), "SSR must render safe bold text");
        assertFalse(html.contains("href=\"javascript:"), "SSR must not emit javascript: href");
    }

    @Test
    @DisplayName("Chat SSE stream JSON-wraps tokens and includes full content on done")
    void streamWrapsTokensAndIncludesDoneContent() throws Exception {
        String userId = "agentic-sse-format-user";
        var createResult = mockMvc.perform(post("/api/v1/chats")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"SSE format\",\"agentId\":\"auto\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String chatId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        String body = streamChatMessage(userId, chatId, "Summarize triage steps");
        assertTrue(body.contains("event:token") && body.contains("\"t\""),
                "token events must JSON-wrap chunks to preserve whitespace");
        assertTrue(body.contains("event:done") && body.contains("\"content\""),
                "done event must include saved assistant content for client rendering");
    }

    @Test
    @DisplayName("Chat SSE stream emits agent and activity events")
    void streamIncludesAgentAndActivityEvents() throws Exception {
        String userId = "agentic-sse-user";
        var createResult = mockMvc.perform(post("/api/v1/chats")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"SSE\",\"agentId\":\"auto\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String chatId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        String body = streamChatMessage(userId, chatId, "Summarize triage steps");
        assertTrue(body.contains("event:agent") || body.contains("event:token"),
                "stream must include agent lifecycle or token events");
        assertTrue(body.contains("event:activity") || body.contains("Planning response"),
                "stream must include reasoning/activity events");
    }
}
