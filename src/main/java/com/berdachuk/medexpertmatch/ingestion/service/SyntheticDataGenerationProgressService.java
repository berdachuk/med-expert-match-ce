package com.berdachuk.medexpertmatch.ingestion.service;

import com.berdachuk.medexpertmatch.ingestion.syntheticdata.domain.RunSummary;
import com.berdachuk.medexpertmatch.ingestion.syntheticdata.domain.SyntheticDataGenerationRun;
import com.berdachuk.medexpertmatch.ingestion.syntheticdata.repository.SyntheticDataGenerationRunRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SyntheticDataGenerationProgressService {

    private final Map<String, SyntheticDataGenerationProgress> progressMap = new ConcurrentHashMap<>();
    private final SyntheticDataGenerationRunRepository runRepository;

    public SyntheticDataGenerationProgressService(SyntheticDataGenerationRunRepository runRepository) {
        this.runRepository = runRepository;
    }

    public SyntheticDataGenerationProgress createProgress(String jobId) {
        SyntheticDataGenerationProgress progress = new SyntheticDataGenerationProgress(jobId);
        progressMap.put(jobId, progress);
        log.debug("Created progress tracker for job: {}", jobId);
        return progress;
    }

    public SyntheticDataGenerationProgress getProgress(String jobId) {
        return progressMap.get(jobId);
    }

    public void removeProgress(String jobId) {
        progressMap.remove(jobId);
        log.debug("Removed progress tracker for job: {}", jobId);
    }

    public boolean cancelJob(String jobId) {
        SyntheticDataGenerationProgress progress = progressMap.get(jobId);
        if (progress != null && "running".equals(progress.getStatus())) {
            progress.cancel();
            log.info("Cancelled job: {}", jobId);
            return true;
        }
        return false;
    }

    public void cleanupOldJobs() {
        progressMap.entrySet().removeIf(entry -> {
            SyntheticDataGenerationProgress progress = entry.getValue();
            if (progress.getEndTime() != null) {
                return progress.getEndTime().isBefore(
                        java.time.LocalDateTime.now().minusHours(1));
            }
            return false;
        });
    }

    public Map<String, List<RunSummary>> getRecentRunsBySize() {
        List<SyntheticDataGenerationRun> allRuns = runRepository.findAll();
        Map<String, List<RunSummary>> result = new LinkedHashMap<>();
        for (SyntheticDataGenerationRun run : allRuns) {
            if (run.totalDurationMs() != null) {
                result.computeIfAbsent(run.size(), k -> new ArrayList<>())
                        .add(new RunSummary(run.size(), run.startTime(), run.totalDurationMs()));
            }
        }
        for (List<RunSummary> summaries : result.values()) {
            summaries.sort((a, b) -> b.startTime().compareTo(a.startTime()));
            if (summaries.size() > 5) {
                result.put(summaries.getFirst().size(),
                        summaries.subList(0, 5));
            }
        }
        return result;
    }
}
