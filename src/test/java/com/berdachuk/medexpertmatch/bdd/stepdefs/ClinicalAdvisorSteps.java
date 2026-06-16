package com.berdachuk.medexpertmatch.bdd.stepdefs;

import com.berdachuk.medexpertmatch.bdd.CucumberSpringConfiguration;
import com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience;
import com.berdachuk.medexpertmatch.clinicalexperience.repository.ClinicalExperienceRepository;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ClinicalAdvisorSteps extends CucumberSpringConfiguration {

    private static final AtomicInteger counter = new AtomicInteger(0);

    @Autowired
    private ClinicalExperienceRepository clinicalExperienceRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    private MedicalCase testCase;
    private Doctor testDoctor;
    private List<ClinicalExperience> experiences;

    @Given("a doctor with clinical experience for the case exists")
    public void aDoctorWithClinicalExperienceForTheCaseExists() {
        int seq = counter.incrementAndGet();
        testCase = new MedicalCase(
                IdGenerator.generateId(),
                45,
                "Chest pain",
                "Additional notes",
                "Acute myocardial infarction",
                List.of("I21.9"),
                List.of(),
                UrgencyLevel.HIGH,
                "Cardiology",
                CaseType.CONSULT_REQUEST,
                "Test case for clinical advisor",
                null
        );
        medicalCaseRepository.insert(testCase);

        testDoctor = new Doctor(
                IdGenerator.generateDoctorId(),
                "Dr. Clinical Advisor",
                "clinical-advisor-" + seq + "@example.com",
                List.of("Cardiology"),
                List.of("Board Certified"),
                List.of(),
                true,
                "AVAILABLE"
        );
        doctorRepository.insert(testDoctor);

        ClinicalExperience experience = new ClinicalExperience(
                IdGenerator.generateId(),
                testDoctor.id(),
                testCase.id(),
                List.of("Echocardiogram", "Medication adjustment"),
                "MEDIUM",
                "SUCCESS",
                List.of(),
                7,
                5
        );
        clinicalExperienceRepository.insert(experience);
    }

    @When("the system retrieves clinical experience for the doctor")
    public void theSystemRetrievesClinicalExperienceForTheDoctor() {
        experiences = clinicalExperienceRepository.findByDoctorId(testDoctor.id());
    }

    @Then("the clinical experience contains outcome and rating data")
    public void theClinicalExperienceContainsOutcomeAndRatingData() {
        assertNotNull(experiences);
        assertFalse(experiences.isEmpty());
        assertNotNull(experiences.getFirst().outcome());
        assertNotNull(experiences.getFirst().rating());
    }

    @Then("the clinical experience contains procedure data")
    public void theClinicalExperienceContainsProcedureData() {
        assertNotNull(experiences);
        assertFalse(experiences.isEmpty());
        assertNotNull(experiences.getFirst().proceduresPerformed());
        assertFalse(experiences.getFirst().proceduresPerformed().isEmpty());
    }
}
