package com.berdachuk.medexpertmatch.web.controller;

import com.berdachuk.medexpertmatch.core.security.AdminAccessGuard;
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

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * REQ-016: integration coverage for registered requirement.
 * REQ-125: integration coverage for registered requirement.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class SessionTokensWebControllerIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Admin session tokens page loads for ?user=admin")
    void adminPageLoads() throws Exception {
        mockMvc.perform(get("/admin/session-tokens").param("user", "admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/session-tokens"));
    }

    @Test
    @DisplayName("Non-admin redirected from session tokens page")
    void nonAdminRedirected() throws Exception {
        mockMvc.perform(get("/admin/session-tokens"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Session tokens page highlights tokens expiring within 7 days")
    void highlightsExpiringTokens() throws Exception {
        String expiresAt = Instant.now().plusSeconds(3 * 86400L).toString();
        mockMvc.perform(post("/api/v1/admin/session-tokens")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, AdminAccessGuard.ADMIN_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "description", "Expiring soon",
                                "rateLimitTier", "DEFAULT",
                                "expiresAt", expiresAt))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/session-tokens").param("user", "admin"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Expires soon")));
    }
}
