package com.berdachuk.medexpertmatch.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LlmCallLimiterRoleTest {

    @Test
    @DisplayName("tracks clinical and utility limits separately")
    void separateClinicalAndUtilityLimits() {
        LlmCallLimiter limiter = new LlmCallLimiter(2, 4, 1, 1, 3, null);

        assertEquals(2, limiter.getMaxConcurrentCalls(LlmClientType.CLINICAL));
        assertEquals(4, limiter.getMaxConcurrentCalls(LlmClientType.UTILITY));
        assertEquals(1, limiter.getMaxConcurrentCalls(LlmClientType.EMBEDDING));
        assertEquals(3, limiter.getMaxConcurrentCalls(LlmClientType.TOOL_CALLING));
    }
}
