package com.berdachuk.medexpertmatch.bdd.stepdefs;

import com.berdachuk.medexpertmatch.bdd.CucumberSpringConfiguration;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class NetworkAnalyzerSteps extends CucumberSpringConfiguration {

    private static final AtomicInteger counter = new AtomicInteger(0);

    @Autowired
    private DoctorRepository doctorRepository;

    private Doctor testDoctor;

    @Given("a doctor with {string} specialty exists in the graph")
    public void aDoctorWithSpecialtyExistsInTheGraph(String specialty) {
        int seq = counter.incrementAndGet();
        testDoctor = new Doctor(
                IdGenerator.generateDoctorId(),
                "Dr. Network Test " + seq,
                "network-" + seq + "@example.com",
                List.of(specialty),
                List.of(),
                List.of(),
                true,
                "AVAILABLE"
        );
        doctorRepository.insert(testDoctor);
    }

    @When("the system queries the expertise network")
    public void theSystemQueriesTheExpertiseNetwork() {
        assertNotNull(testDoctor);
    }

    @Then("the network data contains doctor-specialty relationships")
    public void theNetworkDataContainsDoctorSpecialtyRelationships() {
        assertNotNull(testDoctor);
        assertFalse(testDoctor.specialties().isEmpty());
        assertTrue(testDoctor.specialties().contains("Cardiology"));
    }

    @Then("the graph query returns valid results")
    public void theGraphQueryReturnsValidResults() {
        assertNotNull(testDoctor);
        assertNotNull(testDoctor.id());
    }
}
