package com.berdachuk.medexpertmatch.ingestion.rest;

import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataGenerationProgress;
import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataGenerationProgressService;
import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for synthetic data generation endpoints.
 * Provides endpoints for generating synthetic data using FHIR R5 compliant resources.
 * <p>
 * API-First: This controller implements the OpenAPI specification defined in
 * src/main/resources/api/openapi.yaml
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/synthetic-data")
@Tag(name = "Synthetic Data", description = "Synthetic data generation endpoints")
public class SyntheticDataController {

    private final SyntheticDataGenerator syntheticDataGenerator;
    private final SyntheticDataGenerationProgressService progressService;

    public SyntheticDataController(SyntheticDataGenerator syntheticDataGenerator, SyntheticDataGenerationProgressService progressService) {
        this.syntheticDataGenerator = syntheticDataGenerator;
        this.progressService = progressService;
    }

    /**
     * Generates synthetic data based on size parameter.
     *
     * @param size  Data size: "tiny", "small", "medium", "large", "huge" (default: "medium")
     * @param clear Whether to clear existing data first (default: false)
     * @return Response with generation status
     */
    @Operation(
            summary = "Generate synthetic data",
            description = "Generates synthetic data based on size parameter. " +
                    "Uses FHIR R5 compliant resources for data generation.",
            operationId = "generateSyntheticData"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Synthetic data generated successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateSyntheticData(
            @Parameter(description = "Data size (tiny, small, medium, large, huge)", example = "medium")
            @RequestParam(defaultValue = "medium") String size,
            @Parameter(description = "Whether to clear existing data first", example = "false")
            @RequestParam(defaultValue = "false") boolean clear
    ) {
        log.info("POST /api/v1/synthetic-data/generate - size: {}, clear: {}", size, clear);

        // Generate unique job ID for progress tracking
        String jobId = UUID.randomUUID().toString();
        SyntheticDataGenerationProgress progress = progressService.createProgress(jobId);

        // Start async generation
        CompletableFuture.runAsync(() -> {
            try {
                syntheticDataGenerator.generateTestData(size, clear, jobId);
            } catch (Exception e) {
                log.error("Failed to generate synthetic data", e);
                progress.error(e.getMessage());
            }
        });

        return ResponseEntity.ok(Map.of(
                "status", "started",
                "message", "Synthetic data generation started",
                "jobId", jobId,
                "size", size,
                "cleared", clear
        ));
    }

    /**
     * Gets progress for a synthetic data generation job.
     *
     * @param jobId Job identifier
     * @return Progress information
     */
    @Operation(
            summary = "Get generation progress",
            description = "Gets the current progress of a synthetic data generation job",
            operationId = "getGenerationProgress"
    )
    @GetMapping("/progress/{jobId}")
    public ResponseEntity<Map<String, Object>> getProgress(
            @Parameter(description = "Job identifier")
            @PathVariable String jobId
    ) {
        SyntheticDataGenerationProgress progress = progressService.getProgress(jobId);

        if (progress == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("jobId", progress.getJobId());
        response.put("status", progress.getStatus());
        response.put("progress", progress.getProgress());
        response.put("currentStep", progress.getCurrentStep() != null ? progress.getCurrentStep() : "");
        response.put("message", progress.getMessage() != null ? progress.getMessage() : "");
        response.put("startTime", progress.getStartTime() != null ? progress.getStartTime().toString() : "");
        response.put("endTime", progress.getEndTime() != null ? progress.getEndTime().toString() : "");
        response.put("errorMessage", progress.getErrorMessage() != null ? progress.getErrorMessage() : "");
        response.put("traceEntries", progress.getTraceEntries() != null ? progress.getTraceEntries() : java.util.Collections.emptyList());

        return ResponseEntity.ok(response);
    }

    /**
     * Cancels a running synthetic data generation job.
     *
     * @param jobId Job identifier
     * @return Response with cancellation status
     */
    @Operation(
            summary = "Cancel generation job",
            description = "Cancels a running synthetic data generation job",
            operationId = "cancelGeneration"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Job cancelled successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(responseCode = "404", description = "Job not found"),
            @ApiResponse(responseCode = "400", description = "Job cannot be cancelled (already completed or cancelled)")
    })
    @PostMapping("/cancel/{jobId}")
    public ResponseEntity<Map<String, Object>> cancelGeneration(
            @Parameter(description = "Job identifier")
            @PathVariable String jobId
    ) {
        log.info("POST /api/v1/synthetic-data/cancel/{}", jobId);

        boolean cancelled = progressService.cancelJob(jobId);

        if (!cancelled) {
            SyntheticDataGenerationProgress progress = progressService.getProgress(jobId);
            if (progress == null) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Job cannot be cancelled. Current status: " + progress.getStatus()
                ));
            }
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Generation job cancelled successfully",
                "jobId", jobId
        ));
    }

    /**
     * Generates doctors only.
     *
     * @param count Number of doctors to generate (default: 100)
     * @return Response with generation status
     */
    @Operation(
            summary = "Generate doctors only",
            description = "Generates synthetic doctor data.",
            operationId = "generateDoctors"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Doctors generated successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/generate/doctors")
    public ResponseEntity<Map<String, Object>> generateDoctors(
            @Parameter(description = "Number of doctors to generate", example = "100")
            @RequestParam(defaultValue = "100") int count
    ) {
        log.info("POST /api/v1/synthetic-data/generate/doctors - count: {}", count);

        try {
            syntheticDataGenerator.generateDoctors(count);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Doctors generated successfully",
                    "count", count
            ));
        } catch (Exception e) {
            log.error("Failed to generate doctors", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to generate doctors: " + e.getMessage()
            ));
        }
    }

    /**
     * Generates medical cases only.
     *
     * @param count Number of cases to generate (default: 200)
     * @return Response with generation status
     */
    @Operation(
            summary = "Generate medical cases only",
            description = "Generates synthetic medical case data.",
            operationId = "generateMedicalCases"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Medical cases generated successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/generate/cases")
    public ResponseEntity<Map<String, Object>> generateMedicalCases(
            @Parameter(description = "Number of cases to generate", example = "200")
            @RequestParam(defaultValue = "200") int count
    ) {
        log.info("POST /api/v1/synthetic-data/generate/cases - count: {}", count);

        try {
            syntheticDataGenerator.generateMedicalCases(count);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Medical cases generated successfully",
                    "count", count
            ));
        } catch (Exception e) {
            log.error("Failed to generate medical cases", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to generate medical cases: " + e.getMessage()
            ));
        }
    }

    /**
     * Clears all synthetic data.
     *
     * @return Response with clear status
     */
    @Operation(
            summary = "Clear all synthetic data",
            description = "Clears all synthetic data from the database.",
            operationId = "clearSyntheticData"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Synthetic data cleared successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearSyntheticData() {
        log.info("POST /api/v1/synthetic-data/clear");

        try {
            syntheticDataGenerator.clearTestData();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Synthetic data cleared successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to clear synthetic data", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to clear synthetic data: " + e.getMessage()
            ));
        }
    }

    /**
     * Gets available data size configurations.
     *
     * @return List of available data size configurations
     */
    @Operation(
            summary = "Get available data sizes",
            description = "Returns list of available data size configurations for synthetic data generation",
            operationId = "getAvailableSizes"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Available sizes retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            )
    })
    @GetMapping("/sizes")
    public ResponseEntity<List<Map<String, Object>>> getAvailableSizes() {
        log.info("GET /api/v1/synthetic-data/sizes");

        Map<String, com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataGenerator.DataSizeConfig> sizes =
                syntheticDataGenerator.getAvailableSizes();

        List<Map<String, Object>> response = sizes.values().stream()
                .map(config -> Map.<String, Object>of(
                        "size", config.size(),
                        "doctorCount", config.doctorCount(),
                        "caseCount", config.caseCount(),
                        "description", config.description(),
                        "estimatedTimeMinutes", config.estimatedTimeMinutes()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }
}
