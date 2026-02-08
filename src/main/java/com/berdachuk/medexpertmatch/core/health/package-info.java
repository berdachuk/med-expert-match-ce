/**
 * Core health monitoring - Health check implementations.
 * <p>
 * This package contains core health monitoring classes used across modules:
 * - HealthCheck interface and base implementation
 * - DatabaseHealthCheck for database health monitoring
 * - HealthCheckService for managing health checks
 * - HealthCheckController for health check REST endpoints
 */
@org.springframework.modulith.NamedInterface("health")
package com.berdachuk.medexpertmatch.core.health;