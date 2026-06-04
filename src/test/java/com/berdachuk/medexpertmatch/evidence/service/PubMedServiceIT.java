package com.berdachuk.medexpertmatch.evidence.service;

import com.berdachuk.medexpertmatch.evidence.domain.PubMedArticle;
import com.berdachuk.medexpertmatch.evidence.service.impl.PubMedServiceImpl;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class PubMedServiceIT {

    private WireMockServer wireMockServer;
    private PubMedService pubmedService;

    @BeforeEach
    void setUp() throws IOException {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        configureFor(wireMockServer.port());

        String baseUrl = "http://localhost:" + wireMockServer.port() + "/entrez/eutils";

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

        stubFor(get(urlPathEqualTo("/entrez/eutils/esearch.fcgi"))
                .withQueryParam("db", equalTo("pubmed"))
                .withQueryParam("retmax", equalTo("0"))
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

        RestTemplate restTemplate = new RestTemplate();
        pubmedService = new PubMedServiceImpl(restTemplate, baseUrl);
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    @Test
    void searchShouldReturnParsedArticles() {
        List<PubMedArticle> results = pubmedService.search("diabetes", 5);

        assertNotNull(results);
        assertEquals(2, results.size());

        PubMedArticle first = results.get(0);
        assertTrue(first.title().contains("Early Glycemic Control"));
        assertTrue(first.abstractText().contains("intensive glycemic control"));
        assertEquals("Diabetes care", first.journal());
        assertEquals("12345678", first.pmid());
        assertEquals(3, first.authors().size());
        assertEquals("Johnson, Michael R", first.authors().get(0));
        assertEquals("Williams, Sarah E", first.authors().get(1));
    }

    @Test
    void searchWithZeroMaxResultsShouldReturnEmptyList() {
        List<PubMedArticle> results = pubmedService.search("diabetes", 0);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void searchWithEmptyQueryShouldReturnEmptyList() {
        List<PubMedArticle> results = pubmedService.search("", 5);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void searchResultsShouldHaveValidTimestamps() {
        List<PubMedArticle> results = pubmedService.search("diabetes", 5);
        assertTrue(results.stream().allMatch(a -> a.year() > 0));
    }

    @Test
    void wireMockServerShouldBeRunning() {
        assertTrue(wireMockServer.isRunning());
    }
}
