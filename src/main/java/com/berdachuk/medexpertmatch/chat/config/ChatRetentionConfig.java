package com.berdachuk.medexpertmatch.chat.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ChatRetentionProperties.class)
public class ChatRetentionConfig {
}
