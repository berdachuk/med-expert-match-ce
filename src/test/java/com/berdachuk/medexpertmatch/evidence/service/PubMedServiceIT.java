package com.berdachuk.medexpertmatch.evidence.service;

import com.berdachuk.medexpertmatch.evidence.domain.PubMedArticle;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PubMedServiceIT extends BaseIntegrationTest {

    @Autowired
    private PubMedService pubmedService;

    @Test
    void searchPubmedShouldReturnResultsOrEmptyList() {
        try {
            List<PubMedArticle> results = pubmedService.search("diabetes", 5);
            assertNotNull(results);
        } catch (Exception e) {
            fail("Should not throw unexpected exception: " + e.getMessage());
        }
    }
}
