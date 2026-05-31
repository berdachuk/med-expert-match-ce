package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.llm.domain.A2aSkillDescriptor;
import com.berdachuk.medexpertmatch.llm.service.A2aSkillRegistryService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class A2aSkillRegistryServiceImpl implements A2aSkillRegistryService {

    private static final List<A2aSkillDescriptor> SKILLS = List.of(
            new A2aSkillDescriptor(
                    "doctor_match",
                    "Match anonymized clinical cases to specialist doctors via GraphRAG.",
                    inputSchema("Anonymized case narrative or referral question (no PHI).")),
            new A2aSkillDescriptor(
                    "evidence_search",
                    "Retrieve clinical guidelines and PubMed literature for anonymized queries.",
                    inputSchema("Anonymized clinical question or topic (no PHI).")));

    @Override
    public List<A2aSkillDescriptor> listSkills() {
        return SKILLS;
    }

    private static Map<String, Object> inputSchema(String messageDescription) {
        Map<String, Object> messageField = new LinkedHashMap<>();
        messageField.put("type", "string");
        messageField.put("description", messageDescription);
        messageField.put("required", true);

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("message", messageField);
        properties.put("skill", Map.of(
                "type", "string",
                "description", "Skill id (optional when using dedicated endpoint)",
                "enum", List.of("doctor_match", "evidence_search")));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of("message"));
        return schema;
    }
}
