package com.berdachuk.medexpertmatch.bdd.stepdefs;

import com.berdachuk.medexpertmatch.bdd.CucumberSpringConfiguration;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.facility.domain.Facility;
import com.berdachuk.medexpertmatch.facility.repository.FacilityRepository;
import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.retrieval.domain.FacilityMatch;
import com.berdachuk.medexpertmatch.retrieval.domain.RoutingOptions;
import com.berdachuk.medexpertmatch.retrieval.service.MatchingService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RoutingPlannerSteps extends CucumberSpringConfiguration {

    @Autowired
    private MatchingService matchingService;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    private MedicalCase testCase;
    private Facility testFacility;
    private Facility nearbyFacility;
    private Facility distantFacility;
    private List<FacilityMatch> matches;

    @Given("a medical case for routing requiring {string} specialty")
    public void aMedicalCaseForRoutingRequiringSpecialty(String specialty) {
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
                "Test case for routing",
                null
        );
        medicalCaseRepository.insert(testCase);
    }

    @Given("a facility with {string} and {string} capabilities exists")
    public void aFacilityWithCapabilitiesExists(String cap1, String cap2) {
        testFacility = new Facility(
                IdGenerator.generateFacilityId(),
                "Test Medical Center",
                "HOSPITAL",
                "City",
                "State",
                "US",
                BigDecimal.valueOf(40.7128),
                BigDecimal.valueOf(-74.0060),
                List.of(cap1, cap2),
                100,
                50
        );
        facilityRepository.insert(testFacility);
    }

    @Given("a medical case located in {string}")
    public void aMedicalCaseLocatedIn(String location) {
        String[] parts = location.split(", ");
        String city = parts[0];
        String state = parts[1];
        BigDecimal lat = city.equals("Baltimore") ? BigDecimal.valueOf(39.2904) : BigDecimal.valueOf(38.9072);
        BigDecimal lon = city.equals("Baltimore") ? BigDecimal.valueOf(-76.6122) : BigDecimal.valueOf(-77.0369);

        testCase = new MedicalCase(
                IdGenerator.generateId(),
                45,
                "Chest pain",
                "Needs routing",
                "Rule out ACS",
                List.of("I21.9"),
                List.of(),
                UrgencyLevel.HIGH,
                "Cardiology",
                CaseType.CONSULT_REQUEST,
                "Test case for routing",
                null,
                lat,
                lon
        );
        medicalCaseRepository.insert(testCase);
    }

    @Given("a nearby facility in {string} exists")
    public void aNearbyFacilityInExists(String location) {
        String[] parts = location.split(", ");
        nearbyFacility = new Facility(
                IdGenerator.generateFacilityId(),
                "Nearby " + parts[0] + " Center",
                "ACADEMIC",
                parts[0],
                parts[1],
                "US",
                BigDecimal.valueOf(39.2904),
                BigDecimal.valueOf(-76.6122),
                List.of("ICU", "CARDIOLOGY"),
                100,
                40
        );
        facilityRepository.insert(nearbyFacility);
    }

    @Given("a distant facility in {string} exists")
    public void aDistantFacilityInExists(String location) {
        String[] parts = location.split(", ");
        distantFacility = new Facility(
                IdGenerator.generateFacilityId(),
                "Distant " + parts[0] + " Center",
                "ACADEMIC",
                parts[0],
                parts[1],
                "US",
                BigDecimal.valueOf(34.0522),
                BigDecimal.valueOf(-118.2437),
                List.of("ICU", "CARDIOLOGY"),
                100,
                40
        );
        facilityRepository.insert(distantFacility);
    }

    @When("the system routes the case to facilities")
    public void theSystemRoutesTheCaseToFacilities() {
        matches = matchingService.matchFacilitiesForCase(testCase.id(), RoutingOptions.defaultOptions());
    }

    @Then("the routing results include the facility")
    public void theRoutingResultsIncludeTheFacility() {
        assertNotNull(matches);
        assertTrue(matches.stream().anyMatch(m -> m.facility().id().equals(testFacility.id())));
    }

    @Then("the routing result has a route score greater than {int}")
    public void theRoutingResultHasARouteScoreGreaterThan(int minScore) {
        assertNotNull(matches);
        assertFalse(matches.isEmpty());
        assertTrue(matches.stream().anyMatch(m -> m.routeScore() > minScore));
    }

    @Then("the nearby facility has a higher geographic score than the distant facility")
    public void theNearbyFacilityHasAHigherGeographicScoreThanTheDistantFacility() {
        assertNotNull(matches);
        FacilityMatch nearbyMatch = matches.stream()
                .filter(m -> m.facility().id().equals(nearbyFacility.id()))
                .findFirst()
                .orElse(null);
        FacilityMatch distantMatch = matches.stream()
                .filter(m -> m.facility().id().equals(distantFacility.id()))
                .findFirst()
                .orElse(null);
        assertNotNull(nearbyMatch, "Nearby facility should be in results");
        assertNotNull(distantMatch, "Distant facility should be in results");
        assertTrue(nearbyMatch.routeScore() > distantMatch.routeScore(),
                "Nearby facility should have higher route score than distant facility");
    }
}
