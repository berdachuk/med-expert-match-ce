package com.berdachuk.medexpertmatch.ingestion.adapter;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import org.hl7.fhir.r5.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for FHIR adapters.
 * Tests conversion of FHIR R5 resources to MedicalCase entities.
 */
class FhirAdapterIT extends BaseIntegrationTest {

    @Autowired
    private FhirBundleAdapter fhirBundleAdapter;

    @Autowired
    private FhirPatientAdapter fhirPatientAdapter;

    @Autowired
    private FhirConditionAdapter fhirConditionAdapter;

    @Autowired
    private FhirEncounterAdapter fhirEncounterAdapter;

    @Autowired
    private FhirObservationAdapter fhirObservationAdapter;

    @Test
    void testConvertBundleToMedicalCase() {
        // Create FHIR Bundle with Patient, Condition, Observation, Encounter
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        // Create Patient (anonymized - no PHI)
        Patient patient = new Patient();
        // Use LocalDate for accurate age calculation (accounts for leap years)
        java.time.LocalDate birthDate = java.time.LocalDate.now().minusYears(45);
        patient.setBirthDate(java.util.Date.from(birthDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        // No identifiers, names, addresses, or telecom (anonymized)

        // Create Condition with ICD-10 code
        Condition condition = new Condition();
        CodeableConcept conditionCode = new CodeableConcept();
        Coding icd10Coding = new Coding();
        icd10Coding.setSystem("http://hl7.org/fhir/sid/icd-10");
        icd10Coding.setCode("I21.9");
        icd10Coding.setDisplay("Acute myocardial infarction, unspecified");
        conditionCode.addCoding(icd10Coding);
        conditionCode.setText("Acute myocardial infarction");
        condition.setCode(conditionCode);
        condition.setClinicalStatus(new CodeableConcept().addCoding(
                new Coding().setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                        .setCode("active")));

        // Create Observation (symptoms)
        Observation observation = new Observation();
        CodeableConcept obsCode = new CodeableConcept();
        obsCode.setText("Chest pain");
        observation.setCode(obsCode);
        observation.setValue(new StringType("Severe chest pain radiating to left arm"));

        // Create Encounter
        Encounter encounter = new Encounter();
        encounter.setStatus(org.hl7.fhir.r5.model.Enumerations.EncounterStatus.INPROGRESS);
        CodeableConcept classConcept = new CodeableConcept();
        classConcept.addCoding(new Coding().setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
                .setCode("IMP")
                .setDisplay("inpatient encounter"));
        encounter.addClass_(classConcept);

        // Add resources to Bundle
        bundle.addEntry().setResource(patient);
        bundle.addEntry().setResource(condition);
        bundle.addEntry().setResource(observation);
        bundle.addEntry().setResource(encounter);

        // Convert Bundle to MedicalCase
        MedicalCase medicalCase = fhirBundleAdapter.convertBundleToMedicalCase(bundle);

        assertNotNull(medicalCase);
        assertNotNull(medicalCase.id());
        // Age calculation accounts for leap years, so should be exactly 45
        assertEquals(45, medicalCase.patientAge().intValue());
        assertTrue(medicalCase.icd10Codes().contains("I21.9"));
        assertEquals(UrgencyLevel.HIGH, medicalCase.urgencyLevel()); // Inpatient encounter -> HIGH
        assertEquals(CaseType.INPATIENT, medicalCase.caseType());
        assertNotNull(medicalCase.chiefComplaint());
        assertNotNull(medicalCase.symptoms());
    }

    @Test
    void testConvertBundleWithMultipleConditions() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        // Create Patient
        Patient patient = new Patient();
        patient.setBirthDate(new Date(System.currentTimeMillis() - (50L * 365 * 24 * 60 * 60 * 1000)));

        // Create multiple Conditions
        Condition condition1 = new Condition();
        CodeableConcept code1 = new CodeableConcept();
        code1.addCoding(new Coding()
                .setSystem("http://hl7.org/fhir/sid/icd-10")
                .setCode("I50.9")
                .setDisplay("Heart failure, unspecified"));
        condition1.setCode(code1);

        Condition condition2 = new Condition();
        CodeableConcept code2 = new CodeableConcept();
        code2.addCoding(new Coding()
                .setSystem("http://hl7.org/fhir/sid/icd-10")
                .setCode("E11.9")
                .setDisplay("Type 2 diabetes mellitus without complications"));
        condition2.setCode(code2);

        // Create Encounter
        Encounter encounter = new Encounter();
        encounter.setStatus(org.hl7.fhir.r5.model.Enumerations.EncounterStatus.COMPLETED);
        CodeableConcept classConcept = new CodeableConcept();
        classConcept.addCoding(new Coding().setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
                .setCode("AMB")
                .setDisplay("ambulatory"));
        encounter.addClass_(classConcept);

        bundle.addEntry().setResource(patient);
        bundle.addEntry().setResource(condition1);
        bundle.addEntry().setResource(condition2);
        bundle.addEntry().setResource(encounter);

        MedicalCase medicalCase = fhirBundleAdapter.convertBundleToMedicalCase(bundle);

        assertNotNull(medicalCase);
        assertTrue(medicalCase.icd10Codes().contains("I50.9"));
        assertTrue(medicalCase.icd10Codes().contains("E11.9"));
        assertEquals(2, medicalCase.icd10Codes().size());
    }

    @Test
    void testConvertBundleWithSnomedCodes() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        Patient patient = new Patient();
        patient.setBirthDate(new Date(System.currentTimeMillis() - (30L * 365 * 24 * 60 * 60 * 1000)));

        Condition condition = new Condition();
        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding()
                .setSystem("http://snomed.info/sct")
                .setCode("22298006")
                .setDisplay("Myocardial infarction"));
        condition.setCode(code);

