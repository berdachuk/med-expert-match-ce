package com.berdachuk.medexpertmatch.system.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class ReadinessStateHealthIndicator implements HealthIndicator {

    private final AtomicBoolean ready = new AtomicBoolean(false);

    @EventListener
    public void onReadinessChange(AvailabilityChangeEvent<ReadinessState> event) {
        boolean wasReady = ready.get();
        boolean nowReady = ReadinessState.ACCEPTING_TRAFFIC.equals(event.getState());
        ready.set(nowReady);
        if (wasReady != nowReady) {
            log.info("Readiness state changed: {} -> {}", wasReady ? "READY" : "NOT_READY",
                    nowReady ? "READY" : "NOT_READY");
        }
    }

    @Override
    public Health health() {
        if (ready.get()) {
            return Health.up()
                    .withDetail("readinessState", "ACCEPTING_TRAFFIC")
                    .build();
        }
        return Health.down()
                .withDetail("readinessState", "REFUSING_TRAFFIC")
                .withDetail("reason", "Application is starting up or shutting down")
                .build();
    }
}
