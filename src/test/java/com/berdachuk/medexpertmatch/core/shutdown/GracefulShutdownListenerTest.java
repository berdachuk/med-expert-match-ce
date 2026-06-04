package com.berdachuk.medexpertmatch.core.shutdown;

import org.junit.jupiter.api.Test;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextClosedEvent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GracefulShutdownListenerTest {

    @Test
    void shouldPublishRefusingTrafficOnShutdown() {
        ApplicationContext context = mock(ApplicationContext.class);
        ContextClosedEvent event = new ContextClosedEvent(context);

        GracefulShutdownListener listener = new GracefulShutdownListener();
        listener.onApplicationEvent(event);

        verify(context).publishEvent(any(AvailabilityChangeEvent.class));
    }

    @Test
    void shouldHandleZeroInFlightRequests() {
        ApplicationContext context = mock(ApplicationContext.class);
        ContextClosedEvent event = new ContextClosedEvent(context);

        int before = RequestInFlightFilter.getInFlightCount();
        GracefulShutdownListener listener = new GracefulShutdownListener();
        listener.onApplicationEvent(event);

        org.junit.jupiter.api.Assertions.assertEquals(before, RequestInFlightFilter.getInFlightCount());
    }
}
