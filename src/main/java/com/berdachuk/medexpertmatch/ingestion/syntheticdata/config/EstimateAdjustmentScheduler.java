package com.berdachuk.medexpertmatch.ingestion.syntheticdata.config;

import com.berdachuk.medexpertmatch.ingestion.syntheticdata.service.EstimateAdjustmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "medexpertmatch.synthetic-data.estimate-adjustment.enabled", havingValue = "true")
@RequiredArgsConstructor
public class EstimateAdjustmentScheduler {

    private final EstimateAdjustmentService estimateAdjustmentService;
    private final EstimateAdjustmentProperties properties;

    @Scheduled(cron = "${medexpertmatch.synthetic-data.estimate-adjustment.cron:0 0 3 * * *}")
    public void scheduledAdjustEstimates() {
        log.info("M77: running scheduled estimate adjustment (cron={})", properties.getCron());
        var adjustments = estimateAdjustmentService.adjustEstimates();
        if (adjustments.isEmpty()) {
            log.info("M77: no estimates adjusted");
        } else {
            log.info("M77: adjusted estimates: {}", adjustments);
        }
    }
}