package com.berdachuk.medexpertmatch.llm.eval;

import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import com.berdachuk.medexpertmatch.llm.config.MedicalConfidencePolicyProperties;
import com.berdachuk.medexpertmatch.llm.harness.ConfidencePolicyDecision;
import com.berdachuk.medexpertmatch.llm.harness.ConfidencePolicyInput;
import com.berdachuk.medexpertmatch.llm.harness.MedicalConfidencePolicyService;
import com.berdachuk.medexpertmatch.llm.harness.PolicyAction;
import com.berdachuk.medexpertmatch.llm.harness.impl.MedicalConfidencePolicyServiceImpl;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class PolicyConfidenceEvalRunner {

    private static final String DATASET = "/eval/policy-confidence-cases.jsonl";
    private static final long ESTIMATED_TOKENS = 6000;

    private PolicyConfidenceEvalRunner() {
    }

    public static EvalFamilyResult run() {
        ObjectMapper objectMapper = new ObjectMapper();
        MedicalConfidencePolicyService policyService =
                new MedicalConfidencePolicyServiceImpl(MedicalConfidencePolicyProperties.defaults());
        int passed = 0;
        int total = 0;
        try (InputStream stream = resourceStream(DATASET);
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
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
                if (expected == decision.action()) {
                    passed++;
                }
            }
            return new EvalFamilyResult("policy_confidence", "FULL", passed, total, ESTIMATED_TOKENS, true);
        } catch (Exception e) {
            throw new IllegalStateException("Policy confidence eval failed", e);
        }
    }

    private static InputStream resourceStream(String path) {
        return Objects.requireNonNull(PolicyConfidenceEvalRunner.class.getResourceAsStream(path), path);
    }
}
