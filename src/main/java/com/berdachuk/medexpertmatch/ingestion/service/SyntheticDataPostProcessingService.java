package com.berdachuk.medexpertmatch.ingestion.service;

import java.util.List;

/**
 * Post-processing service for generated synthetic data.
 */
public interface SyntheticDataPostProcessingService {

    /**
     * Clears generated synthetic data and graph objects.
     */
    void clearTestData();

    /**
     * Generates descriptions for medical cases without descriptions.
     *
     * @param progress Optional progress tracker
     */
    void generateMedicalCaseDescriptions(SyntheticDataGenerationProgress progress);

    /**
     * Commits a batch of generated descriptions.
     *
     * @param batch Description updates to commit
     */
    void commitDescriptionBatch(List<CaseDescriptionUpdate> batch);

    /**
     * Generates missing embeddings.
     */
    void generateEmbeddings();

    /**
     * Generates missing embeddings with progress tracking.
     *
     * @param progress Optional progress tracker
     */
    void generateEmbeddings(SyntheticDataGenerationProgress progress);

    /**
     * Rebuilds the graph representation from relational data.
     */
    void buildGraph();

    /**
     * M77: starts a synthetic data generation run for timing tracking.
     *
     * @param size        data size label (e.g. "large")
     * @param doctorCount number of doctors in this run
     * @param caseCount   number of cases in this run
     */
    void startRunTracking(String size, int doctorCount, int caseCount);

    /**
     * M77: completes the current run tracking, writing final timing data.
     *
     * @param errorMessage error message if the run failed, null if successful
     */
    void completeRunTracking(String errorMessage);

    /**
     * M73: walks the SQL doctor table and ensures every
     * {@code (d:Doctor)-[:SPECIALIZES_IN]->(s:MedicalSpecialty)}
     * edge implied by {@code d.specialties} exists in the graph.
     * Idempotent; safe to call on a healthy graph.
     *
     * @return summary of what was processed; never {@code null}
     */
    ReconcileReport reconcileSpecialtyGraph();

    /**
     * Summary of a {@link #reconcileSpecialtyGraph()} run.
     *
     * @param processed         number of (doctor, specialty) pairs that were
     *                          passed to the graph builder (either newly
     *                          created or no-op due to MERGE idempotency)
     * @param doctorsProcessed  number of doctors scanned in this run
     * @param doctors           distinct doctor IDs that were touched
     * @param specialties       distinct specialty names that were touched
     */
    record ReconcileReport(int processed,
                           int doctorsProcessed,
                           List<String> doctors,
                           List<String> specialties) {
    }

    /**
     * M75: walks the SQL {@code medical_cases} table and ensures every
     * case with a non-blank {@code required_specialty} has a matching
     * {@code (c:MedicalCase)-[:REQUIRES_SPECIALTY]->(s:MedicalSpecialty)}
     * edge in the graph. Mirrors M73's {@link #reconcileSpecialtyGraph()}
     * for the case side; idempotent; safe to call on a healthy graph.
     * <p>
     * M73 only touched the doctor side, which left a stale-edge gap on
     * the case side: 5,439 SQL cases with a non-blank specialty but
     * only 600 graph edges (2026-06-09 audit). New cases created via
     * the chat intake harness or the synthetic generator have no
     * pre-existing graph edge, so this helper heals that gap.
     *
     * @return summary of what was processed; never {@code null}
     */
    ReconcileCaseReport reconcileCaseSpecialtyGraph();

    /**
     * Summary of a {@link #reconcileCaseSpecialtyGraph()} run.
     *
     * @param processed         number of (case, specialty) pairs that were
     *                          passed to the graph builder (either newly
     *                          created or no-op due to MERGE idempotency)
     * @param casesProcessed    number of medical cases scanned in this run
     * @param cases             distinct case IDs that were touched
     * @param specialties       distinct specialty names that were touched
     */
    record ReconcileCaseReport(int processed,
                               int casesProcessed,
                               List<String> cases,
                               List<String> specialties) {
    }
}
