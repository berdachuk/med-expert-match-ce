package com.berdachuk.medexpertmatch.chat.service;

/**
 * Rate-limit bucket scope (M25) — isolates chat SSE from A2A federation traffic.
 */
public enum RateLimitScope {
    CHAT_SSE,
    A2A
}
