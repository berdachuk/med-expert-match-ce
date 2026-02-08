package com.berdachuk.medexpertmatch.core.util;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * Utility for retrying operations with exponential backoff.
 * Provides configurable retry behavior for external service calls.
 */
@Slf4j
public class RetryWithBackoff {

    private final int maxRetries;
    private final long initialDelayMillis;
    private final double backoffMultiplier;
    private final long maxDelayMillis;

    private RetryWithBackoff(Builder builder) {
        this.maxRetries = builder.maxRetries;
        this.initialDelayMillis = builder.initialDelayMillis;
        this.backoffMultiplier = builder.backoffMultiplier;
        this.maxDelayMillis = builder.maxDelayMillis;
    }

    /**
     * Creates a default RetryWithBackoff instance.
     */
    public static RetryWithBackoff createDefault() {
        return new Builder().build();
    }

    /**
     * Creates a RetryWithBackoff instance with custom max retries.
     */
    public static RetryWithBackoff createWithMaxRetries(int maxRetries) {
        return new Builder().maxRetries(maxRetries).build();
    }

    /**
     * Executes a supplier with retry logic and exponential backoff.
     *
     * @param supplier The operation to execute
     * @param <T>      The return type of the operation
     * @return The result of the successful operation
     * @throws Exception if all retries fail
     */
    public <T> T execute(Supplier<T> supplier) throws Exception {
        Exception lastException = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                if (attempt > 0) {
                    long delay = calculateDelay(attempt);
                    log.debug("Retry attempt {} after {} ms delay", attempt, delay);
                    Thread.sleep(delay);
                }

                T result = supplier.get();
                if (attempt > 0) {
                    log.info("Operation succeeded after {} attempt(s)", attempt + 1);
                }
                return result;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Thread interrupted during retry", e);
            } catch (Exception e) {
                lastException = e;
                log.warn("Operation failed on attempt {}/{}: {}",
                        attempt + 1, maxRetries + 1, e.getMessage());
                if (attempt < maxRetries) {
                    log.debug("Will retry in {} ms", calculateDelay(attempt + 1));
                }
            }
        }

        log.error("Operation failed after {} attempt(s)", maxRetries + 1);
        throw lastException;
    }

    /**
     * Executes a runnable with retry logic and exponential backoff.
     *
     * @param runnable The operation to execute
     * @throws Exception if all retries fail
     */
    public void executeRunnable(Runnable runnable) throws Exception {
        execute(() -> {
            runnable.run();
            return null;
        });
    }

    private long calculateDelay(int attempt) {
        long delay = (long) (initialDelayMillis * Math.pow(backoffMultiplier, attempt - 1));
        return Math.min(delay, maxDelayMillis);
    }

    /**
     * Builder for creating RetryWithBackoff instances.
     */
    public static class Builder {
        private int maxRetries = 3;
        private long initialDelayMillis = 1000; // 1 second
        private double backoffMultiplier = 2.0;
        private long maxDelayMillis = 10000; // 10 seconds

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder initialDelayMillis(long initialDelayMillis) {
            this.initialDelayMillis = initialDelayMillis;
            return this;
        }

        public Builder backoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
            return this;
        }

        public Builder maxDelayMillis(long maxDelayMillis) {
            this.maxDelayMillis = maxDelayMillis;
            return this;
        }

        public RetryWithBackoff build() {
            return new RetryWithBackoff(this);
        }
    }
}
