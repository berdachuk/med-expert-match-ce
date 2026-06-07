package com.berdachuk.medexpertmatch.llm.config;

import com.berdachuk.medexpertmatch.core.config.LlmResponseCacheHitListener;
import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import com.berdachuk.medexpertmatch.core.util.LlmUsageContext;
import com.berdachuk.medexpertmatch.core.util.LlmUsageContextHolder;
import com.berdachuk.medexpertmatch.llm.advisor.LlmUsageCaptureAdvisor;
import com.berdachuk.medexpertmatch.llm.monitoring.LlmCallSnapshot;
import com.berdachuk.medexpertmatch.llm.monitoring.LlmUsageTelemetryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class LlmUsageConfiguration {

    @Bean
    LlmUsageCaptureAdvisor llmUsageCaptureAdvisor(LlmUsageTelemetryService telemetryService) {
        return new LlmUsageCaptureAdvisor(telemetryService);
    }

    @Bean
    LlmResponseCacheHitListener llmResponseCacheHitListener(LlmUsageTelemetryService telemetryService) {
        return cacheKey -> {
            LlmUsageContext ctx = LlmUsageContextHolder.get();
            if (ctx == null) {
                ctx = new LlmUsageContext(
                        "default",
                        LlmClientType.CLINICAL,
                        LlmCallSnapshot.operationFromCacheKey(cacheKey),
                        null,
                        null,
                        null);
            }
            telemetryService.record(LlmCallSnapshot.fromCacheHit(ctx, cacheKey));
        };
    }
}
