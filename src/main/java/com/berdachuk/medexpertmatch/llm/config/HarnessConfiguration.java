package com.berdachuk.medexpertmatch.llm.config;

import com.berdachuk.medexpertmatch.llm.harness.HarnessRetentionProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({
        HarnessProperties.class,
        HarnessRetentionProperties.class,
        MedicalConfidencePolicyProperties.class})
public class HarnessConfiguration {
}
