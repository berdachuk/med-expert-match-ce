package com.berdachuk.medexpertmatch.core.config;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
@Configuration
@ConfigurationProperties(prefix = "medexpertmatch.features")
public class FeatureFlagConfig {

    @NotNull
    private Boolean documentIngestion = true;
    @NotNull
    private Boolean graphRag = true;
    @NotNull
    private Boolean agentSkills = true;
    @NotNull
    private Boolean evaluation = true;
    @NotNull
    private Boolean semanticReranking = true;

    @SuppressWarnings("unused")
    public void setDocumentIngestion(Boolean documentIngestion) {
        this.documentIngestion = documentIngestion;
    }

    @SuppressWarnings("unused")
    public void setGraphRag(Boolean graphRag) {
        this.graphRag = graphRag;
    }

    @SuppressWarnings("unused")
    public void setAgentSkills(Boolean agentSkills) {
        this.agentSkills = agentSkills;
    }

    @SuppressWarnings("unused")
    public void setEvaluation(Boolean evaluation) {
        this.evaluation = evaluation;
    }

    @SuppressWarnings("unused")
    public void setSemanticReranking(Boolean semanticReranking) {
        this.semanticReranking = semanticReranking;
    }
}
