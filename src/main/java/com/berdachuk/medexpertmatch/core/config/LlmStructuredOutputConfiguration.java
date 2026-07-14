package com.berdachuk.medexpertmatch.core.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LlmStructuredOutputProperties.class)
public class LlmStructuredOutputConfiguration {
}
