package com.berdachuk.medexpertmatch.evidence.service;

import com.berdachuk.medexpertmatch.evidence.domain.PubMedArticle;
import com.berdachuk.medexpertmatch.evidence.service.impl.PubMedServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PubMedServiceImplUnitTest {

    private PubMedServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        service = new PubMedServiceImpl();
    }

    @Test
    void parsePmidsFromValidSearchResponseShouldReturnIds() throws Exception {
        String jsonResponse = "{\"esearchresult\":{\"idlist\":[\"12345678\",\"23456789\"],\"count\":\"2\"}}";
        Method method = PubMedServiceImpl.class.getDeclaredMethod("parsePmidsFromSearchResponse", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> pmids = (List<String>) method.invoke(service, jsonResponse);
        assertEquals(2, pmids.size());
        assertEquals("12345678", pmids.get(0));
        assertEquals("23456789", pmids.get(1));
    }

    @Test
    void parsePmidsFromEmptyResponseShouldReturnEmptyList() throws Exception {
        String jsonResponse = "";
        Method method = PubMedServiceImpl.class.getDeclaredMethod("parsePmidsFromSearchResponse", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> pmids = (List<String>) method.invoke(service, jsonResponse);
        assertTrue(pmids.isEmpty());
    }

    @Test
    void parsePmidsFromMalformedJsonShouldReturnEmptyList() throws Exception {
        String jsonResponse = "not json at all";
        Method method = PubMedServiceImpl.class.getDeclaredMethod("parsePmidsFromSearchResponse", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> pmids = (List<String>) method.invoke(service, jsonResponse);
        assertTrue(pmids.isEmpty());
    }

    @Test
    void parseArticlesFromValidXmlShouldReturnArticles() throws Exception {
        String xmlResponse = "<PubmedArticleSet>" +
                "<PubmedArticle>" +
                "<MedlineCitation><PMID version=\"1\">12345678</PMID>" +
                "<Article><Journal><Title>Test Journal</Title>" +
                "<JournalIssue><PubDate><Year>2023</Year></PubDate></JournalIssue></Journal>" +
                "<ArticleTitle>Test Article Title</ArticleTitle>" +
                "<Abstract><AbstractText>Test abstract content</AbstractText></Abstract>" +
                "<AuthorList><Author><LastName>Smith</LastName><FirstName>John</FirstName></Author></AuthorList>" +
                "</Article></MedlineCitation>" +
                "</PubmedArticle>" +
                "</PubmedArticleSet>";

        Method method = PubMedServiceImpl.class.getDeclaredMethod("parseArticlesFromXml", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<PubMedArticle> articles = (List<PubMedArticle>) method.invoke(service, xmlResponse);

        assertEquals(1, articles.size());
        PubMedArticle article = articles.get(0);
        assertEquals("Test Article Title", article.title());
        assertEquals("Test abstract content", article.abstractText());
        assertEquals("Test Journal", article.journal());
        assertEquals(2023, article.year());
        assertEquals("12345678", article.pmid());
        assertEquals(1, article.authors().size());
        assertEquals("Smith, John", article.authors().get(0));
    }

    @Test
    void parseArticlesFromEmptyXmlShouldReturnEmptyList() throws Exception {
        String xmlResponse = "";
        Method method = PubMedServiceImpl.class.getDeclaredMethod("parseArticlesFromXml", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<PubMedArticle> articles = (List<PubMedArticle>) method.invoke(service, xmlResponse);
        assertTrue(articles.isEmpty());
    }

    @Test
    void parseArticlesFromMalformedXmlShouldReturnEmptyListGracefully() throws Exception {
        String xmlResponse = "<garbage>not valid</garbage>";
        Method method = PubMedServiceImpl.class.getDeclaredMethod("parseArticlesFromXml", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<PubMedArticle> articles = (List<PubMedArticle>) method.invoke(service, xmlResponse);
        assertTrue(articles.isEmpty());
    }

    @Test
    void parseArticlesWithMissingFieldsShouldReturnPartialArticle() throws Exception {
        String xmlResponse = "<PubmedArticleSet>" +
                "<PubmedArticle>" +
                "<MedlineCitation><PMID version=\"1\">99999999</PMID>" +
                "<Article><Journal></Journal>" +
                "<ArticleTitle>Only Title</ArticleTitle>" +
                "</Article></MedlineCitation>" +
                "</PubmedArticle>" +
                "</PubmedArticleSet>";

        Method method = PubMedServiceImpl.class.getDeclaredMethod("parseArticlesFromXml", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<PubMedArticle> articles = (List<PubMedArticle>) method.invoke(service, xmlResponse);

        assertEquals(1, articles.size());
        assertEquals("Only Title", articles.get(0).title());
        assertEquals("", articles.get(0).abstractText());
        assertEquals("99999999", articles.get(0).pmid());
    }

    @Test
    void encodeQueryWithSpacesShouldUrlEncode() throws Exception {
        Method method = PubMedServiceImpl.class.getDeclaredMethod("encodeQuery", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(service, "diabetes type 2");
        assertTrue(result.contains("diabetes"));
        assertTrue(result.contains("+") || result.contains("%20"));
    }

    @Test
    void encodeQueryWithNullShouldReturnEmpty() throws Exception {
        Method method = PubMedServiceImpl.class.getDeclaredMethod("encodeQuery", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(service, (String) null);
        assertEquals("", result);
    }

    @Test
    void cleanXmlTextShouldStripTags() throws Exception {
        Method method = PubMedServiceImpl.class.getDeclaredMethod("cleanXmlText", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(service, "<b>Hello</b> <i>World</i>");
        assertEquals("Hello World", result);
    }

    @Test
    void cleanXmlTextShouldDecodeEntities() throws Exception {
        Method method = PubMedServiceImpl.class.getDeclaredMethod("cleanXmlText", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(service, "A &amp; B &lt; C &gt; D");
        assertEquals("A & B < C > D", result);
    }

    @Test
    void cleanXmlTextWithNullShouldReturnEmpty() throws Exception {
        Method method = PubMedServiceImpl.class.getDeclaredMethod("cleanXmlText", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(service, (String) null);
        assertEquals("", result);
    }
}
