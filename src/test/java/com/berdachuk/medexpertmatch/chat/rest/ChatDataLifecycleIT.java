package com.berdachuk.medexpertmatch.chat.rest;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REQ-014: integration coverage for registered requirement.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ChatDataLifecycleIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Export bundle returns all user chats with phiRedacted messages")
    void exportUserBundle() throws Exception {
        String userId = "lifecycle-export-user";
        createChatWithMessage(userId, "Bundle chat");

        mockMvc.perform(get("/api/v1/chats/export-bundle")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phiRedacted").value(true))
                .andExpect(jsonPath("$.chats").isArray())
                .andExpect(jsonPath("$.chatCount").isNumber())
                .andExpect(jsonPath("$.userIdHash").isNotEmpty())
                .andExpect(jsonPath("$.exportedAt").isNotEmpty())
                .andExpect(jsonPath("$.messageCount").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.auditReferenceHash").isNotEmpty());
    }

    @Test
    @DisplayName("Delete all user data soft-deletes messages and removes non-default chats")
    void deleteAllUserData() throws Exception {
        String userId = "lifecycle-delete-user";
        String chatId = createChatWithMessage(userId, "Delete me");

        mockMvc.perform(delete("/api/v1/chats/data")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("deleted"))
                .andExpect(jsonPath("$.chatsRemoved").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.messagesSoftDeleted").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.auditReferenceHash").isNotEmpty());

        mockMvc.perform(get("/api/v1/chats/" + chatId + "/history")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId))
                .andExpect(status().isNotFound());
    }

    private String createChatWithMessage(String userId, String name) throws Exception {
        var createResult = mockMvc.perform(post("/api/v1/chats")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"agentId\":\"auto\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String chatId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/chats/" + chatId + "/messages")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Anonymized symptom summary for testing\"}"))
                .andExpect(status().isOk());

        return chatId;
    }
}
