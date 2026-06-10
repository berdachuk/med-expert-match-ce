package com.berdachuk.medexpertmatch.ingestion.service.impl;

import com.berdachuk.medexpertmatch.clinicalexperience.repository.ClinicalExperienceRepository;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.doctor.repository.MedicalSpecialtyRepository;
import com.berdachuk.medexpertmatch.facility.repository.FacilityRepository;
import com.berdachuk.medexpertmatch.graph.service.MedicalGraphBuilderService;
import com.berdachuk.medexpertmatch.ingestion.service.*;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.medicalcase.service.MedicalCaseDescriptionService;
import com.berdachuk.medexpertmatch.medicalcoding.repository.ICD10CodeRepository;
import com.berdachuk.medexpertmatch.medicalcoding.repository.ProcedureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles description generation, embeddings, graph rebuild, and cleanup.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyntheticDataPostProcessingServiceImpl implements SyntheticDataPostProcessingService {

    private static final String DESCRIPTIONS_PROGRESS_LABEL = "Descriptions";

    private final ClinicalExperienceRepository clinicalExperienceRepository;
    private final MedicalCaseRepository medicalCaseRepository;
    private final DoctorRepository doctorRepository;
    private final FacilityRepository facilityRepository;
    private final MedicalSpecialtyRepository medicalSpecialtyRepository;
    private final ICD10CodeRepository icd10CodeRepository;
    private final ProcedureRepository procedureRepository;
    private final MedicalGraphBuilderService graphBuilderService;
    private final MedicalCaseDescriptionService medicalCaseDescriptionService;
    private final EmbeddingGeneratorService embeddingGeneratorService;
    private final SyntheticDataCatalogState catalogState;

    @Value("${medexpertmatch.synthetic-data.description.batch-commit-size:10}")
    private int descriptionBatchCommitSize;

    @Override
    @Transactional
    public void clearTestData() {
        log.info("Clearing all test data");
        try {
            graphBuilderService.clearGraph();
            log.info("Graph objects cleared successfully");
        } catch (DataAccessException e) {
            log.error("Database error clearing graph objects: {}", e.getMessage(), e);
        } catch (RuntimeException e) {
            log.error("Unexpected error clearing graph objects: {}", e.getMessage(), e);
        }

        clinicalExperienceRepository.deleteAll();
        medicalCaseRepository.deleteAll();
        doctorRepository.deleteAll();
        facilityRepository.deleteAll();
        medicalSpecialtyRepository.deleteAll();
        icd10CodeRepository.deleteAll();
        procedureRepository.deleteAll();

        catalogState.getCachedIcd10Codes().clear();
        catalogState.getCachedSpecialties().clear();
        catalogState.getCachedProcedures().clear();
        // M73: clear the primary-specialty coverage so a stale
        // snapshot from a previous run cannot mislead operators.
        catalogState.getPrimarySpecialtyCoverage().clear();
        // M75: clear the case-side specialty coverage for the same
        // reason — a stale snapshot from a previous run would report
        // ghost cases that have been wiped by clearTestData.
        catalogState.getCaseSpecialtyCoverage().clear();
        log.debug("Cleared result caches");
        log.info("Synthetic data cleared");
    }

    @Override
    public void generateMedicalCaseDescriptions(SyntheticDataGenerationProgress progress) {
        List<com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase> cases = medicalCaseRepository.findWithoutDescriptions();
        int totalRecords = cases.size();
        log.info("Generating medical case descriptions: starting {} cases (batch commit size: {})",
                totalRecords, descriptionBatchCommitSize);
        if (totalRecords == 0) {
            log.info("Generating medical case descriptions: nothing to do (no cases without descriptions)");
            return;
        }

        int processedCount = 0;
        int successCount = 0;
        int failedCount = 0;
        long startTime = System.currentTimeMillis();
        List<CaseDescriptionUpdate> batch = new ArrayList<>();

        for (com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase medicalCase : cases) {
            if (progress != null && progress.isCancelled()) {
                log.info("Generating medical case descriptions: cancelled after {} of {} cases",
                        processedCount, totalRecords);
                break;
            }
            long caseStartMs = System.currentTimeMillis();
            String outcomeSummary;
            try {
                String description = medicalCaseDescriptionService.generateDescription(medicalCase);
                if (description != null && !description.isBlank()) {
                    batch.add(new CaseDescriptionUpdate(medicalCase.id(), description));
                    successCount++;
                    outcomeSummary = "stored abstract, length=" + description.length();
                } else {
                    log.warn("Generating medical case descriptions: empty result for caseId={}", medicalCase.id());
                    failedCount++;
                    outcomeSummary = "empty description";
                }
            } catch (Exception e) {
                log.error("Generating medical case descriptions: error for caseId={}", medicalCase.id(), e);
                failedCount++;
                outcomeSummary = "exception";
            }
            processedCount++;
            long caseElapsedMs = System.currentTimeMillis() - caseStartMs;
            log.info("Generating medical case descriptions: {}/{} finished (caseId={}, {} ms, {})",
                    processedCount, totalRecords, medicalCase.id(), caseElapsedMs, outcomeSummary);

            if (batch.size() >= descriptionBatchCommitSize) {
                commitDescriptionBatch(batch);
                batch.clear();
                log.info("Generating medical case descriptions: committed DB batch of {} (checkpoint {}/{})",
                        descriptionBatchCommitSize, processedCount, totalRecords);
            }

            if (progress != null) {
                int progressPercent = totalRecords > 0 ? (processedCount * 100 / totalRecords) : 0;
                int descriptionProgress = 55 + (progressPercent * 10 / 100);
                progress.updateProgress(descriptionProgress, DESCRIPTIONS_PROGRESS_LABEL,
                        String.format("Generating medical case descriptions: %d/%d (%d%%)",
                                processedCount, totalRecords, progressPercent));
            }
        }

        if (!batch.isEmpty()) {
            commitDescriptionBatch(batch);
            log.info("Generating medical case descriptions: committed final DB batch of {} rows",
                    batch.size());
        }

        long totalElapsedTime = System.currentTimeMillis() - startTime;
        double totalItemsPerSecond = totalElapsedTime > 0 ? (processedCount * 1000.0) / totalElapsedTime : 0;
        log.info("Generating medical case descriptions: phase complete. processed={}, success={}, failed={}, duration={}s, rate={} cases/s",
                processedCount, successCount, failedCount, totalElapsedTime / 1000.0,
                String.format("%.2f", totalItemsPerSecond));
    }

    @Override
    @Transactional
    public void commitDescriptionBatch(List<CaseDescriptionUpdate> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }
        int committedCount = 0;
        for (CaseDescriptionUpdate update : batch) {
            try {
                medicalCaseRepository.updateAbstract(update.caseId(), update.description());
                committedCount++;
            } catch (Exception e) {
                log.error("Error committing description for case: {}", update.caseId(), e);
            }
        }
        log.info("Committed {} description updates to database (batch size: {})", committedCount, batch.size());
    }

    @Override
    public void generateEmbeddings() {
        generateEmbeddings(null);
    }

    @Override
    public void generateEmbeddings(SyntheticDataGenerationProgress progress) {
        embeddingGeneratorService.generateEmbeddings(progress);
    }

    @Override
    public void buildGraph() {
        log.info("Building Apache AGE graph from generated data...");
        try {
            graphBuilderService.buildGraph();
            log.info("Graph building completed successfully");
            // M73: re-walk the doctor table and ensure every
            // (doctor, specialty) pair has a SPECIALIZES_IN edge in
            // the graph. The build above may have dropped edges if
            // a doctor was added after the build or if a specialty
            // was missing from the catalog at build time.
            ReconcileReport reconcile = reconcileSpecialtyGraph();
            log.info("Post-build reconcile: processed={} doctors={}",
                    reconcile.processed(), reconcile.doctorsProcessed());
            // M75: same idea for the case side — re-walk the
            // medical_cases table and ensure every case with a
            // required_specialty has a REQUIRES_SPECIALTY edge.
            ReconcileCaseReport reconcileCase = reconcileCaseSpecialtyGraph();
            log.info("Post-build case reconcile: processed={} cases={}",
                    reconcileCase.processed(), reconcileCase.casesProcessed());
        } catch (DataAccessException e) {
            log.error("Database error building graph: {}", e.getMessage(), e);
        } catch (RuntimeException e) {
            log.error("Unexpected error building graph: {}", e.getMessage(), e);
        }
    }

    /**
     * Walks the SQL doctor table and ensures every
     * {@code (d:Doctor)-[:SPECIALIZES_IN]->(s:MedicalSpecialty)}
     * edge implied by {@code d.specialties} exists in the graph. The
     * underlying {@code MedicalGraphBuilderService.createSpecializesInRelationship}
     * uses a {@code MERGE} Cypher so the operation is idempotent —
     * running it twice produces the same graph state with no errors.
     * <p>
     * M73 motivation: the pre-M73 {@code buildGraph()} path
     * occasionally missed edges when a doctor was added after the
     * initial graph build (e.g. synthetic data regenerated while the
     * app was up) or when a specialty was missing from the catalog.
     * The Kory Terry gap (Oncology in SQL but no graph edge) was the
     * triggering incident.
     *
     * @return summary of what was processed; never {@code null}.
     */
    @Override
    public ReconcileReport reconcileSpecialtyGraph() {
        List<String> doctorIds = doctorRepository.findAllIds(0);
        final int[] processed = {0};
        final java.util.Set<String> touchedDoctors = new java.util.LinkedHashSet<>();
        final java.util.Set<String> touchedSpecialties = new java.util.LinkedHashSet<>();
        // M73: track primary specialty coverage for the catalog state
        final java.util.Map<String, Integer> coverage = new java.util.LinkedHashMap<>();
        for (String doctorId : doctorIds) {
            doctorRepository.findById(doctorId).ifPresent(doctor -> {
                if (doctor.specialties() == null || doctor.specialties().isEmpty()) {
                    return;
                }
                for (String specialty : doctor.specialties()) {
                    if (specialty == null || specialty.isBlank()) {
                        continue;
                    }
                    try {
                        graphBuilderService.createSpecializesInRelationship(doctorId, specialty);
                        processed[0]++;
                        touchedDoctors.add(doctorId);
                        touchedSpecialties.add(specialty);
                        coverage.merge(specialty, 1, Integer::sum);
                    } catch (RuntimeException e) {
                        log.warn("Reconcile: could not create SPECIALIZES_IN edge for doctor={} specialty='{}': {}",
                                doctorId, specialty, e.getMessage());
                    }
                }
            });
        }
        // M73: publish the specialty coverage so the synthetic-data
        // state endpoint can report it.
        catalogState.setPrimarySpecialtyCoverage(coverage);
        log.info("Reconciled specialty graph: {} (doctor, specialty) pairs across {} doctors",
                processed[0], doctorIds.size());
        return new ReconcileReport(processed[0], doctorIds.size(),
                new java.util.ArrayList<>(touchedDoctors),
                new java.util.ArrayList<>(touchedSpecialties));
    }

    /**
     * Walks the SQL {@code medical_cases} table and ensures every case
     * with a non-blank {@code required_specialty} has a matching
     * {@code (c:MedicalCase)-[:REQUIRES_SPECIALTY]->(s:MedicalSpecialty)}
     * edge in the graph. The underlying
     * {@code MedicalGraphBuilderService.createRequiresSpecialtyRelationship}
     * uses a {@code MERGE} Cypher so the operation is idempotent —
     * running it twice produces the same graph state with no errors.
     * <p>
     * M75 motivation: M73's {@link #reconcileSpecialtyGraph()} only
     * covers the doctor side. The pre-M75 {@code buildGraph()} path
     * created the case-side edges during the initial build, but no
     * code path refreshed them afterwards — new cases created by the
     * chat intake harness or the synthetic generator had no
     * {@code REQUIRES_SPECIALTY} edge, leaving the 25% specialisation
     * component of the Find Specialist score at 0.00.
     *
     * @return summary of what was processed; never {@code null}.
     */
    @Override
    public ReconcileCaseReport reconcileCaseSpecialtyGraph() {
        List<String> caseIds = medicalCaseRepository.findAllIds(0);
        final int[] processed = {0};
        final java.util.Set<String> touchedCases = new java.util.LinkedHashSet<>();
        final java.util.Set<String> touchedSpecialties = new java.util.LinkedHashSet<>();
        // M75: track case-side specialty coverage for the catalog state
        final java.util.Map<String, Integer> coverage = new java.util.LinkedHashMap<>();
        for (String caseId : caseIds) {
            medicalCaseRepository.findById(caseId).ifPresent(medicalCase -> {
                String requiredSpecialty = medicalCase.requiredSpecialty();
                if (requiredSpecialty == null || requiredSpecialty.isBlank()) {
                    return;
                }
                try {
                    graphBuilderService.createRequiresSpecialtyRelationship(caseId, requiredSpecialty);
                    processed[0]++;
                    touchedCases.add(caseId);
                    touchedSpecialties.add(requiredSpecialty);
                    coverage.merge(requiredSpecialty, 1, Integer::sum);
                } catch (RuntimeException e) {
                    log.warn("Reconcile: could not create REQUIRES_SPECIALTY edge for case={} specialty='{}': {}",
                            caseId, requiredSpecialty, e.getMessage());
                }
            });
        }
        // M75: publish the case-side specialty coverage so the
        // synthetic-data state endpoint can report it.
        catalogState.setCaseSpecialtyCoverage(coverage);
        log.info("Reconciled case specialty graph: {} (case, specialty) pairs across {} cases",
                processed[0], caseIds.size());
        return new ReconcileCaseReport(processed[0], caseIds.size(),
                new java.util.ArrayList<>(touchedCases),
                new java.util.ArrayList<>(touchedSpecialties));
    }

    /**
     * Summary of a {@link #reconcileSpecialtyGraph()} run.
     *
     * @param processed         number of (doctor, specialty) pairs that were
     *                          passed to the graph builder (either newly
     *                          created or no-op due to MERGE idempotency)
     * @param doctorsProcessed  number of doctors scanned in this run
     */
    // The ReconcileReport record is declared on the service interface
    // (see SyntheticDataPostProcessingService.ReconcileReport).
}
