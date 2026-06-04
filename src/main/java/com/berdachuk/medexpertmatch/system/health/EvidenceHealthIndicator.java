package com.berdachuk.medexpertmatch.system.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class EvidenceHealthIndicator implements HealthIndicator {

    private static final String PUBMED_PING_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final RestTemplate restTemplate;

    public EvidenceHealthIndicator() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        Instant start = Instant.now();

        try {
            String response = restTemplate.getForObject(PUBMED_PING_URL, String.class);
            long responseTime = Duration.between(start, Instant.now()).toMillis();

            details.put("status", "UP");
            details.put("responseTime", responseTime + "ms");
            details.put("endpoint", PUBMED_PING_URL);
            details.put("reachable", response != null && !response.isBlank());

            return Health.up().withDetails(details).build();
        } catch (Exception e) {
            long responseTime = Duration.between(start, Instant.now()).toMillis();
            details.put("status", "DOWN");
            details.put("responseTime", responseTime + "ms");
            details.put("endpoint", PUBMED_PING_URL);
            details.put("error", e.getClass().getSimpleName());
            details.put("message", e.getMessage());

            log.warn("PubMed evidence API health check failed: {}", e.getMessage());
            return Health.down().withDetails(details).build();
        }
    }
}
