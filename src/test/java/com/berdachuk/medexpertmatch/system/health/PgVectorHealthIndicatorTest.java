package com.berdachuk.medexpertmatch.system.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PgVectorHealthIndicatorTest {

    @Test
    void shouldReturnUpWhenExtensionAvailable() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Collections.emptyMap()), eq(Integer.class)))
                .thenReturn(1)   // extension count
                .thenReturn(1)   // type count
                .thenReturn(1);  // hnsw index count

        PgVectorHealthIndicator indicator = new PgVectorHealthIndicator(jdbc);
        Health health = indicator.health();

        assertEquals("UP", health.getStatus().getCode());
        assertEquals(true, health.getDetails().get("extensionAvailable"));
        assertEquals("UP", health.getDetails().get("status"));
    }

    @Test
    void shouldReturnDownWhenExtensionMissing() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Collections.emptyMap()), eq(Integer.class)))
                .thenReturn(0)   // extension count
                .thenReturn(0)   // type count
                .thenReturn(0);  // hnsw index count

        PgVectorHealthIndicator indicator = new PgVectorHealthIndicator(jdbc);
        Health health = indicator.health();

        assertEquals("DOWN", health.getStatus().getCode());
        assertEquals(false, health.getDetails().get("extensionAvailable"));
        assertEquals("PgVector extension not available", health.getDetails().get("error"));
    }

    @Test
    void shouldReturnDownOnException() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Collections.emptyMap()), eq(Integer.class)))
                .thenThrow(new DataAccessResourceFailureException("DB unavailable"));

        PgVectorHealthIndicator indicator = new PgVectorHealthIndicator(jdbc);
        Health health = indicator.health();

        assertEquals("DOWN", health.getStatus().getCode());
        assertEquals("DB unavailable", health.getDetails().get("error"));
        assertEquals("DataAccessResourceFailureException", health.getDetails().get("exception"));
        assertNotNull(health.getDetails().get("status"));
    }

    @Test
    void shouldReturnUpWhenTypeCountPresentEvenIfExtensionMissing() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Collections.emptyMap()), eq(Integer.class)))
                .thenReturn(0)   // extension count
                .thenReturn(1)   // type count
                .thenReturn(0);  // hnsw index count

        PgVectorHealthIndicator indicator = new PgVectorHealthIndicator(jdbc);
        Health health = indicator.health();

        assertEquals("UP", health.getStatus().getCode());
        assertTrue((boolean) health.getDetails().get("extensionAvailable"));
    }
}
