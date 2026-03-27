package com.berdachuk.medexpertmatch.ingestion.service;

import com.berdachuk.medexpertmatch.ingestion.exception.SyntheticDataGenerationException;
import com.berdachuk.medexpertmatch.ingestion.service.CaseDescriptionUpdate;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r5.model.Bundle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Orchestrates synthetic data generation by delegating to scorched-down helper services.
 */
@Slf4j
@Service
public class SyntheticDataGenerator {

    private final SyntheticDataBootstrapService syntheticDataBootstrapService;
    private final SyntheticDataGenerationService syntheticDataGenerationService;
    private final SyntheticDataPostProcessingService syntheticDataPostProcessingService;
    private final SyntheticDataGenerationProgressService progressService;

    public SyntheticDataGenerator(
            SyntheticDataBootstrapService syntheticDataBootstrapService,
            SyntheticDataGenerationService syntheticDataGenerationService,
            SyntheticDataPostProcessingService syntheticDataPostProcessingService,
            SyntheticDataGenerationProgressService progressService) {
        this.syntheticDataBootstrapService = syntheticDataBootstrapService;
        this.syntheticDataGenerationService = syntheticDataGenerationService;
        this.syntheticDataPostProcessingService = syntheticDataPostProcessingService;
        this.progressService = progressService;
    }

    @PostConstruct
    public void loadDataFromFiles() {
        syntheticDataBootstrapService.loadDataFromFiles();
    }

    @Transactional
    public void persistMedicalSpecialties(List<Map<String, String>> specialtiesData) {
        syntheticDataBootstrapService.persistMedicalSpecialties(specialtiesData);
    }

    @Transactional
    public void persistIcd10Codes(List<Map<String, String>> icd10Data) {
        syntheticDataBootstrapService.persistIcd10Codes(icd10Data);
    }

    @Transactional
    public void persistProcedures(List<Map<String, String>> proceduresData) {
        syntheticDataBootstrapService.persistProcedures(proceduresData);
    }

    public void generateTestData(String size, boolean clear) {
        generateTestData(size, clear, null);
    }

