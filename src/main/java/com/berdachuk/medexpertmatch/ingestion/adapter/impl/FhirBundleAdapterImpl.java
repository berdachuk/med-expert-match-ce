package com.berdachuk.medexpertmatch.ingestion.adapter.impl;

import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.doctor.domain.MedicalSpecialty;
import com.berdachuk.medexpertmatch.doctor.repository.MedicalSpecialtyRepository;
import com.berdachuk.medexpertmatch.ingestion.adapter.*;
import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r5.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of FhirBundleAdapter.
 * Converts FHIR Bundle resources to MedicalCase entities.
 */
@Slf4j
@Component
public class FhirBundleAdapterImpl implements FhirBundleAdapter {

    private final FhirPatientAdapter patientAdapter;
    private final FhirConditionAdapter conditionAdapter;
    private final FhirEncounterAdapter encounterAdapter;
    private final FhirObservationAdapter observationAdapter;
    private final MedicalSpecialtyRepository medicalSpecialtyRepository;

    public FhirBundleAdapterImpl(
            FhirPatientAdapter patientAdapter,
            FhirConditionAdapter conditionAdapter,
            FhirEncounterAdapter encounterAdapter,
            FhirObservationAdapter observationAdapter,
            MedicalSpecialtyRepository medicalSpecialtyRepository) {
        this.patientAdapter = patientAdapter;
        this.conditionAdapter = conditionAdapter;
        this.encounterAdapter = encounterAdapter;
        this.observationAdapter = observationAdapter;
        this.medicalSpecialtyRepository = medicalSpecialtyRepository;
    }

    @Override
    public MedicalCase convertBundleToMedicalCase(Bundle bundle) {
        if (!isValidBundle(bundle)) {
            throw new IllegalArgumentException("Invalid FHIR Bundle structure");
        }

        // Extract resources from Bundle
        Patient patient = extractResource(bundle, Patient.class);
        List<Condition> conditions = extractResources(bundle, Condition.class);
        List<Observation> observations = extractResources(bundle, Observation.class);
        Encounter encounter = extractResource(bundle, Encounter.class);

        // Validate patient is anonymized
        if (patient != null && !patientAdapter.isAnonymized(patient)) {
            throw new IllegalArgumentException("Patient resource contains PHI - cannot process");
        }

        // Extract patient age
        Integer patientAge = patient != null ? patientAdapter.extractAge(patient) : null;

        // Extract ICD-10 codes and SNOMED codes from conditions
        List<String> icd10Codes = new ArrayList<>();
        List<String> snomedCodes = new ArrayList<>();
        List<String> diagnoses = new ArrayList<>();

        for (Condition condition : conditions) {
            icd10Codes.addAll(conditionAdapter.extractIcd10Codes(condition));
            snomedCodes.addAll(conditionAdapter.extractSnomedCodes(condition));
            String codeText = conditionAdapter.extractCodeText(condition);
            if (codeText != null) {
                diagnoses.add(codeText);
            }
        }

        // Extract symptoms from observations
        List<String> symptoms = new ArrayList<>();
        for (Observation observation : observations) {
            String codeText = observationAdapter.extractCodeText(observation);
            if (codeText != null) {
                symptoms.add(codeText);
            }
        }

        // Determine chief complaint (from first condition or observation)
        String chiefComplaint = null;
        if (!conditions.isEmpty()) {
            chiefComplaint = conditionAdapter.extractCodeText(conditions.get(0));
        } else if (!observations.isEmpty()) {
            chiefComplaint = observationAdapter.extractCodeText(observations.get(0));
        }

        // Determine current diagnosis (from first condition)
        String currentDiagnosis = null;
        if (!conditions.isEmpty()) {
            currentDiagnosis = conditionAdapter.extractCodeText(conditions.get(0));
        }

        // Determine urgency level (simplified - based on encounter class or condition severity)
        UrgencyLevel urgencyLevel = determineUrgencyLevel(conditions, encounter);

        // Determine required specialty (simplified - based on ICD-10 codes or condition type)
        String requiredSpecialty = determineRequiredSpecialty(icd10Codes, conditions);

        // Determine case type (based on encounter class)
        CaseType caseType = determineCaseType(encounter);

        // Combine symptoms text
        String symptomsText = symptoms.isEmpty() ? null : String.join(", ", symptoms);

        // Additional notes (combine condition notes, encounter notes, etc.)
        String additionalNotes = buildAdditionalNotes(conditions, encounter, observations);

        // Generate internal ID
        String caseId = IdGenerator.generateId();

        return new MedicalCase(
                caseId,
                patientAge,
                chiefComplaint,
                symptomsText,
                currentDiagnosis,
                icd10Codes.isEmpty() ? List.of() : icd10Codes,
                snomedCodes.isEmpty() ? List.of() : snomedCodes,
                urgencyLevel,
                requiredSpecialty,
                caseType,
                additionalNotes,
                null  // abstractText
        );
    }

