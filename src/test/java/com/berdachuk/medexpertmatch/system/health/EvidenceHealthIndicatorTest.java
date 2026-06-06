package com.berdachuk.medexpertmatch.system.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvidenceHealthIndicatorTest {

    @Test
    void shouldReportStatusWhenPubMedChecked() {
        EvidenceHealthIndicator indicator = new EvidenceHealthIndicator();
        Health health = indicator.health();

        assertNotNull(health);
        assertNotNull(health.getStatus().getCode());
        // Will be either UP (if internet reachable) or DOWN (if not), but must include details
        assertNotNull(health.getDetails().get("status"));
        assertNotNull(health.getDetails().get("responseTime"));
        assertNotNull(health.getDetails().get("endpoint"));
        String endpoint = health.getDetails().get("endpoint").toString();
        assertTrue(endpoint.contains("ncbi.nlm.nih.gov"));
        assertTrue(endpoint.contains("einfo.fcgi"), "health probe must call a valid NCBI eutil");
    }
}
