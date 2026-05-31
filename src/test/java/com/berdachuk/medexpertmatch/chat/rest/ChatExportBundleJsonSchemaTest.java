package com.berdachuk.medexpertmatch.chat.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatExportBundleJsonSchemaTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Fixture export bundle validates against published JSON Schema")
    void fixtureValidatesAgainstSchema() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "userIdHash": "abc123",
                  "exportedAt": "2026-05-31T12:00:00Z",
                  "phiRedacted": true,
                  "chatCount": 1,
                  "messageCount": 1,
                  "auditReferenceHash": "def456",
                  "chats": [{
                    "chatId": "c1",
                    "name": "Test",
                    "agentId": "auto",
                    "isDefault": false,
                    "messages": [{
                      "id": "m1",
                      "role": "user",
                      "content": "Anonymized summary",
                      "sequenceNumber": 1,
                      "createdAt": "2026-05-31T12:00:01Z"
                    }]
                  }]
                }
                """);

        Set<ValidationMessage> errors = loadSchema().validate(payload);
        assertTrue(errors.isEmpty(), () -> "Schema validation errors: " + errors);
    }

    @Test
    @DisplayName("Schema rejects export bundle missing required fields")
    void rejectsMissingRequiredFields() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {"userIdHash":"abc","exportedAt":"2026-05-31T12:00:00Z"}
                """);

        Set<ValidationMessage> errors = loadSchema().validate(payload);
        assertTrue(errors.stream().anyMatch(m -> m.getMessage().contains("auditReferenceHash")
                || m.getMessage().contains("required")),
                () -> "Expected required-field errors but got: " + errors);
    }

    private JsonSchema loadSchema() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/api/schemas/chat-export-bundle.schema.json")) {
            JsonNode schemaNode = objectMapper.readTree(in);
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
            return factory.getSchema(schemaNode);
        }
    }
}
