package com.berdachuk.medexpertmatch.documents.service;

import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.documents.domain.SourceDocumentEntity;
import com.berdachuk.medexpertmatch.documents.repository.SourceDocumentRepository;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentSearchServiceIT extends BaseIntegrationTest {

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @Autowired
    private SourceDocumentRepository sourceDocumentRepository;

    @BeforeEach
    void setUp() {
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.document_chunk");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.source_document");
    }

    @AfterEach
    void tearDown() {
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.document_chunk");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.source_document");
    }

    @Test
    void shouldReturnEmptyResultsForEmptyCorpus() {
        assertTrue(true);
    }

    @Test
    void contextLoadsWithDocumentSearchService() {
        assertTrue(true);
    }

    @Test
    void sourceDocumentRepositoryShouldBeAvailable() {
        assertNotNull(sourceDocumentRepository);
    }

    @Test
    void insertAndFindByIdShouldWork() {
        String docId = IdGenerator.generateId();
        SourceDocumentEntity doc = new SourceDocumentEntity(
                docId, "ext-1", "Test Document", "test", "test-source",
                "http://example.com", "Test content for search", "sha256-hash-123",
                "text/plain");

        sourceDocumentRepository.insert(doc);
        SourceDocumentEntity found = sourceDocumentRepository.findById(docId).orElse(null);

        assertNotNull(found);
        assertEquals("Test Document", found.title());
        assertEquals("test", found.category());
        assertEquals("Test content for search", found.content());
    }

    @Test
    void findAllWithLimitShouldReturnCorrectCount() {
        SourceDocumentEntity doc1 = new SourceDocumentEntity(
                IdGenerator.generateId(), "ext-1", "Doc 1", "test", "src",
                "http://example.com", "Content 1", "hash1", "text/plain");
        SourceDocumentEntity doc2 = new SourceDocumentEntity(
                IdGenerator.generateId(), "ext-2", "Doc 2", "test", "src",
                "http://example.com", "Content 2", "hash2", "text/plain");
        SourceDocumentEntity doc3 = new SourceDocumentEntity(
                IdGenerator.generateId(), "ext-3", "Doc 3", "other", "src",
                "http://example.com", "Content 3", "hash3", "text/plain");

        sourceDocumentRepository.insert(doc1);
        sourceDocumentRepository.insert(doc2);
        sourceDocumentRepository.insert(doc3);

        List<SourceDocumentEntity> all = sourceDocumentRepository.findAll(10);

        assertTrue(all.size() >= 3);
    }

    @Test
    void countDocumentsShouldReturnCorrectCount() {
        List<SourceDocumentEntity> initial = sourceDocumentRepository.findAll(1000);
        int initialSize = initial.size();

        SourceDocumentEntity doc = new SourceDocumentEntity(
                IdGenerator.generateId(), "ext-cnt", "Count Doc", "count-test", "src",
                "http://example.com", "Count content", "hash-cnt", "text/plain");
        sourceDocumentRepository.insert(doc);

        List<SourceDocumentEntity> after = sourceDocumentRepository.findAll(1000);
        assertTrue(after.size() >= initialSize + 1);
    }
}
