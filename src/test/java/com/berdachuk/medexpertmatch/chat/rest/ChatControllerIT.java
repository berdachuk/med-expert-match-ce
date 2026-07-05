package com.berdachuk.medexpertmatch.chat.rest;

import com.berdachuk.medexpertmatch.core.security.HeaderBasedUserContext;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REQ-014: integration coverage for registered requirement.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ChatControllerIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldListAndPostChatMessage() throws Exception {
        String userId = "chat-it-user";

        var createResult = mockMvc.perform(post("/api/v1/chats")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"IT Chat\",\"agentId\":\"auto\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        JsonNode chatNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String chatId = chatNode.get("id").asText();

        mockMvc.perform(get("/api/v1/chats")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/chats/" + chatId + "/messages")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hello from IT\",\"agentId\":\"auto\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userMessage.content").value("Hello from IT"))
                .andExpect(jsonPath("$.assistantMessage.content").exists());
    }
}
