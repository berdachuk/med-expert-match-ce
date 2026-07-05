package com.berdachuk.medexpertmatch.core.repository;

import com.berdachuk.medexpertmatch.core.domain.ApiSessionToken;
import com.berdachuk.medexpertmatch.core.domain.AuditLog;
import com.berdachuk.medexpertmatch.core.domain.RateLimitTier;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * REQ-016: integration coverage for registered requirement.
 */
class SessionAuditRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private ApiSessionTokenRepository apiSessionTokenRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @BeforeEach
    void setUp() {
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM audit_log");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM api_session_token");
    }

    @Test
    void insertAndFindApiSessionToken() {
        Instant now = Instant.now();
        ApiSessionToken token = new ApiSessionToken(
                IdGenerator.generateId(),
                "test-api-key-123",
                "Integration test token",
                RateLimitTier.HIGH,
                now.plusSeconds(3600),
                now,
                null);

        apiSessionTokenRepository.insert(token);

        ApiSessionToken loaded = apiSessionTokenRepository.findByApiKey("test-api-key-123").orElseThrow();
        assertEquals(token.id(), loaded.id());
        assertEquals(RateLimitTier.HIGH, loaded.rateLimitTier());
        assertEquals("Integration test token", loaded.description());
    }

    @Test
    void insertAndFindAuditLog() {
        Instant now = Instant.now();
        AuditLog auditLog = new AuditLog(
                IdGenerator.generateId(),
                "API_ACCESS",
                "endpoint",
                "/api/v1/agent/match",
                "system",
                Map.of("method", "POST", "status", 200),
                now);

        auditLogRepository.insert(auditLog);

        var results = auditLogRepository.findByAction("API_ACCESS", 10);
        assertEquals(1, results.size());
        assertEquals("/api/v1/agent/match", results.get(0).resourceId());
        assertTrue(results.get(0).details().containsKey("method"));
    }
}
