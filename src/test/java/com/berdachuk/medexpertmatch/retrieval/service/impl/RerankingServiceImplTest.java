package com.berdachuk.medexpertmatch.retrieval.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RerankingServiceImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("parseJsonArray parses valid JSON array of indices")
    void parseJsonArrayValid() throws Exception {
        String json = "[3, 0, 5, 1, 2, 4]";
        List<Integer> result = objectMapper.readValue(json, new TypeReference<List<Integer>>() {});
        assertEquals(List.of(3, 0, 5, 1, 2, 4), result);
    }

    @Test
    @DisplayName("parseJsonArray handles empty array")
    void parseJsonArrayEmpty() throws Exception {
        List<Integer> result = objectMapper.readValue("[]", new TypeReference<List<Integer>>() {});
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("parseJsonArray handles single element")
    void parseJsonArraySingle() throws Exception {
        List<Integer> result = objectMapper.readValue("[0]", new TypeReference<List<Integer>>() {});
        assertEquals(List.of(0), result);
    }
}
