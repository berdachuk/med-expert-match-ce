package com.berdachuk.medexpertmatch.llm.service;

import com.berdachuk.medexpertmatch.llm.domain.AnalyzeJobStatus;
import com.berdachuk.medexpertmatch.llm.domain.MatchJobStatus;
import com.berdachuk.medexpertmatch.llm.domain.PrioritizeJobStatus;
import com.berdachuk.medexpertmatch.llm.domain.RouteJobStatus;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JobStoreCleanupScheduler {

    private static final int COMPLETED_FAILED_TTL_MINUTES = 30;
    private static final int PENDING_TTL_MINUTES = 60;

    private final MatchJobStore matchJobStore;
    private final AnalyzeJobStore analyzeJobStore;
    private final PrioritizeJobStore prioritizeJobStore;
    private final RouteJobStore routeJobStore;

    public JobStoreCleanupScheduler(MatchJobStore matchJobStore, AnalyzeJobStore analyzeJobStore,
                                     PrioritizeJobStore prioritizeJobStore, RouteJobStore routeJobStore) {
        this.matchJobStore = matchJobStore;
        this.analyzeJobStore = analyzeJobStore;
        this.prioritizeJobStore = prioritizeJobStore;
        this.routeJobStore = routeJobStore;
    }

    @Scheduled(fixedRate = 300000)
    public void purgeStaleJobs() {
        int purged = 0;
        purged += purgeJobStore(matchJobStore, "match");
        purged += purgeJobStore(analyzeJobStore, "analyze");
        purged += purgeJobStore(prioritizeJobStore, "prioritize");
        purged += purgeJobStore(routeJobStore, "route");
        if (purged > 0) {
            log.info("Purged {} stale jobs across all stores", purged);
        }
    }

    private int purgeJobStore(Object store, String name) {
        if (store instanceof MatchJobStore s) return s.purgeExpired(COMPLETED_FAILED_TTL_MINUTES, PENDING_TTL_MINUTES);
        if (store instanceof AnalyzeJobStore s) return s.purgeExpired(COMPLETED_FAILED_TTL_MINUTES, PENDING_TTL_MINUTES);
        if (store instanceof PrioritizeJobStore s) return s.purgeExpired(COMPLETED_FAILED_TTL_MINUTES, PENDING_TTL_MINUTES);
        if (store instanceof RouteJobStore s) return s.purgeExpired(COMPLETED_FAILED_TTL_MINUTES, PENDING_TTL_MINUTES);
        return 0;
    }
}
