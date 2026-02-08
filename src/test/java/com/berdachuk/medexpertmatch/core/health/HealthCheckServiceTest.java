package com.berdachuk.medexpertmatch.core.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("HealthCheckService Tests")
class HealthCheckServiceTest {

    private HealthCheck healthCheck1;
    private HealthCheck healthCheck2;
    private HealthCheckService healthCheckService;

    @BeforeEach
    void setUp() {
        healthCheck1 = mock(HealthCheck.class);
        healthCheck2 = mock(HealthCheck.class);
        when(healthCheck1.getName()).thenReturn("database");
        when(healthCheck2.getName()).thenReturn("graph-database");

        // Manually set the healthChecks list since @InjectMocks doesn't handle constructor injection of Lists well
        List<HealthCheck> healthCheckList = List.of(healthCheck1, healthCheck2);
        healthCheckService = new HealthCheckService(healthCheckList);
    }

    @Test
    @DisplayName("Should return healthy status when all checks pass")
    void shouldReturnHealthyStatusWhenAllChecksPass() {
        HealthCheck.HealthStatus status1 = new HealthCheck.HealthStatus(true, "database is healthy", System.currentTimeMillis(), Map.of());
        HealthCheck.HealthStatus status2 = new HealthCheck.HealthStatus(true, "graph-database is healthy", System.currentTimeMillis(), Map.of());
        when(healthCheck1.check()).thenReturn(status1);
        when(healthCheck2.check()).thenReturn(status2);

        var result = healthCheckService.performHealthChecks();

        assertThat(result.get("healthy")).isEqualTo(true);
        assertThat(result.get("message")).isEqualTo("All health checks passed");
        assertThat(result.get("totalChecks")).isEqualTo(2);
        @SuppressWarnings("unchecked")
        var checks = (Map<String, HealthCheck.HealthStatus>) result.get("checks");
        assertThat(checks).containsKeys("database", "graph-database");
    }

