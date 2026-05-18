package com.berdachuk.medexpertmatch.ingestion.service.impl;

import com.berdachuk.medexpertmatch.clinicalexperience.repository.ClinicalExperienceRepository;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.doctor.repository.MedicalSpecialtyRepository;
import com.berdachuk.medexpertmatch.facility.repository.FacilityRepository;
import com.berdachuk.medexpertmatch.graph.service.MedicalGraphBuilderService;
import com.berdachuk.medexpertmatch.ingestion.service.CaseDescriptionUpdate;
import com.berdachuk.medexpertmatch.ingestion.service.EmbeddingGeneratorService;
import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataCatalogState;
import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataGenerationProgress;
import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataPostProcessingService;
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
        } catch (DataAccessException e) {
            log.error("Database error building graph: {}", e.getMessage(), e);
        } catch (RuntimeException e) {
            log.error("Unexpected error building graph: {}", e.getMessage(), e);
        }
    }
}
