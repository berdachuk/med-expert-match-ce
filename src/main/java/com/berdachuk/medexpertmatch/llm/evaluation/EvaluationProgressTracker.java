package com.berdachuk.medexpertmatch.llm.evaluation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class EvaluationProgressTracker {

    private static final int MAX_ENTRIES = 500;

    private final List<String> entries = Collections.synchronizedList(new ArrayList<>());
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicInteger completed = new AtomicInteger(0);
    private final AtomicInteger total = new AtomicInteger(0);
    private final Instant startedAt = Instant.now();

    public void setTotal(int total) {
        this.total.set(total);
    }

    public void logResult(String caseId, boolean passed, String details) {
        completed.incrementAndGet();
        String entry = String.format("[%s] %s: %s - %s",
                Instant.now(), caseId, passed ? "PASS" : "FAIL", details);
        if (entries.size() < MAX_ENTRIES) {
            entries.add(entry);
        }
    }

    public void logError(String caseId, String error) {
        completed.incrementAndGet();
        String entry = String.format("[%s] %s: ERROR - %s", Instant.now(), caseId, error);
        if (entries.size() < MAX_ENTRIES) {
            entries.add(entry);
        }
    }

    public void requestTermination() {
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }

    public int getCompleted() {
        return completed.get();
    }

    public int getTotal() {
        return total.get();
    }

    public List<String> getEntries() {
        return new ArrayList<>(entries);
    }

    public String getElapsed() {
        return java.time.Duration.between(startedAt, Instant.now()).toString();
    }
}
