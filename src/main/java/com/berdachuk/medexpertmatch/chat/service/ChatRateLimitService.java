package com.berdachuk.medexpertmatch.chat.service;

import com.berdachuk.medexpertmatch.core.domain.RateLimitTier;
import com.berdachuk.medexpertmatch.core.util.TokenBucket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-user token bucket for chat SSE and A2A turns (M19, M25 scoped buckets).
 */
@Service
public class ChatRateLimitService {

    private static final int DEFAULT_WINDOW_SECONDS = 60;

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final ChatTurnMetrics chatTurnMetrics;
    private final int defaultLimit;
    private final int highLimit;

    @Autowired
    public ChatRateLimitService(ChatTurnMetrics chatTurnMetrics) {
        this(chatTurnMetrics, 10, 30);
    }

    private ChatRateLimitService(ChatTurnMetrics chatTurnMetrics, int defaultLimit, int highLimit) {
        this.chatTurnMetrics = chatTurnMetrics;
        this.defaultLimit = defaultLimit;
        this.highLimit = highLimit;
    }

    static ChatRateLimitService withLimits(ChatTurnMetrics chatTurnMetrics, int defaultLimit, int highLimit) {
        return new ChatRateLimitService(chatTurnMetrics, defaultLimit, highLimit);
    }

    public boolean tryAcquire(String userId) {
        return tryAcquire(userId, RateLimitTier.DEFAULT, RateLimitScope.CHAT_SSE);
    }

    public boolean tryAcquire(String userId, RateLimitTier tier) {
        return tryAcquire(userId, tier, RateLimitScope.CHAT_SSE);
    }

    public boolean tryAcquire(String userId, RateLimitTier tier, RateLimitScope scope) {
        if (tier == RateLimitTier.UNLIMITED) {
            return true;
        }
        int limit = tier == RateLimitTier.HIGH ? highLimit : defaultLimit;
        RateLimitScope bucketScope = scope != null ? scope : RateLimitScope.CHAT_SSE;
        String bucketKey = userId + ":" + tier.name() + ":" + bucketScope.name();
        TokenBucket bucket = buckets.computeIfAbsent(
                bucketKey,
                k -> new TokenBucket(limit, DEFAULT_WINDOW_SECONDS));
        if (bucket.tryConsume()) {
            return true;
        }
        chatTurnMetrics.recordRateLimited(tier, bucketScope);
        return false;
    }

    public int windowSeconds() {
        return DEFAULT_WINDOW_SECONDS;
    }
}
