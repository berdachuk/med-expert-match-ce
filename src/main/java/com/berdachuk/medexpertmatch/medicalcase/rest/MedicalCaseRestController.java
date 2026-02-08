package com.berdachuk.medexpertmatch.medicalcase.rest;

import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST controller for medical case API endpoints.
 * Provides search and query functionality for medical cases.
 */
@Slf4j
@RestController
@RequestMapping("/api/cases")
public class MedicalCaseRestController {

    private final MedicalCaseRepository medicalCaseRepository;

    public MedicalCaseRestController(MedicalCaseRepository medicalCaseRepository) {
        this.medicalCaseRepository = medicalCaseRepository;
    }

    /**
     * Gets a medical case by its ID.
     *
     * @param caseId The medical case ID
     * @return Medical case as JSON
     */
    @GetMapping("/{caseId}")
    public ResponseEntity<MedicalCase> getCaseById(@PathVariable String caseId) {
        log.info("GET /api/cases/{}", caseId);

        return medicalCaseRepository.findById(caseId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medical case not found: " + caseId));
    }

    /**
     * Searches medical cases by text query and optional filters.
     * REST endpoint for case search functionality.
     *
     * @param query        Text query to search in chiefComplaint, symptoms, additionalNotes (optional)
     * @param specialty    Filter by required specialty (optional)
     * @param urgencyLevel Filter by urgency level (optional)
     * @param caseId       Filter by case ID (optional)
     * @param offset       Number of records to skip (default: 0)
     * @param maxResults   Maximum number of results to return (default: 20)
     * @return List of matching medical cases as JSON
     */
    @GetMapping("/search")
    public ResponseEntity<List<MedicalCase>> searchCases(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String urgencyLevel,
            @RequestParam(required = false) String caseId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int maxResults
    ) {
        log.info(
                "GET /api/cases/search - query: {}, specialty: {}, urgencyLevel: {}, caseId: {}, offset: {}, maxResults: {}",
                query, specialty, urgencyLevel, caseId, offset, maxResults);

        List<MedicalCase> cases = medicalCaseRepository.search(query, specialty, urgencyLevel, caseId, offset,
                maxResults);
        return ResponseEntity.ok(cases);
    }

    /**
     * Gets paginated list of medical cases.
     *
     * @param offset Number of records to skip (default: 0)
     * @param limit  Maximum number of records to return (default: 50)
     * @return List of medical cases as JSON
     */
    @GetMapping("/list")
    public ResponseEntity<List<MedicalCase>> listCases(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit
    ) {
        log.info("GET /api/cases/list - offset: {}, limit: {}", offset, limit);

        List<MedicalCase> cases = medicalCaseRepository.findAllPaginated(offset, limit);
        return ResponseEntity.ok(cases);
    }
}
