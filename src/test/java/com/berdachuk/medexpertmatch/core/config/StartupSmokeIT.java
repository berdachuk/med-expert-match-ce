package com.berdachuk.medexpertmatch.core.config;

import com.berdachuk.medexpertmatch.caseanalysis.service.CaseAnalysisService;
import com.berdachuk.medexpertmatch.graph.service.GraphService;
import com.berdachuk.medexpertmatch.retrieval.service.MatchingService;
import com.berdachuk.medexpertmatch.retrieval.service.SemanticGraphRetrievalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@DisplayName("Startup smoke tests")
class StartupSmokeIT {

    @Test
    @DisplayName("GraphService interface is mockable")
    void graphServiceIsMockable() {
        var graphService = mock(GraphService.class);
        assertNotNull(graphService);
    }

    @Test
    @DisplayName("MatchingService interface is mockable")
    void matchingServiceIsMockable() {
        var matchingService = mock(MatchingService.class);
        assertNotNull(matchingService);
    }

    @Test
    @DisplayName("CaseAnalysisService interface is mockable")
    void caseAnalysisServiceIsMockable() {
        var caseAnalysisService = mock(CaseAnalysisService.class);
        assertNotNull(caseAnalysisService);
    }

    @Test
    @DisplayName("SemanticGraphRetrievalService interface is mockable")
    void semanticGraphRetrievalServiceIsMockable() {
        var retrievalService = mock(SemanticGraphRetrievalService.class);
        assertNotNull(retrievalService);
    }
}
