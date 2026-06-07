package com.berdachuk.medexpertmatch.llm.harness;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HarnessContextSummarizerTest {

    private HarnessContextSummarizer summarizer;

    @BeforeEach
    void setUp() {
        summarizer = new HarnessContextSummarizerImpl(new ObjectMapper());
    }

    @Test
    @DisplayName("doctor match JSON compresses to top_matches without full doctor dumps")
    void summarizesDoctorMatches() {
        String raw = """
                [
                  {
                    "doctor": {
                      "id": "doc-1",
                      "name": "Dr. Smith",
                      "email": "s@example.com",
                      "specialties": ["Cardiology"],
                      "certifications": [],
                      "facilityIds": ["fac-1"],
                      "telehealthEnabled": true,
                      "availabilityStatus": "AVAILABLE"
                    },
                    "matchScore": 92.5,
                    "rank": 1,
                    "rationale": "Strong vector and graph alignment for cardiology referral."
                  },
                  {
                    "doctor": {
                      "id": "doc-2",
                      "name": "Dr. Jones",
                      "email": "j@example.com",
                      "specialties": ["Internal Medicine"],
                      "certifications": [],
                      "facilityIds": [],
                      "telehealthEnabled": false,
                      "availabilityStatus": "AVAILABLE"
                    },
                    "matchScore": 81.0,
                    "rank": 2,
                    "rationale": "Moderate specialty overlap."
                  }
                ]
                """;

        String summary = summarizer.summarizeToolResults(raw, HarnessContextKind.DOCTOR_MATCHES);

        assertTrue(summary.contains("\"top_matches\""));
        assertTrue(summary.contains("\"doctor_id\":\"doc-1\""));
        assertTrue(summary.contains("\"name\":\"Dr. Smith\""));
        assertTrue(summary.contains("\"match_count\":2"));
        assertFalse(summary.contains("Strong vector and graph alignment"));
        assertFalse(summary.contains("s@example.com"));
    }

    @Test
    @DisplayName("whitelist fields case_id and verify_status are preserved when present in input")
    void preservesWhitelistFields() {
        String raw = """
                {
                  "case_id": "case-abc",
                  "verify_status": "PASSED",
                  "candidates": ["very long candidate list that should not be forwarded verbatim to clinical model"]
                }
                """;

        String summary = summarizer.summarizeToolResults(raw, HarnessContextKind.GENERIC);

        assertTrue(summary.contains("\"case_id\":\"case-abc\""));
        assertTrue(summary.contains("\"verify_status\":\"PASSED\""));
        assertFalse(summary.contains("very long candidate list"));
    }

    @Test
    @DisplayName("evidence payload summarizes to counts and top citations")
    void summarizesEvidencePayload() {
        String raw = """
                Found 4 PubMed articles:
                1. PMID:111 - Hypertension management guidelines
                2. PMID:222 - ACE inhibitor meta-analysis
                3. PMID:333 - Blood pressure targets in elderly
                4. PMID:444 - Lifestyle modification review
                """;

        String summary = summarizer.summarizeToolResults(raw, HarnessContextKind.EVIDENCE);

        assertTrue(summary.contains("\"evidence_count\":4"));
        assertTrue(summary.contains("PMID:111"));
        assertFalse(summary.contains("PMID:444"));
    }
}
