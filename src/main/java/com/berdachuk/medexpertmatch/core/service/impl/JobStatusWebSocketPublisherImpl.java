package com.berdachuk.medexpertmatch.core.service.impl;

import com.berdachuk.medexpertmatch.core.service.JobStatusWebSocketPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobStatusWebSocketPublisherImpl implements JobStatusWebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public JobStatusWebSocketPublisherImpl(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void publish(String jobId, Object status) {
        String destination = "/topic/jobs/" + jobId;
        log.debug("Publishing job status to {}", destination);
        messagingTemplate.convertAndSend(destination, status);
    }
}
