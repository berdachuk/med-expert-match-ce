package com.berdachuk.medexpertmatch.documents.service.impl;

import com.berdachuk.medexpertmatch.chunking.service.impl.ChunkerFactory;
import com.berdachuk.medexpertmatch.chunking.domain.DocumentChunk;
import com.berdachuk.medexpertmatch.chunking.repository.ChunkRepository;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.documents.DocumentIngestApi;
import com.berdachuk.medexpertmatch.documents.domain.SourceDocumentEntity;
import com.berdachuk.medexpertmatch.documents.repository.SourceDocumentRepository;
import com.berdachuk.medexpertmatch.embedding.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(name = "medexpertmatch.documents.enabled", havingValue = "true")
public class DocumentIngestServiceImpl implements DocumentIngestApi {

    private final SourceDocumentRepository sourceDocumentRepository;
    private final ChunkRepository chunkRepository;
    private final PdfTextExtractor pdfTextExtractor;
    private final StructuredFileParser structuredFileParser;
    private final ChunkerFactory chunkerFactory;
    private final DocumentEmbeddingPipeline embeddingPipeline;
    private final int chunkSize;
    private final int chunkOverlap;

    public DocumentIngestServiceImpl(
            SourceDocumentRepository sourceDocumentRepository,
            ChunkRepository chunkRepository,
            PdfTextExtractor pdfTextExtractor,
            StructuredFileParser structuredFileParser,
            ChunkerFactory chunkerFactory,
            DocumentEmbeddingPipeline embeddingPipeline,
            @Value("${medexpertmatch.documents.chunking.chunk-size:512}") int chunkSize,
            @Value("${medexpertmatch.documents.chunking.chunk-overlap:64}") int chunkOverlap) {
        this.sourceDocumentRepository = sourceDocumentRepository;
        this.chunkRepository = chunkRepository;
        this.pdfTextExtractor = pdfTextExtractor;
        this.structuredFileParser = structuredFileParser;
        this.chunkerFactory = chunkerFactory;
        this.embeddingPipeline = embeddingPipeline;
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    @Override
    @Transactional
    public int ingestPaths(List<String> paths) {
        int total = 0;
        for (String pathStr : paths) {
            Path path = Path.of(pathStr);
            if (!Files.exists(path)) {
                log.warn("File not found: {}", pathStr);
                continue;
            }
            try {
                if (pathStr.endsWith(".pdf")) {
                    total += ingestPdf(path);
                } else if (pathStr.endsWith(".jsonl")) {
                    total += ingestStructured(path, "jsonl");
                } else if (pathStr.endsWith(".json")) {
                    total += ingestStructured(path, "json");
                } else if (pathStr.endsWith(".csv")) {
                    total += ingestStructured(path, "csv");
                } else {
                    log.warn("Unsupported file format: {}", pathStr);
                }
            } catch (IOException e) {
                log.error("Failed to ingest file {}: {}", pathStr, e.getMessage());
            }
        }
        return total;
    }

    @Override
    @Transactional
    public int ingestFromDirectory(String directoryPath) {
        Path dir = Path.of(directoryPath);
        if (!Files.isDirectory(dir)) {
            log.warn("Directory not found: {}", directoryPath);
            return 0;
        }
        try {
            List<String> paths = Files.list(dir)
                    .map(Path::toString)
                    .filter(p -> p.endsWith(".pdf") || p.endsWith(".jsonl") || p.endsWith(".json") || p.endsWith(".csv"))
                    .toList();
            return ingestPaths(paths);
        } catch (IOException e) {
            log.error("Failed to list directory {}: {}", directoryPath, e.getMessage());
            return 0;
        }
    }

    private int ingestPdf(Path path) throws IOException {
        String content = pdfTextExtractor.extract(path);
        String hash = ContentHasher.sha256(content);

        if (sourceDocumentRepository.findByContentHash(hash).isPresent()) {
            log.info("Document already exists (hash match), skipping: {}", path);
            return 0;
        }

        String title = path.getFileName().toString().replace(".pdf", "");
        SourceDocumentEntity doc = saveDocument(null, title, "pdf", path.getFileName().toString(), null, content, hash, "pdf");
        chunkDocument(doc);
        return 1;
    }

    private int ingestStructured(Path path, String format) throws IOException {
        List<StructuredFileParser.ParsedDocument> parsedDocs = switch (format) {
            case "jsonl" -> structuredFileParser.parseJsonl(path);
            case "json" -> structuredFileParser.parseJson(path);
            case "csv" -> structuredFileParser.parseCsv(path);
            default -> List.of();
        };

        int count = 0;
        for (StructuredFileParser.ParsedDocument parsed : parsedDocs) {
            String hash = ContentHasher.sha256(parsed.content());
            if (sourceDocumentRepository.findByContentHash(hash).isPresent()) {
                log.debug("Document already exists (hash match), skipping");
                continue;
            }
            SourceDocumentEntity doc = saveDocument(
                    parsed.externalId(), parsed.title(), parsed.category(),
                    parsed.sourceName(), parsed.sourceUrl(),
                    parsed.content(), hash, parsed.sourceFormat());
            chunkDocument(doc);
            count++;
        }
        return count;
    }

    private SourceDocumentEntity saveDocument(String externalId, String title, String category,
                                              String sourceName, String sourceUrl,
                                              String content, String contentHash, String sourceFormat) {
        String id = IdGenerator.generateId();
        SourceDocumentEntity doc = new SourceDocumentEntity(
                id, externalId, title, category, sourceName, sourceUrl, content, contentHash, sourceFormat);
        sourceDocumentRepository.insert(doc);
        log.debug("Saved document: id={}, title={}", id, title);
        return doc;
    }

    private void chunkDocument(SourceDocumentEntity doc) {
        var chunker = chunkerFactory.getChunker("adaptive");
        List<String> chunks = chunker.chunk(doc.content(), chunkSize, chunkOverlap, 100);
        List<DocumentChunk> chunkEntities = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = new DocumentChunk(IdGenerator.generateId(), doc.id(), i, chunks.get(i), null);
            chunkEntities.add(chunk);
        }
        if (!chunkEntities.isEmpty()) {
            chunkRepository.insertBatch(chunkEntities);
            log.debug("Created {} chunks for document {}", chunkEntities.size(), doc.id());
            embeddingPipeline.embedChunks(chunkEntities);
        }
    }
}
