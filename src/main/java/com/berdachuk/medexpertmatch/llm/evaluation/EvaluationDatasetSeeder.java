package com.berdachuk.medexpertmatch.llm.evaluation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class EvaluationDatasetSeeder {

    private final EvaluationJdbcRepository repository;
    private final ObjectMapper objectMapper;

    public EvaluationDatasetSeeder(EvaluationJdbcRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    public void seedIfMissing(String resourcePath, String datasetName) {
        if (repository.findDatasetByName(datasetName) != null) {
            log.info("Dataset '{}' already exists, skipping seed", datasetName);
            return;
        }

        try {
            Resource resource = new ClassPathResource(resourcePath);
            if (!resource.exists()) {
                log.warn("Dataset resource not found: {}", resourcePath);
                return;
            }

            EvaluationDatasetEntity dataset = repository.insertDataset(datasetName, "1.0",
                    "Seeded from classpath:" + resourcePath);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int count = 0;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    Map<String, Object> entry = objectMapper.readValue(line,
                            new TypeReference<Map<String, Object>>() {});
                    String question = (String) entry.getOrDefault("question", "");
                    String answer = (String) entry.getOrDefault("answer", "");
                    String meta = entry.containsKey("meta") ? objectMapper.writeValueAsString(entry.get("meta")) : null;

                    if (!question.isBlank() && !answer.isBlank()) {
                        repository.insertCase(dataset.id(), question, answer, meta);
                        count++;
                    }
                }
                log.info("Seeded {} cases for dataset '{}'", count, datasetName);
            }
        } catch (Exception e) {
            log.error("Failed to seed dataset '{}': {}", datasetName, e.getMessage(), e);
        }
    }

    public List<EvaluationCaseEntity> loadCases(String datasetName) {
        EvaluationDatasetEntity dataset = repository.findDatasetByName(datasetName);
        if (dataset == null) {
            throw new IllegalArgumentException("Dataset not found: " + datasetName);
        }
        return repository.findCasesByDatasetId(dataset.id());
    }
}
