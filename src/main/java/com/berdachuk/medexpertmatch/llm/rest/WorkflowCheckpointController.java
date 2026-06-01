package com.berdachuk.medexpertmatch.llm.rest;

import com.berdachuk.medexpertmatch.core.security.CheckpointAccessGuard;
import com.berdachuk.medexpertmatch.llm.harness.HarnessWorkflowCheckpointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Harness Workflows", description = "Human checkpoint APIs (requires X-User-Id: admin or clinician)")
@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowCheckpointController {

    private final CheckpointAccessGuard checkpointAccessGuard;
    private final HarnessWorkflowCheckpointService checkpointService;

    public WorkflowCheckpointController(
            CheckpointAccessGuard checkpointAccessGuard,
            HarnessWorkflowCheckpointService checkpointService) {
        this.checkpointAccessGuard = checkpointAccessGuard;
        this.checkpointService = checkpointService;
    }

    @Operation(summary = "Approve or reject a paused harness workflow run")
    @PostMapping("/{runId}/checkpoint")
    public Map<String, Object> checkpoint(
            @PathVariable String runId,
            @RequestBody CheckpointRequestBody body) {
        checkpointAccessGuard.requireCheckpointReviewer();
        return checkpointService.checkpoint(
                runId,
                new HarnessWorkflowCheckpointService.CheckpointDecision(body.decision(), body.resumeToken()));
    }

    public record CheckpointRequestBody(
            HarnessWorkflowCheckpointService.CheckpointAction decision,
            String resumeToken) {
    }
}
