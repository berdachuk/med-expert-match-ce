package com.berdachuk.medexpertmatch.chat.rest;

import com.berdachuk.medexpertmatch.core.security.HeaderBasedUserContext;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ChatAgenticUxIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

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

        mockMvc.perform(post("/api/v1/chats/" + chatId + "/messages")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Plan a triage workflow\",\"agentId\":\"auto\"}"))
                .andExpect(status().isOk());

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
}