    @Test
    @DisplayName("Should return unhealthy status when one check fails")
    void shouldReturnUnhealthyStatusWhenOneCheckFails() {
        HealthCheck.HealthStatus status1 = new HealthCheck.HealthStatus(true, "database is healthy", System.currentTimeMillis(), Map.of());
        HealthCheck.HealthStatus status2 = new HealthCheck.HealthStatus(false, "graph does not exist", System.currentTimeMillis(), Map.of());
        when(healthCheck1.check()).thenReturn(status1);
        when(healthCheck2.check()).thenReturn(status2);

        var result = healthCheckService.performHealthChecks();

        assertThat(result.get("healthy")).isEqualTo(false);
        assertThat(result.get("message")).isEqualTo("1/2 health checks failed");
        assertThat(result.get("totalChecks")).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return unhealthy status when all checks fail")
    void shouldReturnUnhealthyStatusWhenAllChecksFail() {
        HealthCheck.HealthStatus status1 = new HealthCheck.HealthStatus(false, "database connection failed", System.currentTimeMillis(), Map.of());
        HealthCheck.HealthStatus status2 = new HealthCheck.HealthStatus(false, "graph does not exist", System.currentTimeMillis(), Map.of());
        when(healthCheck1.check()).thenReturn(status1);
        when(healthCheck2.check()).thenReturn(status2);

        var result = healthCheckService.performHealthChecks();

        assertThat(result.get("healthy")).isEqualTo(false);
        assertThat(result.get("message")).isEqualTo("2/2 health checks failed");
    }

    @Test
    @DisplayName("Should handle health check exceptions gracefully")
    void shouldHandleHealthCheckExceptionsGracefully() {
        HealthCheck.HealthStatus status1 = new HealthCheck.HealthStatus(true, "database is healthy", System.currentTimeMillis(), Map.of());
        when(healthCheck1.check()).thenReturn(status1);
        when(healthCheck2.check()).thenThrow(new RuntimeException("Unexpected error"));

        var result = healthCheckService.performHealthChecks();

        assertThat(result.get("healthy")).isEqualTo(false);
        @SuppressWarnings("unchecked")
        var checks = (Map<String, HealthCheck.HealthStatus>) result.get("checks");
        assertThat(checks).containsKeys("database", "graph-database");

        var databaseStatus = checks.get("database");
        var graphStatus = checks.get("graph-database");

        assertThat(databaseStatus.healthy()).isTrue();
        assertThat(graphStatus.healthy()).isFalse();
        assertThat(graphStatus.message()).contains("Check execution failed");
    }

    @Test
    @DisplayName("Should perform specific health check by name")
    void shouldPerformSpecificHealthCheckByName() {
        HealthCheck.HealthStatus status1 = new HealthCheck.HealthStatus(true, "database is healthy", System.currentTimeMillis(), Map.of());
        when(healthCheck1.check()).thenReturn(status1);

        var result = healthCheckService.performHealthCheck("database");

        assertThat(result).isNotNull();
        assertThat(result.healthy()).isTrue();
        verify(healthCheck1).check();
        verify(healthCheck2, never()).check();
    }

    @Test
    @DisplayName("Should return null when health check name not found")
    void shouldReturnNullWhenHealthCheckNameNotFound() {
        var result = healthCheckService.performHealthCheck("non-existent");

        assertThat(result).isNull();
        verify(healthCheck1, never()).check();
        verify(healthCheck2, never()).check();
    }

    @Test
    @DisplayName("Should handle exception in specific health check")
    void shouldHandleExceptionInSpecificHealthCheck() {
        when(healthCheck1.check()).thenThrow(new RuntimeException("Connection error"));

        var result = healthCheckService.performHealthCheck("database");

        assertThat(result).isNotNull();
        assertThat(result.healthy()).isFalse();
        assertThat(result.message()).contains("Check execution failed");
    }

    @Test
    @DisplayName("Should return list of health check names")
    void shouldReturnListOfHealthCheckNames() {
        var names = healthCheckService.getHealthCheckNames();

        assertThat(names).hasSize(2);
        assertThat(names).contains("database", "graph-database");
    }

    @Test
    @DisplayName("Should include check time duration in results")
    void shouldIncludeCheckTimeDurationInResults() {
        HealthCheck.HealthStatus status1 = new HealthCheck.HealthStatus(true, "database is healthy", System.currentTimeMillis(), Map.of());
        HealthCheck.HealthStatus status2 = new HealthCheck.HealthStatus(true, "graph-database is healthy", System.currentTimeMillis(), Map.of());
        when(healthCheck1.check()).thenReturn(status1);
        when(healthCheck2.check()).thenReturn(status2);

        var result = healthCheckService.performHealthChecks();

        assertThat(result.get("checkTimeMillis")).isNotNull();
        assertThat(result.get("checkTimeMillis") instanceof Number || result.get("checkTimeMillis") instanceof Long).isTrue();
    }

    @Test
    @DisplayName("Should record individual health check statuses")
    void shouldRecordIndividualHealthCheckStatuses() {
        HealthCheck.HealthStatus status1 = new HealthCheck.HealthStatus(true, "database is healthy", System.currentTimeMillis(), Map.of("key", "value"));
        HealthCheck.HealthStatus status2 = new HealthCheck.HealthStatus(false, "graph does not exist", System.currentTimeMillis(), Map.of());
        when(healthCheck1.check()).thenReturn(status1);
        when(healthCheck2.check()).thenReturn(status2);

        var result = healthCheckService.performHealthChecks();
        @SuppressWarnings("unchecked")
        var checks = (Map<String, HealthCheck.HealthStatus>) result.get("checks");

        assertThat(checks).hasSize(2);
        assertThat(checks.get("database").healthy()).isTrue();
        assertThat(checks.get("database").message()).isEqualTo("database is healthy");
        assertThat(checks.get("graph-database").healthy()).isFalse();
        assertThat(checks.get("graph-database").message()).isEqualTo("graph does not exist");
    }
}
