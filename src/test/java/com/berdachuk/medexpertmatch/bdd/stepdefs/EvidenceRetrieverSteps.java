package com.berdachuk.medexpertmatch.bdd.stepdefs;

import com.berdachuk.medexpertmatch.bdd.CucumberSpringConfiguration;
import com.berdachuk.medexpertmatch.evidence.domain.PubMedArticle;
import com.berdachuk.medexpertmatch.evidence.service.PubMedService;
import com.berdachuk.medexpertmatch.evidence.service.impl.PubMedServiceImpl;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

public class EvidenceRetrieverSteps extends CucumberSpringConfiguration {

    private WireMockServer wireMockServer;
    private PubMedService pubmedService;
    private String query;
    private List<PubMedArticle> results;

    @Before("@evidence-retriever or @clinical-guideline")
    public void setUp() throws IOException {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        configureFor(wireMockServer.port());

        String diabetesJson = new ClassPathResource("evidence/esearch-diabetes.json")
                .getContentAsString(StandardCharsets.UTF_8);
        String emptyJson = new ClassPathResource("evidence/esearch-empty.json")
                .getContentAsString(StandardCharsets.UTF_8);
        String diabetesXml = new ClassPathResource("evidence/efetch-diabetes.xml")
                .getContentAsString(StandardCharsets.UTF_8);

        stubFor(get(urlPathEqualTo("/entrez/eutils/esearch.fcgi"))
                .withQueryParam("db", equalTo("pubmed"))
                .withQueryParam("term", matching("diabetes.*"))
                .withQueryParam("retmax", equalTo("5"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json; charset=UTF-8")
                        .withBody(diabetesJson)));

        stubFor(get(urlPathEqualTo("/entrez/eutils/esearch.fcgi"))
                .withQueryParam("db", equalTo("pubmed"))
                .withQueryParam("term", equalTo(""))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json; charset=UTF-8")
                        .withBody(emptyJson)));

        stubFor(get(urlPathEqualTo("/entrez/eutils/efetch.fcgi"))
                .withQueryParam("db", equalTo("pubmed"))
                .withQueryParam("id", equalTo("12345678,23456789"))
                .withQueryParam("retmode", equalTo("xml"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml; charset=UTF-8")
                        .withBody(diabetesXml)));

        pubmedService = new PubMedServiceImpl(
                new org.springframework.web.client.RestTemplate(),
                "http://localhost:" + wireMockServer.port() + "/entrez/eutils");
    }

    @After("@evidence-retriever or @clinical-guideline")
    public void tearDown() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    @Given("a clinical condition {string}")
    public void aClinicalCondition(String condition) {
        this.query = condition;
    }

    @When("the system searches PubMed for evidence")
    public void theSystemSearchesPubMedForEvidence() {
        results = pubmedService.search(query, 5);
    }

    @Then("PubMed articles are returned with title and abstract")
    public void pubmedArticlesAreReturnedWithTitleAndAbstract() {
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertNotNull(results.getFirst().title());
        assertNotNull(results.getFirst().abstractText());
    }

    @Then("no PubMed articles are returned")
    public void noPubMedArticlesAreReturned() {
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Then("the PubMed articles have valid publication years")
    public void thePubMedArticlesHaveValidPublicationYears() {
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(a -> a.year() > 0));
    }
}
