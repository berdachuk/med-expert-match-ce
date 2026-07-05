package com.berdachuk.medexpertmatch.chat.rest;

import com.berdachuk.medexpertmatch.chat.service.impl.ChatExportAuditorImpl;
import com.berdachuk.medexpertmatch.core.domain.AuditLog;
import com.berdachuk.medexpertmatch.core.repository.AuditLogRepository;
import com.berdachuk.medexpertmatch.core.security.HeaderBasedUserContext;
import com.berdachuk.medexpertmatch.core.util.IdentifierHasher;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REQ-014: integration coverage for registered requirement.
 * REQ-016: integration coverage for registered requirement.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ChatExportAuditIT extends BaseIntegrationTest {

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
    @DisplayName("Export writes hashed audit log without raw user or chat ids")
    void exportAuditLog() throws Exception {
        String userId = "chat-export-audit-user";
        var createResult = mockMvc.perform(post("/api/v1/chats")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Audit test\",\"agentId\":\"auto\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String chatId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/chats/" + chatId + "/export")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId))
                .andExpect(status().isOk());

        List<AuditLog> logs = auditLogRepository.findByAction(ChatExportAuditorImpl.ACTION, 10);
        assertEquals(1, logs.size());
        AuditLog log = logs.getFirst();
        assertEquals(IdentifierHasher.sha256Hex(chatId), log.resourceId());
        assertEquals(IdentifierHasher.sha256Hex(userId), log.actor());
        assertFalse(log.resourceId().contains(chatId));
        assertFalse(log.actor().contains(userId));
    }
}