    @Override
    public boolean isValidBundle(Bundle bundle) {
        if (bundle == null) {
            return false;
        }

        // Check that Bundle has at least one entry with a resource
        // Use getEntry() instead of hasEntry() as hasEntry() may not work correctly in all FHIR R5 implementations
        List<Bundle.BundleEntryComponent> entries = bundle.getEntry();
        if (entries == null || entries.isEmpty()) {
            return false;
        }

        // Check if any entry has a non-null resource
        // Don't rely on hasResource() as it may not work correctly in all FHIR R5 implementations
        return entries.stream()
                .anyMatch(entry -> entry != null && entry.getResource() != null);
    }

    /**
     * Extracts a single resource of the specified type from the Bundle.
     */
    private <T extends Resource> T extractResource(Bundle bundle, Class<T> resourceType) {
        return bundle.getEntry().stream()
                .filter(Bundle.BundleEntryComponent::hasResource)
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resourceType::isInstance)
                .map(resourceType::cast)
                .findFirst()
                .orElse(null);
    }

    /**
     * Extracts all resources of the specified type from the Bundle.
     */
    private <T extends Resource> List<T> extractResources(Bundle bundle, Class<T> resourceType) {
        return bundle.getEntry().stream()
                .filter(Bundle.BundleEntryComponent::hasResource)
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resourceType::isInstance)
                .map(resourceType::cast)
                .collect(Collectors.toList());
    }

    /**
     * Determines urgency level from conditions and encounter.
     */
    private UrgencyLevel determineUrgencyLevel(List<Condition> conditions, Encounter encounter) {
        // Check condition severity first
        for (Condition condition : conditions) {
            String severity = conditionAdapter.extractSeverity(condition);
            if (severity != null) {
                String severityLower = severity.toLowerCase();
                if (severityLower.contains("critical") || severityLower.contains("severe")) {
                    return UrgencyLevel.CRITICAL;
                }
                if (severityLower.contains("high") || severityLower.contains("moderate")) {
                    return UrgencyLevel.HIGH;
                }
            }
        }

        // Check encounter class
        if (encounter != null) {
            String encounterClass = encounterAdapter.extractClass(encounter);
            if (encounterClass != null) {
                String classLower = encounterClass.toLowerCase();
                // Check for emergency/urgent codes
                if (classLower.contains("emergency") || classLower.contains("urgent") ||
                        "emr".equals(classLower) || "emergency".equals(classLower)) {
                    return UrgencyLevel.CRITICAL;
                }
                // Check for inpatient codes (IMP, inpatient, etc.)
                if (classLower.contains("inpatient") || "imp".equals(classLower)) {
                    return UrgencyLevel.HIGH;
                }
                // Check for outpatient/ambulatory codes
                if (classLower.contains("outpatient") || classLower.contains("ambulatory") ||
                        "amb".equals(classLower) || "outpatient".equals(classLower)) {
                    return UrgencyLevel.MEDIUM;
                }
            }
        }

        // Default to MEDIUM
        return UrgencyLevel.MEDIUM;
    }

    /**
     * Determines required specialty from ICD-10 codes and conditions.
     * Uses actual medical specialty ICD-10 code ranges from the database.
     */
    private String determineRequiredSpecialty(List<String> icd10Codes, List<Condition> conditions) {
        if (icd10Codes.isEmpty() && conditions.isEmpty()) {
            return null;
        }

        // Load all medical specialties with their ICD-10 code ranges
        List<MedicalSpecialty> specialties = medicalSpecialtyRepository.findAll();
        if (specialties.isEmpty()) {
            // Fallback to simplified mapping if no specialties in database
            return determineRequiredSpecialtyFallback(icd10Codes, conditions);
        }

        // Build map of ICD-10 code ranges to specialty names
        Map<String, String> codeToSpecialtyMap = specialties.stream()
                .filter(s -> s.icd10CodeRanges() != null && !s.icd10CodeRanges().isEmpty())
                .flatMap(s -> s.icd10CodeRanges().stream()
                        .map(range -> Map.entry(range, s.name())))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, replacement) -> existing // Keep first if duplicates
                ));

        // Try to match ICD-10 codes to specialty ranges
        if (!icd10Codes.isEmpty()) {
            for (String icd10Code : icd10Codes) {
                if (icd10Code == null || icd10Code.isEmpty()) {
                    continue;
                }

                // Extract prefix (e.g., "I21.9" -> "I21", "E11.9" -> "E11")
                String prefix = extractIcd10Prefix(icd10Code);

                // Check if code matches any specialty range
                for (Map.Entry<String, String> entry : codeToSpecialtyMap.entrySet()) {
                    String range = entry.getKey();
                    if (matchesIcd10Range(prefix, range)) {
                        return entry.getValue();
                    }
                }
            }
        }

        // Fallback: check condition code text for specialty hints
        if (!conditions.isEmpty()) {
            String codeText = conditionAdapter.extractCodeText(conditions.get(0));
            if (codeText != null) {
                String textLower = codeText.toLowerCase();
                // Try to find matching specialty by description keywords
                for (MedicalSpecialty specialty : specialties) {
                    String description = specialty.description() != null ? specialty.description().toLowerCase() : "";
                    if (description.contains(textLower) || textLower.contains(description)) {
                        return specialty.name();
                    }
                }
            }
        }

        // Final fallback to simplified mapping
        return determineRequiredSpecialtyFallback(icd10Codes, conditions);
    }

    /**
     * Fallback method for determining required specialty using simplified mapping.
     */
    private String determineRequiredSpecialtyFallback(List<String> icd10Codes, List<Condition> conditions) {
        if (!icd10Codes.isEmpty()) {
            String firstCode = icd10Codes.get(0);
            if (firstCode.startsWith("I")) {
                return "Cardiology";
            }
            if (firstCode.startsWith("E")) {
                return "Endocrinology";
            }
            if (firstCode.startsWith("C")) {
                return "Oncology";
            }
            if (firstCode.startsWith("G")) {
                return "Neurology";
            }
        }

        if (!conditions.isEmpty()) {
            String codeText = conditionAdapter.extractCodeText(conditions.get(0));
            if (codeText != null) {
                String textLower = codeText.toLowerCase();
                if (textLower.contains("cardiac") || textLower.contains("heart")) {
                    return "Cardiology";
                }
                if (textLower.contains("diabetes") || textLower.contains("endocrine")) {
                    return "Endocrinology";
                }
                if (textLower.contains("cancer") || textLower.contains("tumor")) {
                    return "Oncology";
                }
            }
        }

        return null;
    }

    /**
     * Extracts ICD-10 code prefix (e.g., "I21.9" -> "I21", "E11" -> "E11").
     */
    private String extractIcd10Prefix(String code) {
        if (code == null || code.isEmpty()) {
            return "";
        }
        // Remove dot and everything after it
        int dotIndex = code.indexOf('.');
        if (dotIndex > 0) {
            return code.substring(0, dotIndex);
        }
        // If no dot, return first 3 characters (e.g., "I21" from "I21")
        return code.length() >= 3 ? code.substring(0, 3) : code;
    }

    /**
     * Checks if an ICD-10 code prefix matches a range (e.g., "I21" matches "I00-I99").
     */
    private boolean matchesIcd10Range(String codePrefix, String range) {
        if (codePrefix == null || codePrefix.isEmpty() || range == null || range.isEmpty()) {
            return false;
        }

        // Handle range format like "I00-I99" or "E00-E89"
        if (range.contains("-")) {
            String[] parts = range.split("-");
            if (parts.length == 2) {
                String start = parts[0].trim();
                String end = parts[1].trim();

                // Extract category prefix (e.g., "I" from "I00")
                if (codePrefix.length() > 0 && start.length() > 0) {
                    char codeCategory = codePrefix.charAt(0);
                    char startCategory = start.charAt(0);

                    // Category must match
                    if (codeCategory != startCategory) {
                        return false;
                    }

                    // Extract numeric part
                    try {
                        int codeNum = Integer.parseInt(codePrefix.substring(1));
                        int startNum = Integer.parseInt(start.substring(1));
                        int endNum = Integer.parseInt(end.substring(1));
                        return codeNum >= startNum && codeNum <= endNum;
                    } catch (NumberFormatException e) {
                        // If parsing fails, do simple prefix match
                        return codePrefix.startsWith(start.substring(0, Math.min(start.length(), codePrefix.length())));
                    }
                }
            }
        }

        // Handle single code match (e.g., "I50")
        return codePrefix.equals(range) || codePrefix.startsWith(range) || range.startsWith(codePrefix);
    }

    /**
     * Determines case type from encounter.
     */
    private CaseType determineCaseType(Encounter encounter) {
        if (encounter == null) {
            return CaseType.CONSULT_REQUEST; // Default
        }

        String encounterClass = encounterAdapter.extractClass(encounter);
        if (encounterClass == null) {
            return CaseType.CONSULT_REQUEST;
        }

        String classLower = encounterClass.toLowerCase();
        // Check for inpatient codes (IMP, inpatient, etc.)
        if (classLower.contains("inpatient") || "imp".equals(classLower)) {
            return CaseType.INPATIENT;
        }
        // Check for outpatient/ambulatory codes (AMB, outpatient, ambulatory, etc.)
        if (classLower.contains("outpatient") || classLower.contains("ambulatory") ||
                "amb".equals(classLower)) {
            return CaseType.SECOND_OPINION;
        }

        return CaseType.CONSULT_REQUEST;
    }

    /**
     * Builds additional notes from conditions, encounter, and observations.
     */
    private String buildAdditionalNotes(
            List<Condition> conditions,
            Encounter encounter,
            List<Observation> observations) {
        List<String> notes = new ArrayList<>();

        // Add condition notes
        for (Condition condition : conditions) {
            String severity = conditionAdapter.extractSeverity(condition);
            String status = conditionAdapter.extractClinicalStatus(condition);
            if (severity != null || status != null) {
                notes.add(String.format("Condition: severity=%s, status=%s", severity, status));
            }
        }

        // Add encounter notes
        if (encounter != null) {
            String type = encounterAdapter.extractType(encounter);
            String status = encounterAdapter.extractStatus(encounter);
            if (type != null || status != null) {
                notes.add(String.format("Encounter: type=%s, status=%s", type, status));
            }
        }

        return notes.isEmpty() ? null : String.join("; ", notes);
    }
}