    public void generateTestData(String size, boolean clear, String jobId) {
        log.info("Generating synthetic data - size: {}, clear: {}, jobId: {}", size, clear, jobId);

        SyntheticDataGenerationProgress progress = jobId != null ? progressService.getProgress(jobId) : null;
        if (progress != null) {
            progress.addTraceEntry("INFO", "Initializing", "Starting synthetic data generation...");
            progress.updateProgress(0, "Initializing", "Starting synthetic data generation...");
        }

        if (progress != null && progress.isCancelled()) {
            log.info("Generation cancelled before starting - jobId: {}", jobId);
            return;
        }

        if (clear) {
            if (progress != null) {
                progress.updateProgress(2, "Clearing", "Clearing existing synthetic data...");
            }
            clearTestData();
            if (progress != null && progress.isCancelled()) {
                log.info("Generation cancelled after clearing - jobId: {}", jobId);
                return;
            }
        }

        Map<String, SyntheticDataGenerationService.DataSizeConfig> configs = syntheticDataGenerationService.getAvailableSizes();
        String normalizedSize = size == null ? "" : size.toLowerCase(Locale.ROOT);
        SyntheticDataGenerationService.DataSizeConfig config = configs.get(normalizedSize);
        if (config == null) {
            log.warn("Unknown size: {}, using medium", size);
            config = configs.get("medium");
            if (config == null) {
                throw new IllegalArgumentException("No data size configuration found for size: " + size + " and default 'medium' is not available");
            }
        }

        int doctorCount = config.doctorCount();
        int caseCount = config.caseCount();

        if (progress != null) {
            progress.updateProgress(5, "ICD-10 Codes", "Generating ICD-10 codes...");
        }
        try {
            syntheticDataGenerationService.generateIcd10Codes(size, progress);
        } catch (SyntheticDataGenerationException e) {
            log.error("Failed to generate ICD-10 codes - jobId: {}", jobId, e);
            throw e;
        } catch (RuntimeException e) {
            if (progress != null && progress.isCancelled()) {
                log.info("Generation cancelled during ICD-10 codes - jobId: {}", jobId);
                return;
            }
            throw e;
        }
        if (progress != null && progress.isCancelled()) {
            log.info("Generation cancelled during ICD-10 codes - jobId: {}", jobId);
            return;
        }

        if (progress != null) {
            progress.updateProgress(10, "Specialties", "Generating medical specialties...");
        }
        try {
            syntheticDataGenerationService.generateMedicalSpecialties(size, progress);
        } catch (SyntheticDataGenerationException e) {
            log.error("Failed to generate medical specialties - jobId: {}", jobId, e);
            throw e;
        } catch (RuntimeException e) {
            if (progress != null && progress.isCancelled()) {
                log.info("Generation cancelled during specialties - jobId: {}", jobId);
                return;
            }
            throw e;
        }
        if (progress != null && progress.isCancelled()) {
            log.info("Generation cancelled during specialties - jobId: {}", jobId);
            return;
        }

        if (progress != null) {
            progress.updateProgress(12, "Procedures", "Extending procedures list...");
        }
        try {
            syntheticDataGenerationService.generateAdditionalProcedures(size, progress);
        } catch (SyntheticDataGenerationException e) {
            log.error("Failed to extend procedures - jobId: {}", jobId, e);
            throw e;
        } catch (RuntimeException e) {
            if (progress != null && progress.isCancelled()) {
                log.info("Generation cancelled during procedures - jobId: {}", jobId);
                return;
            }
            throw e;
        }
        if (progress != null && progress.isCancelled()) {
            log.info("Generation cancelled during procedures - jobId: {}", jobId);
            return;
        }

        int facilityCount = syntheticDataGenerationService.calculateFacilityCount(doctorCount);
        if (progress != null) {
            progress.updateProgress(15, "Facilities", String.format("Generating %d facilities...", facilityCount));
        }
        try {
            syntheticDataGenerationService.generateFacilities(facilityCount);
        } catch (SyntheticDataGenerationException e) {
            log.error("Failed to generate facilities - jobId: {}", jobId, e);
            throw e;
        }
        if (progress != null && progress.isCancelled()) {
            log.info("Generation cancelled during facilities - jobId: {}", jobId);
            return;
        }

        if (progress != null) {
            progress.updateProgress(20, "Doctors", String.format("Generating %d doctors...", doctorCount));
        }
        try {
            syntheticDataGenerationService.generateDoctors(doctorCount, progress);
        } catch (SyntheticDataGenerationException e) {
            log.error("Failed to generate doctors - jobId: {}", jobId, e);
            throw e;
        }
        if (progress != null && progress.isCancelled()) {
            log.info("Generation cancelled during doctors - jobId: {}", jobId);
            return;
        }

        if (progress != null) {
            progress.updateProgress(40, "Medical Cases", String.format("Generating %d medical cases...", caseCount));
        }
        try {
            syntheticDataGenerationService.generateMedicalCases(caseCount, progress);
        } catch (SyntheticDataGenerationException e) {
            log.error("Failed to generate medical cases - jobId: {}", jobId, e);
            throw e;
        }
        if (progress != null && progress.isCancelled()) {
            log.info("Generation cancelled during medical cases - jobId: {}", jobId);
            return;
        }

        if (progress != null) {
            progress.updateProgress(55, "Descriptions", "Generating medical case descriptions...");
        }
        try {
            generateMedicalCaseDescriptions(progress);
        } catch (Exception e) {
            log.error("Error during description generation - jobId: {} (continuing with partial results)", jobId, e);
        }
        if (progress != null && progress.isCancelled()) {
            log.info("Generation cancelled during descriptions - jobId: {}", jobId);
            return;
        }

        if (progress != null) {
            progress.updateProgress(70, "Embeddings", "Generating embeddings for medical cases...");
        }
        try {
            generateEmbeddings(progress);
        } catch (Exception e) {
            log.error("Error during embedding generation - jobId: {} (continuing with partial results)", jobId, e);
        }
        if (progress != null && progress.isCancelled()) {
            log.info("Generation cancelled during embeddings - jobId: {}", jobId);
            return;
        }

        if (progress != null) {
            progress.updateProgress(90, "Clinical Experiences", "Linking doctors to cases...");
        }
        try {
            syntheticDataGenerationService.generateClinicalExperiences(doctorCount, caseCount, progress);
        } catch (Exception e) {
            log.error("Error during clinical experience generation - jobId: {} (continuing with partial results)", jobId, e);
        }
        if (progress != null && progress.isCancelled()) {
            log.info("Generation cancelled during clinical experiences - jobId: {}", jobId);
            return;
        }

        if (progress != null) {
            progress.updateProgress(95, "Graph Building", "Building Apache AGE graph...");
        }
        try {
            buildGraph();
        } catch (Exception e) {
            log.error("Error during graph building - jobId: {} (continuing - graph building is optional)", jobId, e);
        }
        if (progress != null && progress.isCancelled()) {
            log.info("Generation cancelled after graph building - jobId: {}", jobId);
            return;
        }

        if (progress != null) {
            if (progress.isCancelled()) {
                log.info("Generation was cancelled - jobId: {}", jobId);
                return;
            }
            progress.updateProgress(100, "Complete", "Synthetic data generation complete");
            progress.complete();
        }

        log.info("Synthetic data generation complete - {} doctors, {} cases, {} facilities", doctorCount, caseCount, facilityCount);
    }

