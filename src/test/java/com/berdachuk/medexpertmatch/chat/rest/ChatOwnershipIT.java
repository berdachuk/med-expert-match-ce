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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REQ-014: integration coverage for registered requirement.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ChatOwnershipIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void otherUserCannotAccessChatHistoryOrPost() throws Exception {
        String owner = "chat-owner";
        String intruder = "chat-intruder";

        var createResult = mockMvc.perform(post("/api/v1/chats")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Private\",\"agentId\":\"auto\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String chatId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/chats/" + chatId + "/history")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, intruder))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/chats/" + chatId + "/messages")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, intruder)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"intrusion\",\"agentId\":\"auto\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/chats/" + chatId)
                        .header(HeaderBasedUserContext.USER_ID_HEADER, intruder))
                .andExpect(status().isForbidden());
    }
}
