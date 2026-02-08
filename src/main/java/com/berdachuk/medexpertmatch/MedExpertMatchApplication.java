package com.berdachuk.medexpertmatch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

import java.util.TimeZone;

/**
 * MedExpertMatch Application
 * <p>
 * AI-powered medical expert recommendation system
 * that matches medical cases with appropriate specialists using MedGemma models,
 * hybrid GraphRAG architecture, and intelligent agent skills.
 */
@Slf4j
@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class MedExpertMatchApplication implements ApplicationListener<WebServerInitializedEvent> {

    @Autowired
    private Environment environment;

    public static void main(String[] args) {
        // Set default timezone to UTC
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        SpringApplication.run(MedExpertMatchApplication.class, args);
    }

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        int port = event.getWebServer().getPort();
        String address = environment.getProperty("server.address", "localhost");
        String protocol = environment.getProperty("server.ssl.enabled", Boolean.class, false)
                ? "https"
                : "http";
        String baseUrl = String.format("%s://%s:%d", protocol, address, port);

        log.info("");
        log.info("MedExpertMatch Application Started");
        log.info("Application URLs:");
        log.info("  UI:              {}", baseUrl + "/");
        log.info("  Health:           {}", baseUrl + "/actuator/health");
        log.info("  Swagger UI:       {}", baseUrl + "/swagger-ui.html");
        log.info("  OpenAPI JSON:     {}", baseUrl + "/api/v1/openapi.json");
        log.info("");
    }
}
