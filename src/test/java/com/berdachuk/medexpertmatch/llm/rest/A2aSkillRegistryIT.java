package com.berdachuk.medexpertmatch.llm.rest;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class A2aSkillRegistryIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("GET /a2a/v1/skills lists doctor_match and evidence_search with input hints")
    void listsSupportedSkills() throws Exception {
        var result = mockMvc.perform(get("/a2a/v1/skills"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode skills = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(skills.isArray());
        assertTrue(skills.size() >= 2);

        boolean hasDoctorMatch = false;
        boolean hasEvidenceSearch = false;
        for (JsonNode skill : skills) {
            String id = skill.get("id").asText();
            if ("doctor_match".equals(id)) {
                hasDoctorMatch = skill.has("description") && skill.has("inputSchema");
            }
            if ("evidence_search".equals(id)) {
                hasEvidenceSearch = skill.has("description") && skill.has("inputSchema");
            }
        }
        assertTrue(hasDoctorMatch, "doctor_match skill descriptor required");
        assertTrue(hasEvidenceSearch, "evidence_search skill descriptor required");
    }
}
