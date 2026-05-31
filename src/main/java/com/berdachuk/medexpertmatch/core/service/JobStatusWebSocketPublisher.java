package com.berdachuk.medexpertmatch.core.service;

/**
 * Publishes async job status updates to WebSocket subscribers.
 */
public interface JobStatusWebSocketPublisher {

    void publish(String jobId, Object status);
}
