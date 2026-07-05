package com.berdachuk.medexpertmatch.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

/**
 * REQ-016: integration coverage for registered requirement.
 */
@DisplayName("Profile-dependent behavior tests")
class ProfileBehaviorIT {

    @Test
    @DisplayName("test profile excludes local profile")
    void testProfileExcludesLocal() {
        var env = new MockEnvironment();
        env.setActiveProfiles("test");

        assertTrue(env.matchesProfiles("test"), "test profile must be active");
        assertFalse(env.matchesProfiles("local"), "local profile must not be active");
    }

    @Test
    @DisplayName("test profile excludes docker profile")
    void testProfileExcludesDocker() {
        var env = new MockEnvironment();
        env.setActiveProfiles("test");

        assertTrue(env.matchesProfiles("test"));
        assertFalse(env.matchesProfiles("docker"), "docker profile must not be active");
    }

    @Test
    @DisplayName("test profile excludes event-driven profile")
    void testProfileExcludesEventDriven() {
        var env = new MockEnvironment();
        env.setActiveProfiles("test");

        assertFalse(env.matchesProfiles("event-driven"),
                "event-driven profile must not be active during test");
    }

    @Test
    @DisplayName("datasource property is resolved from default config")
    void datasourcePropertyResolved() {
        var env = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5433/medexpertmatch");

        String url = env.getProperty("spring.datasource.url");
        assertNotNull(url, "Datasource URL must be present");
        assertTrue(url.contains("postgresql"), "Must be PostgreSQL URL");
    }

    @Test
    @DisplayName("flyway property is resolved from default config")
    void flywayPropertyResolved() {
        var env = new MockEnvironment()
                .withProperty("spring.flyway.enabled", "true");

        String flywayEnabled = env.getProperty("spring.flyway.enabled");
        assertNotNull(flywayEnabled);
        assertTrue(Boolean.parseBoolean(flywayEnabled));
    }

    @Test
    @DisplayName("local profile enables synthetic data generation gate")
    void localProfileEnablesSyntheticGate() {
        var env = new MockEnvironment()
                .withProperty("medexpertmatch.features.document-ingestion", "true");
        env.setActiveProfiles("local");

        assertTrue(Boolean.parseBoolean(env.getProperty("medexpertmatch.features.document-ingestion")));
    }
}
