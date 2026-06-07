package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;

/**
 * Resume hook for doctor-match human checkpoints (M65). Enables eval stubs without full engine wiring.
 */
@FunctionalInterface
public interface DoctorMatchCheckpointResumer {

    MedicalAgentService.AgentResponse resumeAfterCheckpoint(String runId, DoctorMatchCheckpointPayload payload);
}
