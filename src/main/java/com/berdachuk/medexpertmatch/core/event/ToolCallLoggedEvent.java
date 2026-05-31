package com.berdachuk.medexpertmatch.core.event;

/**
 * Published when a medical agent tool is invoked during an orchestrated turn.
 * Consumed by chat SSE activity bridge for live UI updates.
 */
public record ToolCallLoggedEvent(String sessionId, String toolName, String parameters) {}
