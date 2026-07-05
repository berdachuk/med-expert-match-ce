package com.berdachuk.medexpertmatch.core.rest;

import com.berdachuk.medexpertmatch.core.domain.RateLimitTier;
import com.berdachuk.medexpertmatch.core.repository.ApiSessionTokenRepository;
import com.berdachuk.medexpertmatch.core.security.AdminAccessGuard;
import com.berdachuk.medexpertmatch.core.security.HeaderBasedUserContext;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REQ-016: integration coverage for registered requirement.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AdminSessionTokenIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApiSessionTokenRepository apiSessionTokenRepository;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM api_session_token");
    }

    @Test
    @DisplayName("Non-admin cannot list session tokens")
    void rejectsNonAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/session-tokens")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, "regular-user"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin creates token once, lists masked, and revokes")
    void adminSessionTokenLifecycle() throws Exception {
        String createBody = objectMapper.writeValueAsString(java.util.Map.of(
                "description", "M21 test token",
                "rateLimitTier", "high"));

        var createResult = mockMvc.perform(post("/api/v1/admin/session-tokens")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, AdminAccessGuard.ADMIN_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String tokenId = created.get("id").asText();
        String fullKey = created.get("apiKey").asText();
        assertTrue(fullKey.length() >= 32);

        var listResult = mockMvc.perform(get("/api/v1/admin/session-tokens")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, AdminAccessGuard.ADMIN_USER_ID))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode list = objectMapper.readTree(listResult.getResponse().getContentAsString());
        assertEquals(1, list.size());
        assertFalse(list.get(0).has("apiKey"));
        assertTrue(list.get(0).get("apiKeyPrefix").asText().endsWith("…"));

        assertEquals(RateLimitTier.HIGH,
                apiSessionTokenRepository.findByApiKey(fullKey).orElseThrow().rateLimitTier());

        mockMvc.perform(delete("/api/v1/admin/session-tokens/" + tokenId)
                        .header(HeaderBasedUserContext.USER_ID_HEADER, AdminAccessGuard.ADMIN_USER_ID))
                .andExpect(status().isOk());

        assertTrue(apiSessionTokenRepository.findByApiKey(fullKey).isEmpty());
    }
}
