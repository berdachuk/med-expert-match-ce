package com.berdachuk.medexpertmatch.llm.evaluation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Slf4j
public final class EvalDatasetLoader {

    private EvalDatasetLoader() {}

    @SuppressWarnings("unchecked")
    public static EvalDataset loadFromClasspath(String resourcePath) {
        String effectivePath = resourcePath.startsWith("classpath:")
                ? resourcePath.substring("classpath:".length()) : resourcePath;
        try {
            Yaml yaml = new Yaml();
            InputStream inputStream = new ClassPathResource(effectivePath).getInputStream();
            Map<String, Object> root = yaml.load(inputStream);

            String datasetId = (String) root.get("datasetId");
            String version = (String) root.get("version");
            List<Map<String, Object>> rawCases = (List<Map<String, Object>>) root.get("cases");

            List<EvalCase> cases = rawCases.stream()
                    .map(EvalDatasetLoader::mapToEvalCase)
                    .toList();

            log.info("Loaded eval dataset: {} (v{}), {} cases", datasetId, version, cases.size());
            return new EvalDataset(datasetId, version, cases);
        } catch (Exception e) {
            log.error("Failed to load eval dataset: {}", resourcePath, e);
            throw new RuntimeException("Failed to load eval dataset: " + resourcePath, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static EvalCase mapToEvalCase(Map<String, Object> raw) {
        return new EvalCase(
                (String) raw.get("id"),
                (String) raw.get("type"),
                (String) raw.get("case_id"),
                (String) raw.get("expected_specialty"),
                (List<String>) raw.get("required_fields"),
                raw.get("min_matches") instanceof Number n ? n.intValue() : null
        );
    }
}
