package com.berdachuk.medexpertmatch.ingestion.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for loading CSV data files.
 * Supports CSV files with header row and comma-separated values.
 */
@Slf4j
public class CsvDataLoader {

    /**
     * Loads CSV data from resource file.
     *
     * @param resourceLoader ResourceLoader to load the file
     * @param filePath       Path to CSV file (e.g., "classpath:/data/file.csv")
     * @param dataType       Description of data type for error messages
     * @return List of maps (each map = row with column names as keys)
     * @throws IllegalStateException if file not found, not readable, or empty
     */
    public static List<Map<String, String>> loadCsv(ResourceLoader resourceLoader, String filePath, String dataType) {
        try {
            Resource resource = resourceLoader.getResource(filePath);
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalStateException(
                        String.format("CSV file not found or not readable: %s (required for %s)", filePath, dataType));
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

                List<String> lines = reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .collect(Collectors.toList());

                if (lines.isEmpty()) {
                    throw new IllegalStateException(
                            String.format("CSV file is empty or contains no valid data: %s (required for %s)", filePath, dataType));
                }

                // Parse header
                String headerLine = lines.get(0);
                String[] headers = parseCsvLine(headerLine);

                if (headers.length == 0) {
                    throw new IllegalStateException(
                            String.format("CSV file has no headers: %s (required for %s)", filePath, dataType));
                }

                // Parse data rows
                List<Map<String, String>> data = new ArrayList<>();
                for (int i = 1; i < lines.size(); i++) {
                    String[] values = parseCsvLine(lines.get(i));
                    if (values.length != headers.length) {
                        log.warn("Skipping row {} in {}: expected {} columns, found {}", i + 1, filePath, headers.length, values.length);
                        continue;
                    }

                    Map<String, String> row = new LinkedHashMap<>();
                    for (int j = 0; j < headers.length; j++) {
                        row.put(headers[j].trim(), values[j].trim());
                    }
                    data.add(row);
                }

                if (data.isEmpty()) {
                    throw new IllegalStateException(
                            String.format("CSV file contains no valid data rows: %s (required for %s)", filePath, dataType));
                }

                log.info("Loaded {} {} from {}", data.size(), dataType, filePath);
                return data;
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    String.format("Error loading CSV file: %s (required for %s). Error: %s", filePath, dataType, e.getMessage()), e);
        }
    }

    /**
     * Parses a CSV line, handling quoted values and escaped quotes.
     * Simple implementation - assumes no embedded newlines in quoted values.
     */
    private static String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Escaped quote
                    current.append('"');
                    i++; // Skip next quote
                } else {
                    // Toggle quote state
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // End of field
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        // Add last field
        values.add(current.toString());

        return values.toArray(new String[0]);
    }

    /**
     * Parses a comma-separated list from a CSV cell value.
     * Returns empty list if value is null or empty.
     */
    public static List<String> parseCommaSeparatedList(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Normalizes a string for matching (lowercase, remove extra spaces).
     */
    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase().trim().replaceAll("\\s+", " ");
    }
}
