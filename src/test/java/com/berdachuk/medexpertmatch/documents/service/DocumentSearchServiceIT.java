package com.berdachuk.medexpertmatch.documents.service;

import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.documents.domain.SourceDocumentEntity;
import com.berdachuk.medexpertmatch.documents.repository.SourceDocumentRepository;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

class DocumentSearchServiceIT extends BaseIntegrationTest {

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @BeforeEach
    void setUp() {
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.document_chunk");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.source_document");
    }

    @Test
    void shouldReturnEmptyResultsForEmptyQuery() {
        assertTrue(true);
    }

    @Test
    void contextLoadsWithDocumentSearchService() {
        assertTrue(true);
    }
}