    public Map<String, SyntheticDataGenerationService.DataSizeConfig> getAvailableSizes() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(syntheticDataGenerationService.getAvailableSizes()));
    }

    @Transactional
    public void generateFacilities(int count) {
        syntheticDataGenerationService.generateFacilities(count);
    }

    @Transactional
    public void generateDoctors(int count) {
        syntheticDataGenerationService.generateDoctors(count, null);
    }

    @Transactional
    public void generateDoctors(int count, SyntheticDataGenerationProgress progress) {
        syntheticDataGenerationService.generateDoctors(count, progress);
    }

    @Transactional
    public void generateMedicalCases(int count) {
        syntheticDataGenerationService.generateMedicalCases(count, null);
    }

    @Transactional
    public void generateMedicalCases(int count, SyntheticDataGenerationProgress progress) {
        syntheticDataGenerationService.generateMedicalCases(count, progress);
    }

    public List<Bundle> generateFhirBundles(int count) {
        return syntheticDataGenerationService.generateFhirBundles(count);
    }

    @Transactional
    public void generateClinicalExperiences(int doctorCount, int caseCount) {
        syntheticDataGenerationService.generateClinicalExperiences(doctorCount, caseCount, null);
    }

    @Transactional
    public void generateClinicalExperiences(int doctorCount, int caseCount, SyntheticDataGenerationProgress progress) {
        syntheticDataGenerationService.generateClinicalExperiences(doctorCount, caseCount, progress);
    }

    @Transactional
    public void generateIcd10Codes(String size) {
        generateIcd10Codes(size, null);
    }

    @Transactional
    public void generateIcd10Codes(String size, SyntheticDataGenerationProgress progress) {
        syntheticDataGenerationService.generateIcd10Codes(size, progress);
    }

    @Transactional
    public void generateMedicalSpecialties(String size) {
        generateMedicalSpecialties(size, null);
    }

    @Transactional
    public void generateMedicalSpecialties(String size, SyntheticDataGenerationProgress progress) {
        syntheticDataGenerationService.generateMedicalSpecialties(size, progress);
    }

    @Transactional
    public void generateAdditionalProcedures(String size) {
        generateAdditionalProcedures(size, null);
    }

    @Transactional
    public void generateAdditionalProcedures(String size, SyntheticDataGenerationProgress progress) {
        syntheticDataGenerationService.generateAdditionalProcedures(size, progress);
    }

    @Transactional
    public void clearTestData() {
        syntheticDataPostProcessingService.clearTestData();
    }

    @Transactional
    public void commitDescriptionBatch(List<CaseDescriptionUpdate> batch) {
        syntheticDataPostProcessingService.commitDescriptionBatch(batch);
    }

    public void generateMedicalCaseDescriptions(SyntheticDataGenerationProgress progress) {
        syntheticDataPostProcessingService.generateMedicalCaseDescriptions(progress);
    }

    public void generateEmbeddings() {
        generateEmbeddings(null);
    }

    public void generateEmbeddings(SyntheticDataGenerationProgress progress) {
        syntheticDataPostProcessingService.generateEmbeddings(progress);
    }

    public void buildGraph() {
        syntheticDataPostProcessingService.buildGraph();
    }
}
