package com.berdachuk.medexpertmatch.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RetryWithBackoff Tests")
class RetryWithBackoffTest {

    @Test
    @DisplayName("Should succeed on first attempt when no exception thrown")
    void shouldSucceedOnFirstAttemptWhenNoExceptionThrown() throws Exception {
        AtomicInteger attemptCount = new AtomicInteger(0);
        RetryWithBackoff retry = new RetryWithBackoff.Builder()
                .maxRetries(3)
                .initialDelayMillis(100)
                .backoffMultiplier(2.0)
                .maxDelayMillis(5000)
                .build();

        String result = retry.execute(() -> {
            attemptCount.incrementAndGet();
            return "success";
        });

        assertThat(result).isEqualTo("success");
        assertThat(attemptCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should retry on exception and succeed on second attempt")
    void shouldRetryOnExceptionAndSucceedOnSecondAttempt() throws Exception {
        AtomicInteger attemptCount = new AtomicInteger(0);
        RetryWithBackoff retry = new RetryWithBackoff.Builder()
                .maxRetries(3)
                .initialDelayMillis(10)
                .backoffMultiplier(2.0)
                .maxDelayMillis(100)
                .build();

        String result = retry.execute(() -> {
            attemptCount.incrementAndGet();
            if (attemptCount.get() < 2) {
                throw new RuntimeException("Temporary failure");
            }
            return "success";
        });

        assertThat(result).isEqualTo("success");
        assertThat(attemptCount.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should retry on exception and succeed on third attempt")
    void shouldRetryOnExceptionAndSucceedOnThirdAttempt() throws Exception {
        AtomicInteger attemptCount = new AtomicInteger(0);
        RetryWithBackoff retry = new RetryWithBackoff.Builder()
                .maxRetries(5)
                .initialDelayMillis(10)
                .backoffMultiplier(2.0)
                .maxDelayMillis(100)
                .build();

        String result = retry.execute(() -> {
            attemptCount.incrementAndGet();
            if (attemptCount.get() < 3) {
                throw new RuntimeException("Temporary failure");
            }
            return "success";
        });

        assertThat(result).isEqualTo("success");
        assertThat(attemptCount.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should fail after max retries exhausted")
    void shouldFailAfterMaxRetriesExhausted() {
        AtomicInteger attemptCount = new AtomicInteger(0);
        RetryWithBackoff retry = new RetryWithBackoff.Builder()
                .maxRetries(3)
                .initialDelayMillis(10)
                .backoffMultiplier(2.0)
                .maxDelayMillis(100)
                .build();

        assertThatThrownBy(() -> retry.execute(() -> {
            attemptCount.incrementAndGet();
            throw new RuntimeException("Persistent failure");
        })).isInstanceOf(RuntimeException.class).hasMessage("Persistent failure");

        assertThat(attemptCount.get()).isEqualTo(4);
    }

    @Test
    @DisplayName("Should respect maxRetries parameter")
    void shouldRespectMaxRetriesParameter() {
        AtomicInteger attemptCount = new AtomicInteger(0);
        RetryWithBackoff retry = new RetryWithBackoff.Builder()
                .maxRetries(2)
                .initialDelayMillis(10)
                .backoffMultiplier(2.0)
                .maxDelayMillis(100)
                .build();

        assertThatThrownBy(() -> retry.execute(() -> {
            attemptCount.incrementAndGet();
            throw new RuntimeException("Failure");
        })).isInstanceOf(RuntimeException.class);

        assertThat(attemptCount.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should apply exponential backoff delay")
    void shouldApplyExponentialBackoffDelay() {
        AtomicInteger attemptCount = new AtomicInteger(0);
        RetryWithBackoff retry = new RetryWithBackoff.Builder()
                .maxRetries(3)
                .initialDelayMillis(100)
                .backoffMultiplier(2.0)
                .maxDelayMillis(5000)
                .build();

        long startTime = System.currentTimeMillis();
        try {
            retry.execute(() -> {
                attemptCount.incrementAndGet();
                throw new RuntimeException("Failure");
            });
        } catch (RuntimeException e) {
            // Expected
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        long endTime = System.currentTimeMillis();

        long expectedMinTime = 100 + 200; // First two delays
        assertThat(endTime - startTime).isGreaterThanOrEqualTo(expectedMinTime - 10);
        assertThat(attemptCount.get()).isEqualTo(4);
    }

    @Test
    @DisplayName("Should cap delay at maxDelayMillis")
    void shouldCapDelayAtMaxDelayMillis() {
        AtomicInteger attemptCount = new AtomicInteger(0);
        RetryWithBackoff retry = new RetryWithBackoff.Builder()
                .maxRetries(3)
                .initialDelayMillis(100)
                .backoffMultiplier(10.0)
                .maxDelayMillis(200)
                .build();

        long startTime = System.currentTimeMillis();
        try {
            retry.execute(() -> {
                attemptCount.incrementAndGet();
                throw new RuntimeException("Failure");
            });
        } catch (RuntimeException e) {
            // Expected
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        long endTime = System.currentTimeMillis();

        // Delay should be capped at 200ms, so total time should be around 100 + 200 = 300ms minimum
        long expectedMinTime = 100 + 200;
        assertThat(endTime - startTime).isGreaterThanOrEqualTo(expectedMinTime - 10);
        assertThat(endTime - startTime).isLessThan(1000);
    }

    @Test
    @DisplayName("Should work with Runnable operation")
    void shouldWorkWithRunnableOperation() throws Exception {
        AtomicInteger attemptCount = new AtomicInteger(0);
        RetryWithBackoff retry = new RetryWithBackoff.Builder()
                .maxRetries(3)
                .initialDelayMillis(10)
                .backoffMultiplier(2.0)
                .maxDelayMillis(100)
                .build();

        retry.executeRunnable(() -> {
            attemptCount.incrementAndGet();
            if (attemptCount.get() < 2) {
                throw new RuntimeException("Temporary failure");
            }
        });

        assertThat(attemptCount.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle null return value")
    void shouldHandleNullReturnValue() throws Exception {
        AtomicInteger attemptCount = new AtomicInteger(0);
        RetryWithBackoff retry = new RetryWithBackoff.Builder()
                .maxRetries(3)
                .initialDelayMillis(10)
                .backoffMultiplier(2.0)
                .maxDelayMillis(100)
                .build();

        String result = retry.execute(() -> {
            attemptCount.incrementAndGet();
            return null;
        });

        assertThat(result).isNull();
        assertThat(attemptCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should propagate wrapped exception cause")
    void shouldPropagateWrappedExceptionCause() {
        RuntimeException originalException = new RuntimeException("Original error");
        RetryWithBackoff retry = new RetryWithBackoff.Builder()
                .maxRetries(2)
                .initialDelayMillis(10)
                .backoffMultiplier(2.0)
                .maxDelayMillis(100)
                .build();

        assertThatThrownBy(() -> retry.execute(() -> {
            throw originalException;
        })).isInstanceOf(RuntimeException.class).hasMessage("Original error");
    }

    @Test
    @DisplayName("createDefault should create default configuration")
    void createDefaultShouldCreateDefaultConfiguration() {
        RetryWithBackoff retry = RetryWithBackoff.createDefault();

        AtomicInteger attemptCount = new AtomicInteger(0);
        try {
            retry.execute(() -> {
                attemptCount.incrementAndGet();
                throw new RuntimeException("Failure");
            });
        } catch (RuntimeException e) {
            // Expected
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertThat(attemptCount.get()).isEqualTo(4); // Default maxRetries = 3 means 4 attempts (0, 1, 2, 3)
    }

    @Test
    @DisplayName("Builder should use custom configuration")
    void builderShouldUseCustomConfiguration() {
        RetryWithBackoff retry = new RetryWithBackoff.Builder()
                .maxRetries(5)
                .initialDelayMillis(50)
                .backoffMultiplier(3.0)
                .maxDelayMillis(3000)
                .build();

        AtomicInteger attemptCount = new AtomicInteger(0);
        try {
            retry.execute(() -> {
                attemptCount.incrementAndGet();
                throw new RuntimeException("Failure");
            });
        } catch (RuntimeException e) {
            // Expected
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertThat(attemptCount.get()).isEqualTo(6); // maxRetries = 5 means 6 attempts
    }

    @Test
    @DisplayName("Should throw IllegalStateException for interrupted thread")
    void shouldThrowIllegalStateExceptionForInterruptedThread() {
        AtomicInteger attemptCount = new AtomicInteger(0);
        RetryWithBackoff retry = new RetryWithBackoff.Builder()
                .maxRetries(3)
                .initialDelayMillis(10)
                .backoffMultiplier(2.0)
                .maxDelayMillis(100)
                .build();

        Thread.currentThread().interrupt();

        try {
            retry.execute(() -> {
                attemptCount.incrementAndGet();
                throw new RuntimeException("Failure");
            });
            Thread.interrupted(); // Clear interrupt status
        } catch (IllegalStateException e) {
            Thread.interrupted(); // Clear interrupt status
            assertThat(e).isInstanceOf(IllegalStateException.class);
            assertThat(e.getMessage()).contains("interrupted");
        } catch (Exception e) {
            Thread.interrupted(); // Clear interrupt status
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Should handle zero maxRetries")
    void shouldHandleZeroMaxRetries() {
        AtomicInteger attemptCount = new AtomicInteger(0);
        RetryWithBackoff retry = new RetryWithBackoff.Builder()
                .maxRetries(0)
                .initialDelayMillis(10)
                .backoffMultiplier(2.0)
                .maxDelayMillis(100)
                .build();

        assertThatThrownBy(() -> retry.execute(() -> {
            attemptCount.incrementAndGet();
            throw new RuntimeException("Failure");
        })).isInstanceOf(RuntimeException.class);

        assertThat(attemptCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("createWithMaxRetries should create RetryWithBackoff with custom max retries")
    void createWithMaxRetriesShouldCreateRetryWithBackoffWithCustomMaxRetries() {
        RetryWithBackoff retry = RetryWithBackoff.createWithMaxRetries(2);

        AtomicInteger attemptCount = new AtomicInteger(0);
        try {
            retry.execute(() -> {
                attemptCount.incrementAndGet();
                throw new RuntimeException("Failure");
            });
        } catch (RuntimeException e) {
            // Expected
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertThat(attemptCount.get()).isEqualTo(3); // maxRetries = 2 means 3 attempts
    }
}