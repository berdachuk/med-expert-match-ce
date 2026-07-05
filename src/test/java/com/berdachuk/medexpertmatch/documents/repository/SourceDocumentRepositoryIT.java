package com.berdachuk.medexpertmatch.documents.repository;

import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.documents.domain.SourceDocumentEntity;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * REQ-016: integration coverage for registered requirement.
 */
class SourceDocumentRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private SourceDocumentRepository sourceDocumentRepository;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @BeforeEach
    void setUp() {
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.document_chunk");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.source_document");
    }

    @Test
    void shouldInsertAndFindById() {
        String id = IdGenerator.generateId();
        SourceDocumentEntity doc = new SourceDocumentEntity(
                id, "ext-1", "Test Document", "clinical", "Test Source",
                "http://example.com", "Test content", "abc123", "pdf");

        String returnedId = sourceDocumentRepository.insert(doc);
        assertEquals(id, returnedId);

        SourceDocumentEntity found = sourceDocumentRepository.findById(id).orElseThrow();
        assertEquals("Test Document", found.title());
        assertEquals("clinical", found.category());
        assertEquals("Test content", found.content());
        assertEquals("abc123", found.contentHash());
    }

    @Test
    void shouldFindByContentHash() {
        String id = IdGenerator.generateId();
        String hash = "hash-" + IdGenerator.generateId();
        SourceDocumentEntity doc = new SourceDocumentEntity(
                id, "ext-2", "Doc", null, null, null, "content", hash, "json");

        sourceDocumentRepository.insert(doc);

        SourceDocumentEntity found = sourceDocumentRepository.findByContentHash(hash).orElseThrow();
        assertEquals(id, found.id());
    }

    @Test
    void shouldFindByExternalId() {
        String id = IdGenerator.generateId();
        SourceDocumentEntity doc = new SourceDocumentEntity(
                id, "ext-unique-1", "Doc", null, null, null, "content", "hash1", "csv");

        sourceDocumentRepository.insert(doc);

        SourceDocumentEntity found = sourceDocumentRepository.findByExternalId("ext-unique-1").orElseThrow();
        assertEquals(id, found.id());
    }

    @Test
    void shouldReturnEmptyForNonExistentId() {
        assertTrue(sourceDocumentRepository.findById("nonexistent123456").isEmpty());
    }

    @Test
    void shouldReturnEmptyForNonExistentHash() {
        assertTrue(sourceDocumentRepository.findByContentHash("nonexistent-hash").isEmpty());
    }

    @Test
    void shouldReturnEmptyForNonExistentExternalId() {
        assertTrue(sourceDocumentRepository.findByExternalId("nonexistent-ext").isEmpty());
    }

    @Test
    void shouldFindAllWithLimit() {
        sourceDocumentRepository.insert(createDoc("Content A", "hash-a"));
        sourceDocumentRepository.insert(createDoc("Content B", "hash-b"));
        sourceDocumentRepository.insert(createDoc("Content C", "hash-c"));

        List<SourceDocumentEntity> docs = sourceDocumentRepository.findAll(2);
        assertEquals(2, docs.size());
    }

    @Test
    void shouldFindAllIds() {
        sourceDocumentRepository.insert(createDoc("A", "hash-a1"));
        sourceDocumentRepository.insert(createDoc("B", "hash-b1"));

        List<String> ids = sourceDocumentRepository.findAllIds(10);
        assertEquals(2, ids.size());
    }

    @Test
    void shouldInsertBatch() {
        SourceDocumentEntity doc1 = createDoc("Batch 1", "hash-batch-1");
        SourceDocumentEntity doc2 = createDoc("Batch 2", "hash-batch-2");

        List<String> ids = sourceDocumentRepository.insertBatch(List.of(doc1, doc2));
        assertEquals(2, ids.size());
        assertEquals(2, sourceDocumentRepository.findAllIds(10).size());
    }

    @Test
    void shouldRejectDuplicateContentHash() {
        String id = IdGenerator.generateId();
        SourceDocumentEntity doc = new SourceDocumentEntity(
                id, "ext-3", "Doc", null, null, null, "content", "duplicate-hash", "pdf");
        sourceDocumentRepository.insert(doc);

        String id2 = IdGenerator.generateId();
        SourceDocumentEntity doc2 = new SourceDocumentEntity(
                id2, "ext-4", "Doc2", null, null, null, "content", "duplicate-hash", "pdf");

        assertThrows(DuplicateKeyException.class, () -> sourceDocumentRepository.insert(doc2));
    }

    @Test
    void shouldFindCategories() {
        sourceDocumentRepository.insert(createDocWithCategory("Doc1", "hash-c1", "clinical"));
        sourceDocumentRepository.insert(createDocWithCategory("Doc2", "hash-c2", "research"));
        sourceDocumentRepository.insert(createDocWithCategory("Doc3", "hash-c3", "clinical"));

        List<String> categories = sourceDocumentRepository.findCategories();
        assertEquals(2, categories.size());
        assertTrue(categories.contains("clinical"));
        assertTrue(categories.contains("research"));
    }

    @Test
    void shouldFindCategoriesWithNulls() {
        sourceDocumentRepository.insert(createDocWithCategory("Doc1", "hash-nc1", "clinical"));
        sourceDocumentRepository.insert(createDocWithCategory("Doc2", "hash-nc2", null));

        List<String> categories = sourceDocumentRepository.findCategories();
        assertEquals(1, categories.size());
        assertEquals("clinical", categories.get(0));
    }

    @Test
    void shouldDeleteAll() {
        sourceDocumentRepository.insert(createDoc("A", "hash-da1"));
        sourceDocumentRepository.insert(createDoc("B", "hash-da2"));

        int deleted = sourceDocumentRepository.deleteAll();
        assertTrue(deleted >= 2);
        assertTrue(sourceDocumentRepository.findAll(10).isEmpty());
    }

    @Test
    void shouldFindAllWithZeroLimitReturnsAll() {
        sourceDocumentRepository.insert(createDoc("A", "hash-zl1"));
        sourceDocumentRepository.insert(createDoc("B", "hash-zl2"));
        sourceDocumentRepository.insert(createDoc("C", "hash-zl3"));

        List<SourceDocumentEntity> docs = sourceDocumentRepository.findAll(0);
        assertEquals(3, docs.size());
    }

    @Test
    void shouldReturnEmptyListWhenNoDocuments() {
        assertTrue(sourceDocumentRepository.findAll(10).isEmpty());
        assertTrue(sourceDocumentRepository.findAllIds(10).isEmpty());
        assertTrue(sourceDocumentRepository.findCategories().isEmpty());
    }

    @Test
    void shouldInsertBatchWithEmptyList() {
        List<String> ids = sourceDocumentRepository.insertBatch(List.of());
        assertTrue(ids.isEmpty());
    }

    private SourceDocumentEntity createDoc(String content, String contentHash) {
        return new SourceDocumentEntity(
                IdGenerator.generateId(), null, "Test Doc", null, null, null,
                content, contentHash, "pdf");
    }

    private SourceDocumentEntity createDocWithCategory(String content, String contentHash, String category) {
        return new SourceDocumentEntity(
                IdGenerator.generateId(), null, "Test Doc", category, null, null,
                content, contentHash, "pdf");
    }
}
