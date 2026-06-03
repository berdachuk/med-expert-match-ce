package com.berdachuk.medexpertmatch.llm.event;

import java.time.Instant;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

@Component
public class EventDeadLetterQueue {

    private final Queue<DeadLetterEvent> deadLetters = new ConcurrentLinkedQueue<>();
    private final AtomicLong idSeq = new AtomicLong(0);

    public void enqueue(String sessionId, String agentName, String originalEventType, String error, String payload) {
        var event = new DeadLetterEvent(
                "dlq-" + idSeq.incrementAndGet(),
                sessionId, agentName, originalEventType, error, Instant.now(), payload);
        deadLetters.add(event);
    }

    public List<DeadLetterEvent> listAll() {
        return List.copyOf(deadLetters);
    }

    public DeadLetterEvent findById(String id) {
        return deadLetters.stream()
                .filter(e -> e.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    public boolean removeById(String id) {
        return deadLetters.removeIf(e -> e.id().equals(id));
    }

    public int size() {
        return deadLetters.size();
    }
}