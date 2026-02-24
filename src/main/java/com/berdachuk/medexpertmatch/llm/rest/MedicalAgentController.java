package com.berdachuk.medexpertmatch.llm.rest;

import com.berdachuk.medexpertmatch.llm.domain.PrioritizeJobStatus;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.llm.service.PrioritizeJobStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for Medical Agent API endpoints.
 * Provides agent-based endpoints for medical case analysis, doctor matching, and routing.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent")
public class MedicalAgentController {

    private final MedicalAgentService medicalAgentService;
    private final PrioritizeJobStore prioritizeJobStore;

    public MedicalAgentController(MedicalAgentService medicalAgentService, PrioritizeJobStore prioritizeJobStore) {
        this.medicalAgentService = medicalAgentService;
        this.prioritizeJobStore = prioritizeJobStore;
    }

    /**
     * Matches doctors to a medical case.
     * Uses case-analyzer and doctor-matcher skills.
     * <p>
     * Use Cases: Use Case 1 (Specialist Matching), Use Case 2 (Second Opinion)
     * UI Page: /match
     *
     * @param caseId  The medical case ID
     * @param request Optional request parameters
     * @return Agent response with matched doctors
     */
    @PostMapping("/match/{caseId}")
    public ResponseEntity<MedicalAgentService.AgentResponse> matchDoctors(
            @PathVariable String caseId,
            @RequestBody(required = false) Map<String, Object> request
    ) {
        log.info("POST /api/v1/agent/match/{}", caseId);
        MedicalAgentService.AgentResponse response = medicalAgentService.matchDoctors(
                caseId,
                request != null ? request : Map.of()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Matches doctors from raw text input.
     * Creates a medical case, generates embeddings, and matches doctors in a single call.
     * Supports handwritten text (after OCR/transcription) and direct text input.
     * <p>
     * Use Cases: Use Case 1 (Specialist Matching), Use Case 2 (Second Opinion)
     * UI Page: /match
     *
     * @param request Request body containing caseText (required) and optional parameters
     * @return Agent response with matched doctors
     */
    @Operation(
            summary = "Match doctors from text input",
            description = "Accepts raw text input, creates a medical case, generates embeddings, and matches doctors in a single call. " +
                    "Supports handwritten text (after OCR/transcription) and direct text input.",
            operationId = "matchFromText"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful response with matched doctors",
                    content = @Content(schema = @Schema(implementation = MedicalAgentService.AgentResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Bad request - missing or invalid caseText"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/match-from-text")
    public ResponseEntity<MedicalAgentService.AgentResponse> matchFromText(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Request body containing caseText (required) and optional parameters: " +
                            "patientAge (Integer), caseType (String: INPATIENT, SECOND_OPINION, CONSULT_REQUEST), " +
                            "symptoms (String), additionalNotes (String)",
                    required = true
            )
            @RequestBody Map<String, Object> request
    ) {
        log.info("POST /api/v1/agent/match-from-text");

        // Extract caseText from request (required)
        Object caseTextObj = request.get("caseText");
        if (caseTextObj == null) {
            throw new IllegalArgumentException("caseText is required");
        }
        String caseText = caseTextObj.toString();
        if (caseText.isBlank()) {
            throw new IllegalArgumentException("caseText cannot be empty");
        }

        MedicalAgentService.AgentResponse response = medicalAgentService.matchFromText(caseText, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Starts async queue prioritization. Returns 202 with job ID; client polls status endpoint.
     * Avoids gateway timeout for long-running operations.
     *
     * @param request Request parameters (case IDs, sessionId, etc.)
     * @return 202 Accepted with job ID
     */
    @PostMapping("/prioritize-consults")
    public ResponseEntity<Map<String, String>> prioritizeConsults(
            @RequestBody(required = false) Map<String, Object> request
    ) {
        log.info("POST /api/v1/agent/prioritize-consults (async)");
        String jobId = prioritizeJobStore.createJob();
        Map<String, Object> params = request != null ? request : Map.of();
        CompletableFuture.runAsync(() -> {
            try {
                MedicalAgentService.AgentResponse response = medicalAgentService.prioritizeConsults(params);
                prioritizeJobStore.completeJob(jobId, response);
            } catch (Exception e) {
                log.error("Queue prioritization failed for job {}", jobId, e);
                prioritizeJobStore.failJob(jobId, e.getMessage());
            }
        });
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("jobId", jobId));
    }

    /**
     * Sync queue prioritization (legacy). May timeout for long operations; prefer async POST + poll.
     */
    @PostMapping("/prioritize-consults-sync")
    public ResponseEntity<MedicalAgentService.AgentResponse> prioritizeConsultsSync(
            @RequestBody(required = false) Map<String, Object> request
    ) {
        log.info("POST /api/v1/agent/prioritize-consults-sync");
        MedicalAgentService.AgentResponse response = medicalAgentService.prioritizeConsults(
                request != null ? request : Map.of()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Polls status of async queue prioritization job.
     *
     * @param jobId Job ID from POST /prioritize-consults
     * @return Job status (PENDING, COMPLETED, FAILED) and result when done
     */
    @GetMapping("/prioritize-consults/status/{jobId}")
    public ResponseEntity<PrioritizeJobStatus> getPrioritizeConsultsStatus(@PathVariable String jobId) {
        PrioritizeJobStatus status = prioritizeJobStore.getStatus(jobId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }

    /**
     * Performs network analytics.
     * Uses network-analyzer skill.
     * <p>
     * Use Case: Use Case 4 (Network Analytics)
     * UI Page: /analytics
     *
     * @param request Request parameters (condition codes, metrics, etc.)
     * @return Agent response with network analytics
     */
    @PostMapping("/network-analytics")
    public ResponseEntity<MedicalAgentService.AgentResponse> networkAnalytics(
            @RequestBody(required = false) Map<String, Object> request
    ) {
        log.info("POST /api/v1/agent/network-analytics");
        MedicalAgentService.AgentResponse response = medicalAgentService.networkAnalytics(
                request != null ? request : Map.of()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Analyzes a medical case.
     * Uses case-analyzer, evidence-retriever, and recommendation-engine skills.
     * <p>
     * Use Case: Use Case 5 (Decision Support)
     * UI Page: /analyze/{caseId}
     *
     * @param caseId  The medical case ID
     * @param request Optional request parameters
     * @return Agent response with case analysis and recommendations
     */
    @PostMapping("/analyze-case/{caseId}")
    public ResponseEntity<MedicalAgentService.AgentResponse> analyzeCase(
            @PathVariable String caseId,
            @RequestBody(required = false) Map<String, Object> request
    ) {
        log.info("POST /api/v1/agent/analyze-case/{}", caseId);
        MedicalAgentService.AgentResponse response = medicalAgentService.analyzeCase(
                caseId,
                request != null ? request : Map.of()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Generates expert recommendations.
     * Uses doctor-matcher skill.
     * <p>
     * Use Case: Use Case 5 (Decision Support)
     * UI Page: /analyze/{caseId}
     *
     * @param matchId The match ID
     * @param request Optional request parameters
     * @return Agent response with expert recommendations
     */
    @PostMapping("/recommendations/{matchId}")
    public ResponseEntity<MedicalAgentService.AgentResponse> generateRecommendations(
            @PathVariable String matchId,
            @RequestBody(required = false) Map<String, Object> request
    ) {
        log.info("POST /api/v1/agent/recommendations/{}", matchId);
        MedicalAgentService.AgentResponse response = medicalAgentService.generateRecommendations(
                matchId,
                request != null ? request : Map.of()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Routes a case to facilities.
     * Uses case-analyzer and routing-planner skills.
     * <p>
     * Use Case: Use Case 6 (Regional Routing)
     * UI Page: /routing
     *
     * @param caseId  The medical case ID
     * @param request Optional request parameters
     * @return Agent response with facility routing recommendations
     */
    @Operation(
            summary = "Route a case to facilities",
            description = "Routes a medical case to appropriate facilities based on case requirements. " +
                    "Uses case-analyzer and routing-planner skills. Use Case: Use Case 6 (Regional Routing)",
            operationId = "routeCase"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful response with facility routing recommendations",
                    content = @Content(schema = @Schema(implementation = MedicalAgentService.AgentResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Bad request - invalid case ID or request parameters"),
            @ApiResponse(responseCode = "404", description = "Medical case not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/route-case/{caseId}")
    public ResponseEntity<MedicalAgentService.AgentResponse> routeCase(
            @Parameter(description = "The medical case ID", required = true, example = "696d4041ee50c1cfdb2e27ae")
            @PathVariable String caseId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Optional request parameters")
            @RequestBody(required = false) Map<String, Object> request
    ) {
        log.info("POST /api/v1/agent/route-case/{}", caseId);
        MedicalAgentService.AgentResponse response = medicalAgentService.routeCase(
                caseId,
                request != null ? request : Map.of()
        );
        return ResponseEntity.ok(response);
    }
}
