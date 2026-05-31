package com.berdachuk.medexpertmatch.core.security;

import com.berdachuk.medexpertmatch.core.domain.ApiSessionToken;
import com.berdachuk.medexpertmatch.core.domain.RateLimitTier;
import com.berdachuk.medexpertmatch.core.repository.ApiSessionTokenRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeaderBasedUserContextTest {

    @Mock
    private ApiSessionTokenRepository apiSessionTokenRepository;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Defaults to DEFAULT tier when no API key is present")
    void defaultTierWithoutApiKey() {
        bindRequest(new MockHttpServletRequest("GET", "/api/v1/chats"));
        HeaderBasedUserContext context = new HeaderBasedUserContext(apiSessionTokenRepository);
        assertEquals(RateLimitTier.DEFAULT, context.currentRateLimitTier());
    }

    @Test
    @DisplayName("Resolves tier from API session token when X-API-Key is present")
    void tierFromApiSessionToken() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/chats");
        request.addHeader("X-API-Key", "high-tier-key");
        bindRequest(request);

        Instant now = Instant.now();
        when(apiSessionTokenRepository.findByApiKey("high-tier-key")).thenReturn(Optional.of(
                new ApiSessionToken("t1", "high-tier-key", "test", RateLimitTier.HIGH, now, now, null)));

        HeaderBasedUserContext context = new HeaderBasedUserContext(apiSessionTokenRepository);
        assertEquals(RateLimitTier.HIGH, context.currentRateLimitTier());
    }

    private void bindRequest(MockHttpServletRequest request) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
