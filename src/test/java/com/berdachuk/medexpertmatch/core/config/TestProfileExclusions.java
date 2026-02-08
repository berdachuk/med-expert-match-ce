package com.berdachuk.medexpertmatch.core.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

/**
 * Test profile exclusions to prevent Spring AI auto-configuration.
 */
@TestConfiguration
@Profile("test")
@PropertySource("classpath:application-test.properties")
public class TestProfileExclusions {
    // Empty class - used for property source loading
}
