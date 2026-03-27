package com.berdachuk.medexpertmatch.embedding.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for multi-endpoint embedding generation.
 * Pool activates when {@code endpoints} has one or more valid URL entries.
 *
 * @see com.berdachuk.medexpertmatch.embedding.multiendpoint.EmbeddingEndpointPool
 */
@Component
@ConfigurationProperties(prefix = "medexpertmatch.embedding.multi-endpoint")
@Getter
@Setter
public class MultiEndpointEmbeddingProperties {

    private List<EndpointConfig> endpoints = new ArrayList<>();
    private int skipDurationMin = 10;
    private int workerPerEndpoint = 1;
    private int apiBatchSize = 50;

    @Getter
    @Setter
    public static class EndpointConfig {
        private String url;
        private String model;
        private int priority = 0;
        private Integer workers;
    }
}
