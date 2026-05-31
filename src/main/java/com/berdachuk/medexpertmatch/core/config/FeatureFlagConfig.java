package com.berdachuk.medexpertmatch.core.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
@ConfigurationProperties(prefix = "medexpertmatch.features")
public class FeatureFlagConfig {

    private boolean documentIngestion = true;
    private boolean graphRag = true;
    private boolean agentSkills = true;
    private boolean evaluation = true;
    private boolean semanticReranking = true;

    @SuppressWarnings("unused")
    public void setDocumentIngestion(boolean documentIngestion) {
        this.documentIngestion = documentIngestion;
    }

    @SuppressWarnings("unused")
    public void setGraphRag(boolean graphRag) {
        this.graphRag = graphRag;
    }

    @SuppressWarnings("unused")
    public void setAgentSkills(boolean agentSkills) {
        this.agentSkills = agentSkills;
    }

    @SuppressWarnings("unused")
    public void setEvaluation(boolean evaluation) {
        this.evaluation = evaluation;
    }

    @SuppressWarnings("unused")
    public void setSemanticReranking(boolean semanticReranking) {
        this.semanticReranking = semanticReranking;
    }
}
