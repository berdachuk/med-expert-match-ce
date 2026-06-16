package com.berdachuk.medexpertmatch.bdd.stepdefs;

import com.berdachuk.medexpertmatch.bdd.CucumberSpringConfiguration;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;
import com.berdachuk.medexpertmatch.retrieval.domain.MatchOptions;
import com.berdachuk.medexpertmatch.retrieval.service.MatchingService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class DoctorMatcherSteps extends CucumberSpringConfiguration {

    private static final AtomicInteger scenarioCounter = new AtomicInteger(0);

    @Autowired
    private MatchingService matchingService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    private MedicalCase testCase;
    private Doctor cardiologist;
    private Doctor neurologist;
    private List<DoctorMatch> matches;

    @Given("a medical case requiring {string} specialty")
    public void aMedicalCaseRequiringSpecialty(String specialty) {
        testCase = new MedicalCase(
                IdGenerator.generateId(),
                50,
                "Heart condition",
                "Chest pain and shortness of breath",
                "Suspected cardiac issue",
                List.of("I21.9"),
                List.of(),
                UrgencyLevel.HIGH,
                specialty,
                CaseType.CONSULT_REQUEST,
                "Test case for matching",
                null
        );
        medicalCaseRepository.insert(testCase);
    }

    @Given("a cardiologist doctor exists")
    public void aCardiologistDoctorExists() {
        int seq = scenarioCounter.incrementAndGet();
        cardiologist = new Doctor(
                IdGenerator.generateDoctorId(),
                "Dr. Cardiology Specialist",
                "cardio-" + seq + "@example.com",
                List.of("Cardiology"),
                List.of("Board Certified"),
                List.of(),
                true,
                "AVAILABLE"
        );
        doctorRepository.insert(cardiologist);
    }

    @Given("a neurologist doctor exists")
    public void aNeurologistDoctorExists() {
        int seq = scenarioCounter.incrementAndGet();
        neurologist = new Doctor(
                IdGenerator.generateDoctorId(),
                "Dr. Neurology Specialist",
                "neuro-" + seq + "@example.com",
                List.of("Neurology"),
                List.of(),
                List.of(),
                true,
                "AVAILABLE"
        );
        doctorRepository.insert(neurologist);
    }

    @Given("a doctor with cardiology specialty exists")
    public void aDoctorWithCardiologySpecialtyExists() {
        int seq = scenarioCounter.incrementAndGet();
        cardiologist = new Doctor(
                IdGenerator.generateDoctorId(),
                "Dr. Cardiology Specialist",
                "cardio-" + seq + "@example.com",
                List.of("Cardiology"),
                List.of("Board Certified"),
                List.of(),
                true,
                "AVAILABLE"
        );
        doctorRepository.insert(cardiologist);
    }

    @When("the system matches doctors to the case")
    public void theSystemMatchesDoctorsToTheCase() {
        MatchOptions options = MatchOptions.builder()
                .maxResults(10)
                .preferredSpecialties(List.of(testCase.requiredSpecialty()))
                .build();
        matches = matchingService.matchDoctorsToCase(testCase.id(), options);
    }

    @Then("the match results include the cardiologist")
    public void theMatchResultsIncludeTheCardiologist() {
        assertNotNull(matches);
        assertTrue(matches.stream().anyMatch(m -> m.doctor().id().equals(cardiologist.id())));
    }

    @Then("the match results exclude the neurologist")
    public void theMatchResultsExcludeTheNeurologist() {
        assertNotNull(matches);
        assertTrue(matches.stream().noneMatch(m -> m.doctor().id().equals(neurologist.id())));
    }

    @Then("each match result has a score greater than {int}")
    public void eachMatchResultHasAScoreGreaterThan(int minScore) {
        assertNotNull(matches);
        assertFalse(matches.isEmpty());
        assertTrue(matches.stream().allMatch(m -> m.matchScore() > minScore));
    }

    @Then("each match result has a rank starting from {int}")
    public void eachMatchResultHasARankStartingFrom(int startRank) {
        assertNotNull(matches);
        assertFalse(matches.isEmpty());
        for (int i = 0; i < matches.size(); i++) {
            assertEquals(i + startRank, matches.get(i).rank());
        }
    }
}
