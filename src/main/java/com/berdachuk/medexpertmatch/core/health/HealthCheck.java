package com.berdachuk.medexpertmatch.core.health;

import java.util.Map;

/**
 * Interface for health checks of system dependencies.
 * Provides proactive monitoring of service health status.
 */
public interface HealthCheck {

    /**
     * Performs a health check for the service/component.
     *
     * @return Health status with details
     */
    HealthStatus check();

    /**
     * Gets the name of this health check.
     *
     * @return Name of the health check
     */
    String getName();

    /**
     * Represents the health status of a service/component.
     */
    record HealthStatus(
            boolean healthy,
            String message,
            long checkTimeMillis,
            Map<String, Object> details
    ) {
        public static HealthStatus healthy(String name) {
            return new HealthStatus(
                    true,
                    name + " is healthy",
                    System.currentTimeMillis(),
                    Map.of()
            );
        }

        public static HealthStatus unhealthy(String name, String reason) {
            return new HealthStatus(
                    false,
                    name + " is unhealthy: " + reason,
                    System.currentTimeMillis(),
                    Map.of("reason", reason)
            );
        }

        public static HealthStatus unhealthy(String name, String reason, Map<String, Object> details) {
            Map<String, Object> combinedDetails = new java.util.HashMap<>(details);
            combinedDetails.put("reason", reason);
            return new HealthStatus(
                    false,
                    name + " is unhealthy: " + reason,
                    System.currentTimeMillis(),
                    combinedDetails
            );
        }
    }
}