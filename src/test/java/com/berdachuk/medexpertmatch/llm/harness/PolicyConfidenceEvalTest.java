package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import com.berdachuk.medexpertmatch.llm.config.MedicalConfidencePolicyProperties;
import com.berdachuk.medexpertmatch.llm.harness.impl.MedicalConfidencePolicyServiceImpl;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyConfidenceEvalTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MedicalConfidencePolicyService policyService;

    @BeforeEach
    void setUp() {
        policyService = new MedicalConfidencePolicyServiceImpl(MedicalConfidencePolicyProperties.defaults());
    }

    @Test
    @DisplayName("Policy confidence eval JSONL regression set matches expected actions")
    void evalJsonlRegressionSet() throws Exception {
        int total = 0;
        int urgentVerifyFailEscalations = 0;
        int urgentVerifyFailTotal = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/eval/policy-confidence-cases.jsonl")),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                total++;
                JsonNode node = objectMapper.readTree(line);
                ConfidencePolicyInput input = new ConfidencePolicyInput(
                        node.get("matchCount").asInt(),
                        node.get("topMatchScore").asDouble(),
                        node.get("verificationPassed").asBoolean(),
                        UrgencyLevel.valueOf(node.get("urgency").asText()),
                        GoalType.valueOf(node.get("goalType").asText()),
                        node.path("insufficientGrounding").asBoolean(false));
                PolicyAction expected = PolicyAction.valueOf(node.get("expectedAction").asText());

                ConfidencePolicyDecision decision = policyService.decide(input);
                assertEquals(expected, decision.action(),
                        "Line " + total + " expected " + expected + " but got " + decision.action());

                if (!input.verificationPassed()
                        && (input.urgencyLevel() == UrgencyLevel.CRITICAL || input.urgencyLevel() == UrgencyLevel.HIGH)) {
                    urgentVerifyFailTotal++;
                    if (decision.action() == PolicyAction.ESCALATE) {
                        urgentVerifyFailEscalations++;
                    }
                }
            }
        }
        assertEquals(12, total, "Expected twelve policy-confidence eval scenarios");
        assertTrue(urgentVerifyFailTotal >= 3, "Need urgent verify-fail scenarios in eval set");
        double escalationRate = (double) urgentVerifyFailEscalations / urgentVerifyFailTotal;
        assertTrue(escalationRate >= 0.95,
                "URGENT verify-fail escalation rate " + escalationRate + " below 95% threshold");
    }
}
