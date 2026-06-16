package com.berdachuk.medexpertmatch.bdd.stepdefs;

import com.berdachuk.medexpertmatch.bdd.CucumberSpringConfiguration;
import com.berdachuk.medexpertmatch.caseanalysis.domain.CaseAnalysisResult;
import com.berdachuk.medexpertmatch.caseanalysis.service.CaseAnalysisService;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CaseAnalyzerSteps extends CucumberSpringConfiguration {

    @Autowired
    private CaseAnalysisService caseAnalysisService;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    private MedicalCase testCase;
    private CaseAnalysisResult analysisResult;
    private List<String> extractedIcd10Codes;
    private UrgencyLevel classifiedUrgency;

    @Given("a medical case with chief complaint {string} and diagnosis {string}")
    public void aMedicalCaseWithChiefComplaintAndDiagnosis(String complaint, String diagnosis) {
        testCase = new MedicalCase(
                IdGenerator.generateId(),
                45,
                complaint,
                "Additional notes for " + complaint,
                diagnosis,
                List.of("I21.9"),
                List.of(),
                UrgencyLevel.HIGH,
                "Cardiology",
                CaseType.CONSULT_REQUEST,
                "Test case for analysis",
                null
        );
        medicalCaseRepository.insert(testCase);
    }

    @Given("a medical case with urgency level {string}")
    public void aMedicalCaseWithUrgencyLevel(String urgency) {
        UrgencyLevel level = UrgencyLevel.valueOf(urgency);
        testCase = new MedicalCase(
                IdGenerator.generateId(),
                50,
                "Test complaint",
                "Test notes",
                "Test diagnosis",
                List.of("I21.9"),
                List.of(),
                level,
                "Cardiology",
                CaseType.CONSULT_REQUEST,
                "Test case for triage",
                null
        );
        medicalCaseRepository.insert(testCase);
    }

    @When("the system analyzes the case")
    public void theSystemAnalyzesTheCase() {
        analysisResult = caseAnalysisService.analyzeCase(testCase);
        extractedIcd10Codes = caseAnalysisService.extractICD10Codes(testCase);
        classifiedUrgency = caseAnalysisService.classifyUrgency(testCase);
    }

    @Then("the analysis result contains extracted entities")
    public void theAnalysisResultContainsExtractedEntities() {
        assertNotNull(analysisResult);
        assertNotNull(analysisResult.clinicalFindings());
    }

    @Then("the analysis result contains ICD-10 codes")
    public void theAnalysisResultContainsIcd10Codes() {
        assertNotNull(extractedIcd10Codes);
    }

    @Then("the analysis result contains an urgency classification")
    public void theAnalysisResultContainsAnUrgencyClassification() {
        assertNotNull(classifiedUrgency);
    }

    @Then("the urgency classification is not null")
    public void theUrgencyClassificationIsNotNull() {
        assertNotNull(classifiedUrgency);
    }

    @Then("the urgency classification is {string}")
    public void theUrgencyClassificationIs(String expectedUrgency) {
        assertEquals(UrgencyLevel.valueOf(expectedUrgency), classifiedUrgency);
    }
}
