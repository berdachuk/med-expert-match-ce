package com.berdachuk.medexpertmatch.chat.service;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class ChatRateLimitLowLimitTestConfig {

    @Bean
    @Primary
    ChatRateLimitService chatRateLimitService(ChatTurnMetrics chatTurnMetrics) {
        return ChatRateLimitService.withLimits(chatTurnMetrics, 2, 10);
    }
}
