package com.berdachuk.medexpertmatch.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Test configuration listener to verify test AI mocks are loaded.
 */
@Slf4j
@TestConfiguration
@Profile("test")
public class TestAIConfigListener implements ApplicationListener<ContextRefreshedEvent> {

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("Test AI configuration loaded - using MOCK ChatModel and EmbeddingModel");
    }
}
