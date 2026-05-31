package com.berdachuk.medexpertmatch.core.util;

/**
 * Token-bucket rate limiter used by global API filter and per-user chat limits.
 */
public final class TokenBucket {

    private final double maxTokens;
    private final double refillRate;
    private double tokens;
    private long lastRefill;

    public TokenBucket(int maxRequests, int windowSeconds) {
        this.maxTokens = maxRequests;
        this.refillRate = (double) maxRequests / windowSeconds;
        this.tokens = maxRequests;
        this.lastRefill = System.nanoTime();
    }

    public synchronized boolean tryConsume() {
        refill();
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;
        }
        return false;
    }

    private void refill() {
        long now = System.nanoTime();
        double elapsed = (now - lastRefill) / 1_000_000_000.0;
        tokens = Math.min(maxTokens, tokens + elapsed * refillRate);
        lastRefill = now;
    }
}
