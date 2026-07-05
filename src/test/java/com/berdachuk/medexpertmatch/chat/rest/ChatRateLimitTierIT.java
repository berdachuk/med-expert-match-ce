package com.berdachuk.medexpertmatch.chat.rest;

import com.berdachuk.medexpertmatch.core.domain.ApiSessionToken;
import com.berdachuk.medexpertmatch.core.domain.RateLimitTier;
import com.berdachuk.medexpertmatch.core.repository.ApiSessionTokenRepository;
import com.berdachuk.medexpertmatch.core.security.HeaderBasedUserContext;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REQ-014: integration coverage for registered requirement.
 * REQ-016: integration coverage for registered requirement.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(com.berdachuk.medexpertmatch.chat.service.ChatRateLimitLowLimitTestConfig.class)
class ChatRateLimitTierIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApiSessionTokenRepository apiSessionTokenRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("HIGH tier API session allows more chat stream turns than DEFAULT tier")
    void highTierAllowsMoreRequests() throws Exception {
        String apiKey = "high-tier-" + IdGenerator.generateId();
        Instant now = Instant.now();
        apiSessionTokenRepository.insert(new ApiSessionToken(
                IdGenerator.generateId(),
                apiKey,
                "HIGH tier test token",
                RateLimitTier.HIGH,
                now.plusSeconds(3600),
                now,
                null));

        String userId = "tier-user-high-" + IdGenerator.generateId();
        String chatId = createChat(userId);
        String payload = "{\"content\":\"Hello\",\"agentId\":\"auto\"}";

        for (int i = 0; i < 3; i++) {
            streamOk(userId, chatId, payload, apiKey);
        }
    }

    @Test
    @DisplayName("DEFAULT tier exhausts bucket sooner than HIGH tier")
    void defaultTierExhaustsSooner() throws Exception {
        String userId = "tier-user-default-" + IdGenerator.generateId();
        String chatId = createChat(userId);
        String payload = "{\"content\":\"Hello\",\"agentId\":\"auto\"}";

        streamOk(userId, chatId, payload, null);
        streamOk(userId, chatId, payload, null);

        mockMvc.perform(post("/api/v1/chats/" + chatId + "/messages/stream")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isTooManyRequests());
    }

    private void streamOk(String userId, String chatId, String payload, String apiKey) throws Exception {
        var requestBuilder = post("/api/v1/chats/" + chatId + "/messages/stream")
                .header(HeaderBasedUserContext.USER_ID_HEADER, userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload);
        if (apiKey != null) {
            requestBuilder = requestBuilder.header(HeaderBasedUserContext.API_KEY_HEADER, apiKey);
        }
        MvcResult asyncStarted = mockMvc.perform(requestBuilder)
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(asyncStarted))
                .andExpect(status().isOk());
    }

    private String createChat(String userId) throws Exception {
        var createResult = mockMvc.perform(post("/api/v1/chats")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tier\",\"agentId\":\"auto\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();
    }
}
