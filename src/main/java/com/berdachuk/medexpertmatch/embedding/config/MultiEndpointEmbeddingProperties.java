package com.berdachuk.medexpertmatch.embedding.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Component
@Validated
@ConfigurationProperties(prefix = "medexpertmatch.embedding.multi-endpoint")
@Getter
@Setter
public class MultiEndpointEmbeddingProperties {

    @Valid
    private List<EndpointConfig> endpoints = new ArrayList<>();
    @Min(1)
    private int skipDurationMin = 10;
    @Min(1)
    private int workerPerEndpoint = 1;
    @Min(1)
    private int apiBatchSize = 50;

    @Getter
    @Setter
    public static class EndpointConfig {
        @NotBlank
        private String url;
        private String model;
        private int priority = 0;
        private Integer workers;
    }
}
