package com.berdachuk.medexpertmatch.ingestion.syntheticdata.service.impl;

import com.berdachuk.medexpertmatch.ingestion.syntheticdata.config.EstimateAdjustmentProperties;
import com.berdachuk.medexpertmatch.ingestion.syntheticdata.domain.SyntheticDataGenerationRun;
import com.berdachuk.medexpertmatch.ingestion.syntheticdata.repository.SyntheticDataGenerationRunRepository;
import com.berdachuk.medexpertmatch.ingestion.syntheticdata.service.EstimateAdjustmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Slf4j
@Service
public class EstimateAdjustmentServiceImpl implements EstimateAdjustmentService {

    private final SyntheticDataGenerationRunRepository runRepository;
    private final EstimateAdjustmentProperties properties;

    public EstimateAdjustmentServiceImpl(
            SyntheticDataGenerationRunRepository runRepository,
            EstimateAdjustmentProperties properties) {
        this.runRepository = runRepository;
        this.properties = properties;
    }

    @Override
    public Map<String, Integer> adjustEstimates() {
        List<String> lines = readDataSizesCsv();
        if (lines.isEmpty()) {
            return Map.of();
        }

        Map<String, Integer> adjustments = new LinkedHashMap<>();
        List<String> updatedLines = new ArrayList<>();
        String header = lines.getFirst();
        updatedLines.add(header);

        int sizeIndex = findColumnIndex(header, "size");
        int estimatedMinutesIndex = findColumnIndex(header, "estimated_time_minutes");

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                updatedLines.add(line);
                continue;
            }
            String[] columns = line.split(",", -1);
            if (sizeIndex < 0 || estimatedMinutesIndex < 0 || columns.length <= Math.max(sizeIndex, estimatedMinutesIndex)) {
                updatedLines.add(line);
                continue;
            }
            String size = columns[sizeIndex].trim();
            List<SyntheticDataGenerationRun> latestRuns = runRepository.findLatestBySize(size, 1);
            if (!latestRuns.isEmpty() && latestRuns.getFirst().totalDurationMs() != null && latestRuns.getFirst().totalDurationMs() > 0) {
                long actualMs = latestRuns.getFirst().totalDurationMs();
                int newMinutes = (int) Math.ceil((actualMs / 60000.0) * properties.getSafetyMarginMultiplier());
                newMinutes = Math.min(newMinutes, properties.getMaxMinutes());
                newMinutes = Math.max(newMinutes, 1);
                columns[estimatedMinutesIndex] = String.valueOf(newMinutes);
                adjustments.put(size, newMinutes);
                log.info("M77: adjusted estimate for size={} actualMs={} newMinutes={}", size, actualMs, newMinutes);
            }
            updatedLines.add(String.join(",", columns));
        }

        writeDataSizesCsv(updatedLines);
        return adjustments;
    }

    private List<String> readDataSizesCsv() {
        try {
            ClassPathResource resource = new ClassPathResource("data/data-sizes.csv");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
                return reader.lines().toList();
            }
        } catch (IOException e) {
            log.error("M77: failed to read data-sizes.csv: {}", e.getMessage());
            return List.of();
        }
    }

    private void writeDataSizesCsv(List<String> lines) {
        try {
            ClassPathResource resource = new ClassPathResource("data/data-sizes.csv");
            Path tempFile = Files.createTempFile("data-sizes", ".csv");
            Files.write(tempFile, lines);
            Path target = Path.of(resource.getURL().getPath().replace("!", "").replace("file:", ""));
            if (target.toFile().exists()) {
                Files.copy(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
            } else {
                try (InputStream is = Files.newInputStream(tempFile)) {
                    try (OutputStream os = new FileOutputStream("src/main/resources/data/data-sizes.csv")) {
                        is.transferTo(os);
                    }
                }
            }
            Files.deleteIfExists(tempFile);
        } catch (IOException e) {
            log.error("M77: failed to write data-sizes.csv: {}", e.getMessage());
        }
    }

    private int findColumnIndex(String header, String columnName) {
        String[] columns = header.split(",", -1);
        for (int i = 0; i < columns.length; i++) {
            if (columns[i].trim().equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        return -1;
    }
}