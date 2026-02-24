package com.berdachuk.medexpertmatch.llm.rest;

import com.berdachuk.medexpertmatch.llm.domain.AnalyzeJobStatus;
import com.berdachuk.medexpertmatch.llm.domain.MatchJobStatus;
import com.berdachuk.medexpertmatch.llm.domain.PrioritizeJobStatus;
import com.berdachuk.medexpertmatch.llm.domain.RouteJobStatus;
import com.berdachuk.medexpertmatch.llm.service.AnalyzeJobStore;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.llm.service.MatchJobStore;
import com.berdachuk.medexpertmatch.llm.service.PrioritizeJobStore;
import com.berdachuk.medexpertmatch.llm.service.RouteJobStore;
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
    private final RouteJobStore routeJobStore;
    private final MatchJobStore matchJobStore;
    private final AnalyzeJobStore analyzeJobStore;

    public MedicalAgentController(MedicalAgentService medicalAgentService, PrioritizeJobStore prioritizeJobStore,
                                  RouteJobStore routeJobStore, MatchJobStore matchJobStore,
                                  AnalyzeJobStore analyzeJobStore) {
        this.medicalAgentService = medicalAgentService;
        this.prioritizeJobStore = prioritizeJobStore;
        this.routeJobStore = routeJobStore;
        this.matchJobStore = matchJobStore;
        this.analyzeJobStore = analyzeJobStore;
    }

    /**
     * Starts async match doctors. Returns 202 with job ID; client polls status endpoint.
     * Avoids gateway timeout for long-running operations.
     *
     * @param caseId  The medical case ID
     * @param request Optional request parameters (sessionId, etc.)
     * @return 202 Accepted with job ID
     */
    @PostMapping("/match/{caseId}")
    public ResponseEntity<Map<String, String>> matchDoctors(
            @PathVariable String caseId,
            @RequestBody(required = false) Map<String, Object> request
    ) {
        log.info("POST /api/v1/agent/match/{} (async)", caseId);
        String jobId = matchJobStore.createJob();
        Map<String, Object> params = request != null ? request : Map.of();
        CompletableFuture.runAsync(() -> {
            try {
                MedicalAgentService.AgentResponse response = medicalAgentService.matchDoctors(caseId, params);
                matchJobStore.completeJob(jobId, response);
            } catch (Exception e) {
                log.error("Match doctors failed for job {}", jobId, e);
                matchJobStore.failJob(jobId, e.getMessage());
            }
        });
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("jobId", jobId));
    }

    /**
     * Sync match doctors (legacy). May timeout for long operations; prefer async POST + poll.
     */
    @PostMapping("/match-sync/{caseId}")
    public ResponseEntity<MedicalAgentService.AgentResponse> matchDoctorsSync(
            @PathVariable String caseId,
            @RequestBody(required = false) Map<String, Object> request
    ) {
        log.info("POST /api/v1/agent/match-sync/{}", caseId);
        MedicalAgentService.AgentResponse response = medicalAgentService.matchDoctors(
                caseId,
                request != null ? request : Map.of()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Polls status of async match job.
     *
     * @param jobId Job ID from POST /match/{caseId} or POST /match-from-text
     * @return Job status (PENDING, COMPLETED, FAILED) and result when done
     */
    @GetMapping("/match/status/{jobId}")
    public ResponseEntity<MatchJobStatus> getMatchStatus(@PathVariable String jobId) {
        MatchJobStatus status = matchJobStore.getStatus(jobId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }

    /**
     * Starts async match-from-text. Returns 202 with job ID; client polls status endpoint.
     * Avoids gateway timeout for long-running operations.
     *
     * @param request Request body containing caseText (required) and optional parameters
     * @return 202 Accepted with job ID
     */
    @Operation(
            summary = "Match doctors from text input (async)",
            description = "Starts match from text. Returns 202 with jobId; poll GET /match/status/{jobId} for result.",
            operationId = "matchFromText"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Accepted - poll status endpoint for result"),
            @ApiResponse(responseCode = "400", description = "Bad request - missing or invalid caseText"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/match-from-text")
    public ResponseEntity<Map<String, String>> matchFromText(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Request body containing caseText (required) and optional parameters: " +
                            "patientAge (Integer), caseType (String), symptoms (String), additionalNotes (String)",
                    required = true
            )
            @RequestBody Map<String, Object> request
    ) {
        log.info("POST /api/v1/agent/match-from-text (async)");

        Object caseTextObj = request.get("caseText");
        if (caseTextObj == null) {
            throw new IllegalArgumentException("caseText is required");
        }
        String caseText = caseTextObj.toString();
        if (caseText.isBlank()) {
            throw new IllegalArgumentException("caseText cannot be empty");
        }

        String jobId = matchJobStore.createJob();
        CompletableFuture.runAsync(() -> {
            try {
                MedicalAgentService.AgentResponse response = medicalAgentService.matchFromText(caseText, request);
                matchJobStore.completeJob(jobId, response);
            } catch (Exception e) {
                log.error("Match from text failed for job {}", jobId, e);
                matchJobStore.failJob(jobId, e.getMessage());
            }
        });
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("jobId", jobId));
    }

    /**
     * Sync match-from-text (legacy). May timeout for long operations; prefer async POST + poll.
     */
    @PostMapping("/match-from-text-sync")
    public ResponseEntity<MedicalAgentService.AgentResponse> matchFromTextSync(
            @RequestBody Map<String, Object> request
    ) {
        log.info("POST /api/v1/agent/match-from-text-sync");
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
     * Starts async case analysis. Returns 202 with job ID; client polls status endpoint.
     * Avoids gateway timeout for long-running operations.
     *
     * @param caseId  The medical case ID
     * @param request Optional request parameters (sessionId, etc.)
     * @return 202 Accepted with job ID
     */
    @PostMapping("/analyze-case/{caseId}")
    public ResponseEntity<Map<String, String>> analyzeCase(
            @PathVariable String caseId,
            @RequestBody(required = false) Map<String, Object> request
    ) {
        log.info("POST /api/v1/agent/analyze-case/{} (async)", caseId);
        String jobId = analyzeJobStore.createJob();
        Map<String, Object> params = request != null ? request : Map.of();
        CompletableFuture.runAsync(() -> {
            try {
                MedicalAgentService.AgentResponse response = medicalAgentService.analyzeCase(caseId, params);
                analyzeJobStore.completeJob(jobId, response);
            } catch (Exception e) {
                log.error("Case analysis failed for job {}", jobId, e);
                analyzeJobStore.failJob(jobId, e.getMessage());
            }
        });
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("jobId", jobId));
    }

    /**
     * Sync case analysis (legacy). May timeout for long operations; prefer async POST + poll.
     */
    @PostMapping("/analyze-case-sync/{caseId}")
    public ResponseEntity<MedicalAgentService.AgentResponse> analyzeCaseSync(
            @PathVariable String caseId,
            @RequestBody(required = false) Map<String, Object> request
    ) {
        log.info("POST /api/v1/agent/analyze-case-sync/{}", caseId);
        MedicalAgentService.AgentResponse response = medicalAgentService.analyzeCase(
                caseId,
                request != null ? request : Map.of()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Polls status of async case analysis job.
     *
     * @param jobId Job ID from POST /analyze-case/{caseId}
     * @return Job status (PENDING, COMPLETED, FAILED) and result when done
     */
    @GetMapping("/analyze-case/status/{jobId}")
    public ResponseEntity<AnalyzeJobStatus> getAnalyzeCaseStatus(@PathVariable String jobId) {
        AnalyzeJobStatus status = analyzeJobStore.getStatus(jobId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
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
     * Starts async route-case. Returns 202 with job ID; client polls status endpoint.
     * Avoids gateway timeout for long-running operations.
     *
     * @param caseId  The medical case ID
     * @param request Optional request parameters (sessionId, etc.)
     * @return 202 Accepted with job ID
     */
    @Operation(
            summary = "Route a case to facilities (async)",
            description = "Starts facility routing. Returns 202 with jobId; poll GET /route-case/status/{jobId} for result.",
            operationId = "routeCase"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Accepted - poll status endpoint for result"),
            @ApiResponse(responseCode = "400", description = "Bad request - invalid case ID"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/route-case/{caseId}")
    public ResponseEntity<Map<String, String>> routeCase(
            @Parameter(description = "The medical case ID", required = true, example = "696d4041ee50c1cfdb2e27ae")
            @PathVariable String caseId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Optional request parameters")
            @RequestBody(required = false) Map<String, Object> request
    ) {
        log.info("POST /api/v1/agent/route-case/{} (async)", caseId);
        String jobId = routeJobStore.createJob();
        Map<String, Object> params = request != null ? request : Map.of();
        CompletableFuture.runAsync(() -> {
            try {
                MedicalAgentService.AgentResponse response = medicalAgentService.routeCase(caseId, params);
                routeJobStore.completeJob(jobId, response);
            } catch (Exception e) {
                log.error("Route case failed for job {}", jobId, e);
                routeJobStore.failJob(jobId, e.getMessage());
            }
        });
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("jobId", jobId));
    }

    /**
     * Sync route-case (legacy). May timeout for long operations; prefer async POST + poll.
     */
    @PostMapping("/route-case-sync/{caseId}")
    public ResponseEntity<MedicalAgentService.AgentResponse> routeCaseSync(
            @PathVariable String caseId,
            @RequestBody(required = false) Map<String, Object> request
    ) {
        log.info("POST /api/v1/agent/route-case-sync/{}", caseId);
        MedicalAgentService.AgentResponse response = medicalAgentService.routeCase(
                caseId,
                request != null ? request : Map.of()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Polls status of async route-case job.
     *
     * @param jobId Job ID from POST /route-case/{caseId}
     * @return Job status (PENDING, COMPLETED, FAILED) and result when done
     */
    @GetMapping("/route-case/status/{jobId}")
    public ResponseEntity<RouteJobStatus> getRouteCaseStatus(@PathVariable String jobId) {
        RouteJobStatus status = routeJobStore.getStatus(jobId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }
}
