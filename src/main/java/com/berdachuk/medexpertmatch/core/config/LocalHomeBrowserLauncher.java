package com.berdachuk.medexpertmatch.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;

@Slf4j
@Component
@Profile("local")
@ConditionalOnProperty(prefix = "medexpertmatch.local", name = "open-browser-on-start", matchIfMissing = true)
public class LocalHomeBrowserLauncher implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${server.port:8080}")
    private int port;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String url = "http://localhost:" + port;
        try {
            Desktop.getDesktop().browse(URI.create(url));
            log.info("Browser opened at {}", url);
        } catch (Exception e) {
            log.info("Application ready at {} (browser auto-open not supported)", url);
        }
    }
}
