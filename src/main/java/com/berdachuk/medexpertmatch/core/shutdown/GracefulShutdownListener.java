package com.berdachuk.medexpertmatch.core.shutdown;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
public class GracefulShutdownListener implements ApplicationListener<ContextClosedEvent> {

    private static final Duration MAX_DRAIN_WAIT = Duration.ofSeconds(25);

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        int remaining = RequestInFlightFilter.getInFlightCount();
        log.info("Graceful shutdown initiated, {} in-flight requests", remaining);

        AvailabilityChangeEvent.publish(event.getApplicationContext(), ReadinessState.REFUSING_TRAFFIC);

        if (remaining > 0) {
            Instant deadline = Instant.now().plus(MAX_DRAIN_WAIT);
            while (RequestInFlightFilter.getInFlightCount() > 0 && Instant.now().isBefore(deadline)) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            remaining = RequestInFlightFilter.getInFlightCount();
            if (remaining > 0) {
                log.warn("Shutdown proceeding with {} in-flight requests after drain timeout", remaining);
            } else {
                log.info("All in-flight requests drained");
            }
        }
    }
}
