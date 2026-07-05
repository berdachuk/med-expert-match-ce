package com.berdachuk.medexpertmatch.documents.service;

import com.berdachuk.medexpertmatch.documents.service.impl.StructuredFileParser;
import com.berdachuk.medexpertmatch.documents.service.impl.StructuredFileParser.ParsedDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * REQ-016: integration coverage for registered requirement.
 */
class DocumentParserIT {

    private StructuredFileParser parser;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        parser = new StructuredFileParser(new ObjectMapper());
    }

    @Test
    void shouldParseJsonlWithMetadata() throws Exception {
        Path file = tempDir.resolve("test.jsonl");
        String content = """
                {"id": "1", "title": "Doc One", "category": "cardiology", "source": "pubmed", "url": "http://example.com/1", "text": "Heart disease study content"}
                {"id": "2", "title": "Doc Two", "category": "neurology", "source": "pubmed", "url": "http://example.com/2", "text": "Brain research content"}
                """;
        Files.writeString(file, content);

        List<ParsedDocument> docs = parser.parseJsonl(file);

        assertEquals(2, docs.size());
        assertEquals("1", docs.get(0).externalId());
        assertEquals("Doc One", docs.get(0).title());
        assertEquals("cardiology", docs.get(0).category());
        assertEquals("Heart disease study content", docs.get(0).content());
    }

    @Test
    void shouldHandleEmptyJsonlFile() throws Exception {
        Path file = tempDir.resolve("empty.jsonl");
        Files.writeString(file, "");

        List<ParsedDocument> docs = parser.parseJsonl(file);

        assertTrue(docs.isEmpty());
    }

    @Test
    void shouldParseSingleJsonObject() throws Exception {
        Path file = tempDir.resolve("single.json");
        String content = """
                {"id": "single-1", "title": "Single Doc", "category": "oncology", "source": "pubmed", "url": "http://example.com/s", "text": "Cancer research"}
                """;
        Files.writeString(file, content);

        List<ParsedDocument> docs = parser.parseJson(file);

        assertEquals(1, docs.size());
        assertEquals("Single Doc", docs.get(0).title());
        assertEquals("Cancer research", docs.get(0).content());
    }

    @Test
    void shouldParseJsonArray() throws Exception {
        Path file = tempDir.resolve("array.json");
        String content = """
                [
                {"id": "a1", "title": "Array One", "text": "Content A"},
                {"id": "a2", "title": "Array Two", "text": "Content B"}
                ]
                """;
        Files.writeString(file, content);

        List<ParsedDocument> docs = parser.parseJson(file);

        assertEquals(2, docs.size());
    }

    @Test
    void shouldParseCsvWithHeaders() throws Exception {
        Path file = tempDir.resolve("test.csv");
        String content = """
                id,title,category,text
                1,CSV Doc One,cardiology,Content for CSV 1
                2,CSV Doc Two,neurology,Content for CSV 2
                """;
        Files.writeString(file, content);

        List<ParsedDocument> docs = parser.parseCsv(file);

        assertEquals(2, docs.size());
        ParsedDocument first = docs.get(0);
        assertTrue(first.content().contains("id: 1"));
        assertTrue(first.content().contains("title: CSV Doc One"));
    }

    @Test
    void shouldHandleEmptyCsv() throws Exception {
        Path file = tempDir.resolve("empty.csv");
        Files.writeString(file, "");

        List<ParsedDocument> docs = parser.parseCsv(file);

        assertTrue(docs.isEmpty());
    }
}
