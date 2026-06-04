package com.berdachuk.medexpertmatch.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Chat retention settings (M21). {@code idleDays = 0} disables purge.
 */
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "chat.retention")
public class ChatRetentionProperties {

    private int idleDays = 0;
    private int batchSize = 100;

    public int idleDays() {
        return idleDays;
    }

    public void setIdleDays(int idleDays) {
        this.idleDays = idleDays;
    }

    public int batchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public boolean enabled() {
        return idleDays > 0;
    }
}
