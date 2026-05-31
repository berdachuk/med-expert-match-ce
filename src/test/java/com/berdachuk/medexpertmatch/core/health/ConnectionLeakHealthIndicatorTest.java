package com.berdachuk.medexpertmatch.core.health;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConnectionLeakHealthIndicatorTest {

    @Test
    void shouldReportUnknownForNonHikariDataSource() {
        var indicator = new ConnectionLeakHealthIndicator(new SimpleDataSource());
        var health = indicator.health();
        assertEquals("UNKNOWN", health.getStatus().getCode());
        assertTrue(health.getDetails().containsKey("message"));
    }

    private static class SimpleDataSource implements javax.sql.DataSource {
        @Override public java.sql.Connection getConnection() { return null; }
        @Override public java.sql.Connection getConnection(String u, String p) { return null; }
        @Override public java.io.PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(java.io.PrintWriter o) {}
        @Override public void setLoginTimeout(int s) {}
        @Override public int getLoginTimeout() { return 0; }
        @Override public java.util.logging.Logger getParentLogger() { return null; }
        @Override public <T> T unwrap(Class<T> iface) { return null; }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }
}
