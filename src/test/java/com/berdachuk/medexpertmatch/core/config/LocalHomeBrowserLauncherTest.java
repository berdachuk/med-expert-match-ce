package com.berdachuk.medexpertmatch.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class LocalHomeBrowserLauncherTest {

    @Test
    void shouldBeInstantiable() {
        LocalHomeBrowserLauncher launcher = new LocalHomeBrowserLauncher();
        assertNotNull(launcher);
    }

    @Test
    void shouldHandleApplicationReadyEventWithoutThrowing() {
        LocalHomeBrowserLauncher launcher = new LocalHomeBrowserLauncher();
        ReflectionTestUtils.setField(launcher, "port", 8080);

        ApplicationReadyEvent event = mockEvent();
        // In headless test environment, Desktop.browse will throw, but the launcher
        // catches the exception and logs a fallback message.
        assertDoesNotThrow(() -> launcher.onApplicationEvent(event));
    }

    @Test
    void shouldHandleHeadlessEnvironment() {
        LocalHomeBrowserLauncher launcher = new LocalHomeBrowserLauncher();
        ReflectionTestUtils.setField(launcher, "port", 9090);

        ApplicationReadyEvent event = mockEvent();
        // No exception should propagate from the launcher regardless of environment
        assertDoesNotThrow(() -> launcher.onApplicationEvent(event));
    }

    private ApplicationReadyEvent mockEvent() {
        ConfigurableApplicationContext ctx = mock(ConfigurableApplicationContext.class);
        SpringApplication app = new SpringApplication();
        return new ApplicationReadyEvent(app, new String[]{}, ctx, java.time.Duration.ZERO);
    }
}
