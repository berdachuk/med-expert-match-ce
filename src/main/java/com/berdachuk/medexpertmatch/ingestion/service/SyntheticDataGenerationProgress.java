package com.berdachuk.medexpertmatch.ingestion.service;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tracks progress of synthetic data generation.
 */
@Data
public class SyntheticDataGenerationProgress {
    private String jobId;
    private String status; // "running", "completed", "error", "cancelled"
    private int progress; // 0-100
    private String currentStep;
    private String message;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String errorMessage;
    private volatile boolean cancelled = false;
    private List<TraceEntry> traceEntries = new ArrayList<>();

    public SyntheticDataGenerationProgress(String jobId) {
        this.jobId = jobId;
        this.status = "running";
        this.progress = 0;
        this.startTime = LocalDateTime.now();
        this.traceEntries = new ArrayList<>();
    }

    public void updateProgress(int progress, String currentStep, String message) {
        this.progress = Math.min(100, Math.max(0, progress));
        this.currentStep = currentStep;
        this.message = message;
        addTraceEntry("INFO", currentStep, message, progress);
    }

    public void addTraceEntry(String level, String step, String message, int progress) {
        traceEntries.add(new TraceEntry(LocalDateTime.now(), level, step, message, progress));
    }

    public void addTraceEntry(String level, String step, String message) {
        traceEntries.add(new TraceEntry(LocalDateTime.now(), level, step, message, this.progress));
    }

    public void complete() {
        this.status = "completed";
        this.progress = 100;
        this.endTime = LocalDateTime.now();
        addTraceEntry("SUCCESS", "Complete", "Synthetic data generation completed successfully", 100);
    }

    public void error(String errorMessage) {
        this.status = "error";
        this.errorMessage = errorMessage;
        this.endTime = LocalDateTime.now();
        addTraceEntry("ERROR", "Error", errorMessage, this.progress);
    }

    public void cancel() {
        this.cancelled = true;
        this.status = "cancelled";
        this.endTime = LocalDateTime.now();
        this.message = "Generation cancelled by user";
        addTraceEntry("WARN", "Cancelled", "Generation cancelled by user", this.progress);
    }

    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Represents a single trace entry in the execution log.
     */
    @Data
    public static class TraceEntry {
        private LocalDateTime timestamp;
        private String level; // INFO, SUCCESS, WARN, ERROR
        private String step;
        private String message;
        private int progress;

        public TraceEntry(LocalDateTime timestamp, String level, String step, String message, int progress) {
            this.timestamp = timestamp;
            this.level = level;
            this.step = step;
            this.message = message;
            this.progress = progress;
        }
    }
}
