package com.berdachuk.medexpertmatch.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;

@TestConfiguration
@Profile("test")
public class TestAIConfigListener implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(TestAIConfigListener.class);

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("Test AI configuration loaded - using MOCK ChatModel and EmbeddingModel");
    }
}
