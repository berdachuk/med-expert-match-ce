package com.berdachuk.medexpertmatch.core.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/features")
public class FeatureFlagController {

    private final FeatureFlagConfig config;

    public FeatureFlagController(FeatureFlagConfig config) {
        this.config = config;
    }

    @GetMapping
    public Map<String, Object> listFeatures() {
        Map<String, Object> features = new LinkedHashMap<>();
        features.put("documentIngestion", config.getDocumentIngestion());
        features.put("graphRag", config.getGraphRag());
        features.put("agentSkills", config.getAgentSkills());
        features.put("evaluation", config.getEvaluation());
        features.put("semanticReranking", config.getSemanticReranking());
        return features;
    }
}
