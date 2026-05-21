package com.berdachuk.medexpertmatch.documents.repository;

import com.berdachuk.medexpertmatch.documents.domain.SourceDocumentEntity;

import java.util.List;
import java.util.Optional;

public interface SourceDocumentRepository {

    Optional<SourceDocumentEntity> findById(String id);

    Optional<SourceDocumentEntity> findByContentHash(String contentHash);

    Optional<SourceDocumentEntity> findByExternalId(String externalId);

    List<SourceDocumentEntity> findAll(int limit);

    List<String> findAllIds(int limit);

    String insert(SourceDocumentEntity document);

    List<String> insertBatch(List<SourceDocumentEntity> documents);

    int deleteAll();

    List<String> findCategories();
}
