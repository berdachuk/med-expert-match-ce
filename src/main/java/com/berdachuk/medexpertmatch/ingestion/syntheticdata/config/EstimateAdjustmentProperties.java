package com.berdachuk.medexpertmatch.ingestion.syntheticdata.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "medexpertmatch.synthetic-data.estimate-adjustment")
public class EstimateAdjustmentProperties {

    private boolean enabled = false;
    private double safetyMarginMultiplier = 1.5;
    private int maxMinutes = 60;
    private String cron = "0 0 3 * * *";
}