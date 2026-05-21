package com.berdachuk.medexpertmatch.core.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

@Component("hikariPool")
public class ConnectionLeakHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public ConnectionLeakHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        if (dataSource instanceof HikariDataSource hikari) {
            HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
            if (pool == null) {
                return Health.up()
                        .withDetail("activeConnections", hikari.getHikariConfigMXBean().getMaximumPoolSize())
                        .build();
            }
            int active = pool.getActiveConnections();
            int idle = pool.getIdleConnections();
            int total = pool.getTotalConnections();
            int pending = pool.getThreadsAwaitingConnection();
            int max = hikari.getMaximumPoolSize();

            var builder = Health.up()
                    .withDetail("activeConnections", active)
                    .withDetail("idleConnections", idle)
                    .withDetail("totalConnections", total)
                    .withDetail("maxConnections", max)
                    .withDetail("pendingThreads", pending);

            if (active >= max) {
                builder = Health.down()
                        .withDetail("activeConnections", active)
                        .withDetail("idleConnections", idle)
                        .withDetail("maxConnections", max);
            }

            return builder.build();
        }
        return Health.unknown().withDetail("message", "DataSource is not HikariCP").build();
    }
}
