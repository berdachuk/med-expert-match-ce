package com.berdachuk.medexpertmatch.core.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for health check endpoints.
 * Provides HTTP endpoints for monitoring system health.
 */
@Slf4j
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthCheckController {

    private final HealthCheckService healthCheckService;

    /**
     * Performs all health checks and returns aggregated results.
     *
     * @return Aggregated health status
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> checkAll() {
        log.debug("Performing all health checks");
        Map<String, Object> results = healthCheckService.performHealthChecks();
        Boolean isHealthy = (Boolean) results.get("healthy");
        return ResponseEntity.status(isHealthy ? 200 : 503).body(results);
    }

    /**
     * Performs a specific health check by name.
     *
     * @param name Name of the health check to perform
     * @return Health status of the specified check
     */
    @GetMapping("/{name}")
    public ResponseEntity<?> checkSpecific(@PathVariable String name) {
        log.debug("Performing health check for: {}", name);
        HealthCheck.HealthStatus status = healthCheckService.performHealthCheck(name);

        if (status == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.status(status.healthy() ? 200 : 503).body(status);
    }

    /**
     * Gets all registered health check names.
     *
     * @return List of health check names
     */
    @GetMapping("/list")
    public ResponseEntity<List<String>> listHealthChecks() {
        List<String> names = healthCheckService.getHealthCheckNames();
        return ResponseEntity.ok(names);
    }
}
