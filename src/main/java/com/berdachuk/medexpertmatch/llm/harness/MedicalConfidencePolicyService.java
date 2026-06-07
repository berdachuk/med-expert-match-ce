package com.berdachuk.medexpertmatch.llm.harness;

public interface MedicalConfidencePolicyService {

    ConfidencePolicyDecision decide(ConfidencePolicyInput input);
}
