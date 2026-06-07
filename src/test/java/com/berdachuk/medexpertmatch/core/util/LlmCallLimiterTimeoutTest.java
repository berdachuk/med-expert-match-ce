package com.berdachuk.medexpertmatch.core.util;

import com.berdachuk.medexpertmatch.core.monitoring.LlmLimiterMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LlmCallLimiterTimeoutTest {

    @Test
    @DisplayName("acquire timeout throws clear exception and records metric")
    void timesOutWhenPoolExhausted() throws InterruptedException {
        LlmLimiterMetrics limiterMetrics = mock(LlmLimiterMetrics.class);
        LlmCallLimiter limiter = new LlmCallLimiter(1, 1, 1, 1, 1, 1L, null, limiterMetrics);
        CountDownLatch holderStarted = new CountDownLatch(1);
        CountDownLatch releaseHolder = new CountDownLatch(1);

        Thread holder = new Thread(() -> limiter.execute(LlmClientType.CLINICAL, () -> {
            holderStarted.countDown();
            try {
                releaseHolder.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "held";
        }));
        holder.start();
        holderStarted.await(5, TimeUnit.SECONDS);

        assertThrows(LlmCallLimiterTimeoutException.class,
                () -> limiter.execute(LlmClientType.CLINICAL, () -> "blocked"));

        verify(limiterMetrics).recordTimeout(LlmClientType.CLINICAL);
        releaseHolder.countDown();
        holder.join(5000);
    }
}
