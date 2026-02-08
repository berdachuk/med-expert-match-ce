package com.berdachuk.medexpertmatch.core.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for performing health checks on system dependencies.
 * Aggregates multiple health checks and provides unified health status.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckService {

    private final List<HealthCheck> healthChecks;

    /**
     * Performs all health checks and returns aggregated results.
     *
     * @return Aggregated health status with results from all checks
     */
    public Map<String, Object> performHealthChecks() {
        Map<String, Object> overallResult = new HashMap<>();
        Map<String, HealthCheck.HealthStatus> results = new HashMap<>();
        boolean allHealthy = true;
        long totalStartTime = System.currentTimeMillis();

        for (HealthCheck healthCheck : healthChecks) {
            try {
                HealthCheck.HealthStatus status = healthCheck.check();
                results.put(healthCheck.getName(), status);

                if (!status.healthy()) {
                    allHealthy = false;
                    log.warn("Health check '{}' failed: {}", healthCheck.getName(), status.message());
                } else {
                    log.debug("Health check '{}' passed", healthCheck.getName());
                }
            } catch (Exception e) {
                allHealthy = false;
                log.error("Health check '{}' threw exception", healthCheck.getName(), e);
                results.put(healthCheck.getName(),
                        HealthCheck.HealthStatus.unhealthy(
                                healthCheck.getName(),
                                "Check execution failed: " + e.getMessage()
                        ));
            }
        }

        long totalDuration = System.currentTimeMillis() - totalStartTime;

        overallResult.put("healthy", allHealthy);
        overallResult.put("checkTimeMillis", totalDuration);
        overallResult.put("checks", results);
        overallResult.put("totalChecks", healthChecks.size());

        if (allHealthy) {
            overallResult.put("message", "All health checks passed");
        } else {
            long unhealthyCount = results.values().stream().filter(s -> !s.healthy()).count();
            overallResult.put("message", String.format("%d/%d health checks failed", unhealthyCount, healthChecks.size()));
        }

        return overallResult;
    }

    /**
     * Performs a specific health check by name.
     *
     * @param name Name of the health check to perform
     * @return Health status of the specified check, or null if not found
     */
    public HealthCheck.HealthStatus performHealthCheck(String name) {
        return healthChecks.stream()
                .filter(hc -> hc.getName().equals(name))
                .findFirst()
                .map(hc -> {
                    try {
                        return hc.check();
                    } catch (Exception e) {
                        log.error("Health check '{}' threw exception", name, e);
                        return HealthCheck.HealthStatus.unhealthy(
                                name,
                                "Check execution failed: " + e.getMessage()
                        );
                    }
                })
                .orElse(null);
    }

    /**
     * Gets all registered health check names.
     *
     * @return List of health check names
     */
    public List<String> getHealthCheckNames() {
        return healthChecks.stream().map(HealthCheck::getName).toList();
    }
}