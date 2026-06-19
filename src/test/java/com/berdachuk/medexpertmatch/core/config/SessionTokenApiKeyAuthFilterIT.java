package com.berdachuk.medexpertmatch.core.config;

import com.berdachuk.medexpertmatch.core.domain.ApiSessionToken;
import com.berdachuk.medexpertmatch.core.domain.RateLimitTier;
import com.berdachuk.medexpertmatch.core.repository.ApiSessionTokenRepository;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.evidence.service.PubMedService;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "medexpertmatch.auth.session-tokens.enabled=true",
        "medexpertmatch.auth.enabled=true"
})
class SessionTokenApiKeyAuthFilterIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApiSessionTokenRepository apiSessionTokenRepository;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @MockBean
    private PubMedService pubmedService;

    @BeforeEach
    void setUp() {
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM api_session_token");
        when(pubmedService.search(anyString(), anyInt())).thenReturn(List.of());
    }

    @Test
    @DisplayName("Rejects requests without valid session token API key")
    void rejectsMissingKey() throws Exception {
        mockMvc.perform(get("/api/v1/evidence/verify-pubmed"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Allows requests with valid session token API key")
    void allowsValidKey() throws Exception {
        String apiKey = "session-key-" + IdGenerator.generateId();
        Instant now = Instant.now();
        apiSessionTokenRepository.insert(new ApiSessionToken(
                IdGenerator.generateId(),
                apiKey,
                "filter test",
                RateLimitTier.DEFAULT,
                now.plusSeconds(3600),
                now,
                null));

        mockMvc.perform(get("/api/v1/evidence/verify-pubmed")
                        .header(SessionTokenApiKeyAuthFilter.HEADER_NAME, apiKey))
                .andExpect(status().isOk());
    }
}
