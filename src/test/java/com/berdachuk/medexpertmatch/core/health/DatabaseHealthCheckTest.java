package com.berdachuk.medexpertmatch.core.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DatabaseHealthCheck Tests")
class DatabaseHealthCheckTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Should return healthy status when database query succeeds")
    void shouldReturnHealthyStatusWhenDatabaseQuerySucceeds() {
        DatabaseHealthCheck healthCheck = new DatabaseHealthCheck(jdbcTemplate);
        when(jdbcTemplate.queryForObject(anyString(), any(Class.class))).thenReturn(1);

        HealthCheck.HealthStatus status = healthCheck.check();

        assertThat(status.healthy()).isTrue();
        assertThat(status.message()).contains("database is healthy");
        assertThat(status.details()).containsKey("responseTime");
    }

    @Test
    @DisplayName("Should return unhealthy status when query result is unexpected")
    void shouldReturnUnhealthyStatusWhenQueryResultIsUnexpected() {
        DatabaseHealthCheck healthCheck = new DatabaseHealthCheck(jdbcTemplate);
        when(jdbcTemplate.queryForObject(anyString(), any(Class.class))).thenReturn(42);

        HealthCheck.HealthStatus status = healthCheck.check();

        assertThat(status.healthy()).isFalse();
        assertThat(status.message()).contains("Unexpected query result");
        assertThat(status.message()).contains("42");
    }

    @Test
    @DisplayName("Should return unhealthy status when database connection fails")
    void shouldReturnUnhealthyStatusWhenDatabaseConnectionFails() {
        DatabaseHealthCheck healthCheck = new DatabaseHealthCheck(jdbcTemplate);
        when(jdbcTemplate.queryForObject(anyString(), any(Class.class)))
                .thenThrow(new RuntimeException("Connection timeout"));

        HealthCheck.HealthStatus status = healthCheck.check();

        assertThat(status.healthy()).isFalse();
        assertThat(status.message()).contains("Connection failed");
        assertThat(status.details()).containsKey("error");
        assertThat(status.details()).containsKey("message");
    }

    @Test
    @DisplayName("Should return correct name")
    void shouldReturnCorrectName() {
        DatabaseHealthCheck healthCheck = new DatabaseHealthCheck(jdbcTemplate);

        assertThat(healthCheck.getName()).isEqualTo("database");
    }

    @Test
    @DisplayName("Should include response time in healthy details")
    void shouldIncludeResponseTimeInHealthyDetails() {
        DatabaseHealthCheck healthCheck = new DatabaseHealthCheck(jdbcTemplate);
        when(jdbcTemplate.queryForObject(anyString(), any(Class.class))).thenReturn(1);

        HealthCheck.HealthStatus status = healthCheck.check();

        assertThat(status.details().get("responseTime")).isNotNull();
        assertThat(status.details().get("responseTime").toString()).endsWith("ms");
    }

    @Test
    @DisplayName("Should handle null query result")
    void shouldHandleNullQueryResult() {
        DatabaseHealthCheck healthCheck = new DatabaseHealthCheck(jdbcTemplate);
        when(jdbcTemplate.queryForObject(anyString(), any(Class.class))).thenReturn(null);

        HealthCheck.HealthStatus status = healthCheck.check();

        assertThat(status.healthy()).isFalse();
        assertThat(status.message()).contains("Unexpected query result");
        assertThat(status.message()).contains("null");
    }
}