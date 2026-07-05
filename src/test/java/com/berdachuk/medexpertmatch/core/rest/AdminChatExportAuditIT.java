package com.berdachuk.medexpertmatch.core.rest;

import com.berdachuk.medexpertmatch.chat.service.impl.ChatExportAuditorImpl;
import com.berdachuk.medexpertmatch.core.domain.AuditLog;
import com.berdachuk.medexpertmatch.core.repository.AuditLogRepository;
import com.berdachuk.medexpertmatch.core.security.AdminAccessGuard;
import com.berdachuk.medexpertmatch.core.security.HeaderBasedUserContext;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REQ-016: integration coverage for registered requirement.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AdminChatExportAuditIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM audit_log");
    }

    @Test
    @DisplayName("Admin lists chat export audit entries with hashed ids")
    void listChatExportAudits() throws Exception {
        auditLogRepository.insert(new AuditLog(
                IdGenerator.generateId(),
                ChatExportAuditorImpl.ACTION,
                "chat",
                "hashed-chat-id",
                "hashed-user-id",
                Map.of("messageCount", 3),
                Instant.now()));

        var result = mockMvc.perform(get("/api/v1/admin/audit/chat-exports")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, AdminAccessGuard.ADMIN_USER_ID))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode rows = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(1, rows.size());
        assertEquals("hashed-chat-id", rows.get(0).get("resourceIdHash").asText());
        assertEquals(3, rows.get(0).get("details").get("messageCount").asInt());
    }

    @Test
    @DisplayName("Admin can filter chat export audits by action")
    void filterByAction() throws Exception {
        auditLogRepository.insert(new AuditLog(
                IdGenerator.generateId(),
                ChatExportAuditorImpl.BUNDLE_ACTION,
                "chat_bundle",
                "hashed-user",
                "hashed-user",
                Map.of("chatCount", 2, "messageCount", 5),
                Instant.now()));

        var bundleOnly = mockMvc.perform(get("/api/v1/admin/audit/chat-exports")
                        .param("action", ChatExportAuditorImpl.BUNDLE_ACTION)
                        .header(HeaderBasedUserContext.USER_ID_HEADER, AdminAccessGuard.ADMIN_USER_ID))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode rows = objectMapper.readTree(bundleOnly.getResponse().getContentAsString());
        assertEquals(1, rows.size());
        assertEquals(ChatExportAuditorImpl.BUNDLE_ACTION, rows.get(0).get("action").asText());
        assertEquals(2, rows.get(0).get("details").get("chatCount").asInt());
    }
}