        Encounter encounter = new Encounter();
        encounter.setStatus(org.hl7.fhir.r5.model.Enumerations.EncounterStatus.COMPLETED);
        CodeableConcept classConcept = new CodeableConcept();
        classConcept.addCoding(new Coding().setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
                .setCode("AMB"));
        encounter.addClass_(classConcept);

        bundle.addEntry().setResource(patient);
        bundle.addEntry().setResource(condition);
        bundle.addEntry().setResource(encounter);

        MedicalCase medicalCase = fhirBundleAdapter.convertBundleToMedicalCase(bundle);

        assertNotNull(medicalCase);
        assertTrue(medicalCase.snomedCodes().contains("22298006"));
    }

    @Test
    void testConvertBundleRejectsPhi() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        // Create Patient with PHI (identifiers, names)
        Patient patient = new Patient();
        patient.setBirthDate(new Date(System.currentTimeMillis() - (40L * 365 * 24 * 60 * 60 * 1000)));
        patient.addIdentifier().setValue("123-45-6789"); // PHI - SSN
        patient.addName().setFamily("Smith").addGiven("John"); // PHI - Name

        Condition condition = new Condition();
        condition.setCode(new CodeableConcept().addCoding(
                new Coding().setSystem("http://hl7.org/fhir/sid/icd-10").setCode("I21.9")));

        bundle.addEntry().setResource(patient);
        bundle.addEntry().setResource(condition);

        // Should reject Bundle with PHI
        assertThrows(IllegalArgumentException.class, () -> {
            fhirBundleAdapter.convertBundleToMedicalCase(bundle);
        });
    }

    @Test
    void testIsValidBundle() {
        Bundle validBundle = new Bundle();
        validBundle.setType(Bundle.BundleType.COLLECTION);
        validBundle.addEntry().setResource(new Patient());

        assertTrue(fhirBundleAdapter.isValidBundle(validBundle));

        Bundle invalidBundle = new Bundle();
        assertFalse(fhirBundleAdapter.isValidBundle(invalidBundle));

        assertFalse(fhirBundleAdapter.isValidBundle(null));
    }

    @Test
    void testFhirPatientAdapterExtractAge() {
        Patient patient = new Patient();
        // Use LocalDate for accurate age calculation (accounts for leap years)
        java.time.LocalDate birthDate = java.time.LocalDate.now().minusYears(35);
        patient.setBirthDate(java.util.Date.from(birthDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));

        Integer age = fhirPatientAdapter.extractAge(patient);
        // Age calculation accounts for leap years, so should be exactly 35
        assertEquals(35, age.intValue());
    }

    @Test
    void testFhirPatientAdapterIsAnonymized() {
        // Anonymized patient (no PHI)
        Patient anonymizedPatient = new Patient();
        anonymizedPatient.setBirthDate(new Date());
        assertTrue(fhirPatientAdapter.isAnonymized(anonymizedPatient));

        // Patient with PHI
        Patient patientWithPhi = new Patient();
        patientWithPhi.addIdentifier().setValue("123-45-6789");
        patientWithPhi.addName().setFamily("Smith");
        assertFalse(fhirPatientAdapter.isAnonymized(patientWithPhi));
    }

    @Test
    void testFhirConditionAdapterExtractIcd10Codes() {
        Condition condition = new Condition();
        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding()
                .setSystem("http://hl7.org/fhir/sid/icd-10")
                .setCode("I21.9"));
        code.addCoding(new Coding()
                .setSystem("http://hl7.org/fhir/sid/icd-10")
                .setCode("E11.9"));
        condition.setCode(code);

        List<String> icd10Codes = fhirConditionAdapter.extractIcd10Codes(condition);
        assertTrue(icd10Codes.contains("I21.9"));
        assertTrue(icd10Codes.contains("E11.9"));
        assertEquals(2, icd10Codes.size());
    }

    @Test
    void testFhirConditionAdapterExtractSnomedCodes() {
        Condition condition = new Condition();
        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding()
                .setSystem("http://snomed.info/sct")
                .setCode("22298006"));
        condition.setCode(code);

        List<String> snomedCodes = fhirConditionAdapter.extractSnomedCodes(condition);
        assertTrue(snomedCodes.contains("22298006"));
    }

    @Test
    void testFhirEncounterAdapterExtractType() {
        Encounter encounter = new Encounter();
        encounter.addType().setText("Emergency Department Visit");
        encounter.setStatus(org.hl7.fhir.r5.model.Enumerations.EncounterStatus.COMPLETED);

        String type = fhirEncounterAdapter.extractType(encounter);
        assertEquals("Emergency Department Visit", type);
    }

    @Test
    void testFhirEncounterAdapterExtractStatus() {
        Encounter encounter = new Encounter();
        encounter.setStatus(org.hl7.fhir.r5.model.Enumerations.EncounterStatus.COMPLETED);

        String status = fhirEncounterAdapter.extractStatus(encounter);
        // FHIR R5 EncounterStatus.COMPLETED.toCode() returns "completed"
        assertEquals("completed", status);
    }

    @Test
    void testFhirObservationAdapterExtractCodeText() {
        Observation observation = new Observation();
        observation.setCode(new CodeableConcept().setText("Chest pain"));

        String codeText = fhirObservationAdapter.extractCodeText(observation);
        assertEquals("Chest pain", codeText);
    }
}
