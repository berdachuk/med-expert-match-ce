package com.berdachuk.medexpertmatch.core.rest;

import com.berdachuk.medexpertmatch.chat.service.ChatRetentionMetrics;
import com.berdachuk.medexpertmatch.core.security.AdminAccessGuard;
import com.berdachuk.medexpertmatch.core.security.HeaderBasedUserContext;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REQ-016: integration coverage for registered requirement.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AdminChatRetentionIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChatRetentionMetrics chatRetentionMetrics;

    @Test
    @DisplayName("Admin chat-retention API returns config and last-run snapshot")
    void adminChatRetentionStats() throws Exception {
        chatRetentionMetrics.recordPurgeRun(Instant.parse("2026-05-31T03:00:00Z"), 2, 5, true, 30);

        mockMvc.perform(get("/api/v1/admin/chat-retention")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, AdminAccessGuard.ADMIN_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.idleDays").value(0))
                .andExpect(jsonPath("$.lastRunAt").value("2026-05-31T03:00:00Z"))
                .andExpect(jsonPath("$.lastChatsPurged").value(2))
                .andExpect(jsonPath("$.lastMessagesPurged").value(5));
    }

    @Test
    @DisplayName("Non-admin cannot access chat-retention API")
    void nonAdminForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/chat-retention")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, "regular-user"))
                .andExpect(status().isForbidden());
    }
}
