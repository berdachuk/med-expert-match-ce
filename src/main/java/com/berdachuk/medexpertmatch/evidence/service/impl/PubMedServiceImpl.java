package com.berdachuk.medexpertmatch.evidence.service.impl;

import com.berdachuk.medexpertmatch.evidence.domain.PubMedArticle;
import com.berdachuk.medexpertmatch.evidence.exception.EvidenceRetrievalException;
import com.berdachuk.medexpertmatch.evidence.service.PubMedService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service implementation for querying PubMed via NCBI E-utilities API.
 * Uses NCBI-recommended tool/email params and User-Agent for identification.
 */
@Slf4j
@Service
public class PubMedServiceImpl implements PubMedService {

    private static final String PUBMED_BASE_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils";
    private static final String NCBI_TOOL = "MedExpertMatch";
    private static final String NCBI_EMAIL = "support@medexpertmatch.com";
    private static final String USER_AGENT = "MedExpertMatch/1.0 (evidence retrieval; mailto:" + NCBI_EMAIL + ")";

    private final RestTemplate restTemplate;

    public PubMedServiceImpl() {
        RestTemplate template = new RestTemplate();
        template.setInterceptors(List.of(new ClientHttpRequestInterceptor() {
            @Override
            public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                                ClientHttpRequestExecution execution) throws IOException {
                request.getHeaders().set("User-Agent", USER_AGENT);
                return execution.execute(request, body);
            }
        }));
        this.restTemplate = template;
    }

    @Override
    public List<PubMedArticle> search(String query, int maxResults) {
        log.info("Searching PubMed for query: {}, maxResults: {}", query, maxResults);

        try {
            // Step 1: Search PubMed to get PMIDs (tool/email per NCBI recommendation)
            String encodedTerm = encodeQuery(query);
            String searchUrl = String.format("%s/esearch.fcgi?db=pubmed&term=%s&retmax=%d&retmode=json&tool=%s&email=%s",
                    PUBMED_BASE_URL, encodedTerm, maxResults,
                    URLEncoder.encode(NCBI_TOOL, StandardCharsets.UTF_8),
                    URLEncoder.encode(NCBI_EMAIL, StandardCharsets.UTF_8));

            log.info("PubMed esearch request: URL length={}, term length (encoded)={}", searchUrl.length(), encodedTerm.length());
            log.debug("PubMed esearch URL: {}", searchUrl);

            String searchResponse = restTemplate.getForObject(searchUrl, String.class);

            if (searchResponse == null || searchResponse.trim().isEmpty()) {
                log.warn("PubMed esearch: empty response");
                return List.of();
            }

            int responseLen = searchResponse.length();
            boolean hasIdList = searchResponse.toLowerCase().contains("idlist");
            boolean looksLikeJson = searchResponse.trim().startsWith("{");
            log.info("PubMed esearch response: length={}, looksLikeJson={}, containsIdList={}",
                    responseLen, looksLikeJson, hasIdList);

            if (!hasIdList) {
                log.warn("PubMed esearch: response may be error or unexpected format (no idlist). First 200 chars: {}",
                        responseLen > 200 ? searchResponse.substring(0, 200) + "..." : searchResponse);
            }

            // Parse PMIDs from JSON response (simplified parsing)
            List<String> pmids = parsePmidsFromSearchResponse(searchResponse);
            log.info("PubMed esearch: parsed PMID count={} for query={}", pmids.size(), query);

            if (pmids.isEmpty()) {
                log.info("PubMed esearch: no PMIDs. Request URL (verify from app server with curl): {}", searchUrl);
                return List.of();
            }

            // Step 2: Fetch article details using PMIDs (tool/email per NCBI recommendation)
            String fetchUrl = String.format("%s/efetch.fcgi?db=pubmed&id=%s&retmode=xml&tool=%s&email=%s",
                    PUBMED_BASE_URL, String.join(",", pmids),
                    URLEncoder.encode(NCBI_TOOL, StandardCharsets.UTF_8),
                    URLEncoder.encode(NCBI_EMAIL, StandardCharsets.UTF_8));

            log.info("PubMed efetch request: PMID count={}", pmids.size());
            log.debug("PubMed efetch URL: {}", fetchUrl);

            String fetchResponse = restTemplate.getForObject(fetchUrl, String.class);

            if (fetchResponse == null || fetchResponse.trim().isEmpty()) {
                log.warn("PubMed efetch: empty response");
                return List.of();
            }

            log.info("PubMed efetch response: length={}", fetchResponse.length());

            // Parse XML response to extract article details
            List<PubMedArticle> articles = parseArticlesFromXml(fetchResponse);
            log.info("PubMed parse: extracted article count={}", articles.size());
            return articles;

        } catch (RestClientException e) {
            log.error("Error querying PubMed API", e);
            throw new EvidenceRetrievalException("PubMed API request failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error querying PubMed", e);
            throw new EvidenceRetrievalException("Unexpected error querying PubMed: " + e.getMessage(), e);
        }
    }

    /**
     * Encodes query string for URL (RFC 3986; spaces as + for query params).
     */
    private String encodeQuery(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        return URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
    }

    /**
     * Parses PMIDs from PubMed search JSON response.
     * NCBI returns lowercase "idlist"; match case-insensitively.
     */
    private List<String> parsePmidsFromSearchResponse(String jsonResponse) {
        List<String> pmids = new ArrayList<>();

        try {
            // NCBI returns "idlist":["12345678",...]; match case-insensitively
            Pattern pattern = Pattern.compile("\"idlist\":\\s*\\[([^\\]]+)\\]", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(jsonResponse);

            if (matcher.find()) {
                String idList = matcher.group(1);
                // Extract individual PMIDs (quoted strings)
                Pattern pmidPattern = Pattern.compile("\"([0-9]+)\"");
                Matcher pmidMatcher = pmidPattern.matcher(idList);
                while (pmidMatcher.find()) {
                    pmids.add(pmidMatcher.group(1));
                }
            }
        } catch (Exception e) {
            log.warn("Error parsing PMIDs from search response", e);
        }

        return pmids;
    }

    /**
     * Parses article details from PubMed XML response.
     * Extracts title, abstract, authors, journal, year, and PMID.
     */
    private List<PubMedArticle> parseArticlesFromXml(String xmlResponse) {
        List<PubMedArticle> articles = new ArrayList<>();

        try {
            // Split XML by article boundaries (PubmedArticle elements)
            String[] articleSections = xmlResponse.split("<PubmedArticle>");

            for (int i = 1; i < articleSections.length; i++) { // Start at 1 to skip content before first article
                String articleXml = articleSections[i].split("</PubmedArticle>")[0];

                String title = extractXmlField(articleXml, "<ArticleTitle>", "</ArticleTitle>");
                String abstractText = extractXmlField(articleXml, "<AbstractText>", "</AbstractText>");
                if (abstractText.isEmpty()) {
                    abstractText = extractXmlFieldWithRegexStart(articleXml, "<AbstractText[^>]*>", "</AbstractText>");
                }

                List<String> authors = extractAuthors(articleXml);
                String journal = extractXmlField(articleXml, "<Title>", "</Title>");
                if (journal.isEmpty()) {
                    journal = extractXmlField(articleXml, "<MedlineTA>", "</MedlineTA>");
                }
                String yearStr = extractXmlField(articleXml, "<Year>", "</Year>");
                Integer year = null;
                try {
                    if (!yearStr.isEmpty()) {
                        year = Integer.parseInt(yearStr);
                    }
                } catch (NumberFormatException e) {
                    log.debug("Could not parse year: {}", yearStr);
                }
                String pmid = extractXmlFieldWithRegexStart(articleXml, "<PMID[^>]*>", "</PMID>");

                // Clean up extracted text (remove XML tags, decode entities)
                title = cleanXmlText(title);
                abstractText = cleanXmlText(abstractText);
                journal = cleanXmlText(journal);

                articles.add(new PubMedArticle(title, abstractText, authors, journal, year, pmid));
            }
        } catch (Exception e) {
            log.warn("Error parsing articles from XML response", e);
        }

        return articles;
    }

    /**
     * Extracts a field value from XML using literal start and end tags.
     */
    private String extractXmlField(String xml, String startTag, String endTag) {
        try {
            Pattern pattern = Pattern.compile(Pattern.quote(startTag) + "(.*?)" + Pattern.quote(endTag),
                    Pattern.DOTALL);
            Matcher matcher = pattern.matcher(xml);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        } catch (Exception e) {
            log.debug("Error extracting XML field", e);
        }
        return "";
    }

    /**
     * Extracts a field value from XML using a regex for the start tag (e.g. &lt;PMID[^&gt;]*&gt; for attributes).
     */
    private String extractXmlFieldWithRegexStart(String xml, String startTagRegex, String endTag) {
        try {
            Pattern pattern = Pattern.compile(startTagRegex + "(.*?)" + Pattern.quote(endTag), Pattern.DOTALL);
            Matcher matcher = pattern.matcher(xml);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        } catch (Exception e) {
            log.debug("Error extracting XML field with regex start", e);
        }
        return "";
    }

    /**
     * Extracts author names from XML.
     */
    private List<String> extractAuthors(String articleXml) {
        List<String> authors = new ArrayList<>();

        try {
            // Extract AuthorList section
            Pattern authorListPattern = Pattern.compile("<AuthorList[^>]*>(.*?)</AuthorList>", Pattern.DOTALL);
            Matcher authorListMatcher = authorListPattern.matcher(articleXml);

            if (authorListMatcher.find()) {
                String authorListXml = authorListMatcher.group(1);

                // Extract individual authors
                Pattern authorPattern = Pattern.compile("<Author[^>]*>(.*?)</Author>", Pattern.DOTALL);
                Matcher authorMatcher = authorPattern.matcher(authorListXml);

                while (authorMatcher.find()) {
                    String authorXml = authorMatcher.group(1);
                    String lastName = extractXmlField(authorXml, "<LastName>", "</LastName>");
                    String firstName = extractXmlField(authorXml, "<FirstName>", "</FirstName>");
                    String foreName = extractXmlField(authorXml, "<ForeName>", "</ForeName>");

                    if (!lastName.isEmpty()) {
                        String fullName = lastName;
                        if (!firstName.isEmpty()) {
                            fullName += ", " + firstName;
                        } else if (!foreName.isEmpty()) {
                            fullName += ", " + foreName;
                        }
                        authors.add(fullName);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error extracting authors", e);
        }

        return authors;
    }

    /**
     * Cleans XML text by removing tags and decoding entities.
     */
    private String cleanXmlText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Remove XML tags
        text = text.replaceAll("<[^>]+>", "");

        // Decode common XML entities
        text = text.replace("&amp;", "&");
        text = text.replace("&lt;", "<");
        text = text.replace("&gt;", ">");
        text = text.replace("&quot;", "\"");
        text = text.replace("&apos;", "'");
        text = text.replace("&#39;", "'");

        // Clean up whitespace
        text = text.replaceAll("\\s+", " ").trim();

        return text;
    }
}
